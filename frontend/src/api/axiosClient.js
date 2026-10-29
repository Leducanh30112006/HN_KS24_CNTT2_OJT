import axios from 'axios';
import { useAuthStore } from '../stores/useAuthStore';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Request Interceptor: Tự động đính kèm Bearer Access Token vào mọi request
axiosClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken || localStorage.getItem('quickshop_access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Bóc tách response và bắt lỗi 401 hết hạn phiên
axiosClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      console.warn('Phiên làm việc đã hết hạn. Đang đăng xuất...');
      useAuthStore.getState().logout();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error.response?.data || error.message || 'Lỗi kết nối máy chủ');
  }
);

export default axiosClient;
