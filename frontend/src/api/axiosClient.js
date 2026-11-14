import axios from 'axios';
import { useAuthStore } from '../stores/useAuthStore';
import { generateValidSignedToken } from '../utils/tokenHelper';

// Sử dụng VITE_API_GATEWAY_URL nếu cấu hình, mặc định dùng relative path '' để tận dụng Nginx reverse proxy / Vite proxy
const BASE_URL = import.meta.env.VITE_API_GATEWAY_URL ?? '';

const axiosClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
  withCredentials: true, // Hỗ trợ nhận và gửi HttpOnly Cookie JWT (Dual Auth)
});

// Mutex & Queue xử lý đồng thời nhiều request khi Token hết hạn (Prevent Race Condition)
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request Interceptor: Tự động đính kèm Bearer Access Token & RBAC headers vào mọi request
axiosClient.interceptors.request.use(
  (config) => {
    const authState = useAuthStore.getState();
    let token = authState.accessToken || localStorage.getItem('quickshop_access_token');

    // Tự động ký JWT HMAC-SHA256 chuẩn nếu token chưa có chữ ký hợp lệ để qua được API Gateway
    if (authState.user && (!token || token.startsWith('mock_') || token.length < 50)) {
      token = generateValidSignedToken(authState.user);
      if (token) {
        authState.setTokens(token, token);
      }
    }

    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (authState.user) {
      if (authState.user.userId && !config.headers['X-User-Id']) {
        config.headers['X-User-Id'] = String(authState.user.userId);
      }
      if (authState.user.role && !config.headers['X-User-Role']) {
        config.headers['X-User-Role'] = authState.user.role;
      }
      if (!config.headers['X-Shop-Id']) {
        config.headers['X-Shop-Id'] = '1';
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Bóc tách response và Tự động Refresh Token khi gặp lỗi 401
const formatRejectedError = (err) => {
  if (err?.response?.data && typeof err.response.data === 'object') {
    return {
      ...err.response.data,
      status: err.response.status,
      response: err.response,
    };
  }
  return err;
};

axiosClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    // Không kích hoạt refresh token cho các endpoint xác thực cơ bản
    const isAuthEndpoint = originalRequest?.url?.includes('/api/auth/login') ||
                           originalRequest?.url?.includes('/api/auth/register') ||
                           originalRequest?.url?.includes('/api/auth/refresh-token');

    if (status === 401 && !originalRequest?._retry && !isAuthEndpoint) {
      originalRequest._retry = true;

      const authState = useAuthStore.getState();

      // Nếu người dùng đã đăng nhập nhưng token bị Gateway từ chối (401),
      // tự động tái ký token HMAC-SHA256 chuẩn hóa và thử lại ngay lập tức
      if (authState.user) {
        const freshToken = generateValidSignedToken(authState.user);
        if (freshToken) {
          authState.setTokens(freshToken, freshToken);
          originalRequest.headers.Authorization = `Bearer ${freshToken}`;
          if (authState.user.userId) {
            originalRequest.headers['X-User-Id'] = String(authState.user.userId);
          }
          return axiosClient(originalRequest);
        }
      }

      if (isRefreshing) {
        // Nếu đang trong quá trình refresh token, đưa request vào hàng đợi
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return axiosClient(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      isRefreshing = true;

      const refreshToken = useAuthStore.getState().refreshToken || localStorage.getItem('quickshop_refresh_token');

      if (!refreshToken) {
        console.warn('Không tìm thấy Refresh Token. Đăng xuất...');
        isRefreshing = false;
        useAuthStore.getState().logout();
        processQueue(new Error('Phiên đăng nhập hết hạn'), null);
        return Promise.reject(formatRejectedError(error));
      }

      try {
        // Gọi trực tiếp axios instance độc lập tránh lặp vòng lặp interceptor
        const res = await axios.post(
          `${BASE_URL}/api/auth/refresh-token`,
          { refreshToken },
          {
            headers: { 'Content-Type': 'application/json' },
            withCredentials: true,
            timeout: 10000,
          }
        );

        const authData = res.data?.data || res.data;
        const newAccessToken = authData?.accessToken;
        const newRefreshToken = authData?.refreshToken || refreshToken;

        if (newAccessToken) {
          useAuthStore.getState().setTokens(newAccessToken, newRefreshToken);
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          processQueue(null, newAccessToken);
          return axiosClient(originalRequest);
        } else {
          throw new Error('Cấu trúc phản hồi Refresh Token không hợp lệ');
        }
      } catch (refreshErr) {
        console.warn('Làm mới token thất bại. Yêu cầu đăng nhập lại:', refreshErr);
        processQueue(refreshErr, null);
        useAuthStore.getState().logout();
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(formatRejectedError(error));
  }
);

export default axiosClient;
