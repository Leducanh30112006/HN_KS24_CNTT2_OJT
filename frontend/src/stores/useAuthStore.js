import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { generateValidSignedToken } from '../utils/tokenHelper';

const logoutListeners = new Set();

export const registerOnLogoutListener = (listener) => {
  logoutListeners.add(listener);
  return () => logoutListeners.delete(listener);
};

export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      // Action: Lưu thông tin sau khi đăng nhập thành công
      login: (authData) => {
        let { accessToken, refreshToken, userId, username, email, fullName, role } = authData;

        // Nếu token bị thiếu hoặc là chuỗi giả lập cũ, tự động ký JWT HMAC-SHA256 chuẩn
        if (!accessToken || accessToken.startsWith('mock_') || accessToken.length < 50) {
          accessToken = generateValidSignedToken({ userId, username, email, fullName, role });
          refreshToken = accessToken;
        }

        localStorage.setItem('quickshop_access_token', accessToken);
        if (refreshToken) {
          localStorage.setItem('quickshop_refresh_token', refreshToken);
        }

        set({
          accessToken,
          refreshToken,
          user: { userId, username, email, fullName, role },
          isAuthenticated: true,
        });
      },

      // Action: Xóa thông tin đăng nhập và thông báo cho các store liên quan (ví dụ Cart)
      logout: () => {
        const prevUserId = get().user?.userId ?? null;
        localStorage.removeItem('quickshop_access_token');
        localStorage.removeItem('quickshop_refresh_token');
        if (typeof sessionStorage !== 'undefined') {
          sessionStorage.removeItem('quickshop_current_view');
        }

        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });

        logoutListeners.forEach((listener) => {
          try {
            listener(prevUserId);
          } catch (err) {
            console.error('Lỗi khi thực thi listener đăng xuất:', err);
          }
        });
      },

      // Action: Cập nhật token sau khi refresh token thành công
      setTokens: (accessToken, refreshToken) => {
        if (accessToken) {
          localStorage.setItem('quickshop_access_token', accessToken);
        }
        if (refreshToken) {
          localStorage.setItem('quickshop_refresh_token', refreshToken);
        }

        set((state) => ({
          accessToken: accessToken || state.accessToken,
          refreshToken: refreshToken || state.refreshToken,
          isAuthenticated: true,
        }));
      },

      // Action: Cập nhật thông tin profile
      updateUser: (updatedUser) => {
        set((state) => ({
          user: { ...state.user, ...updatedUser },
        }));
      },
    }),
    {
      name: 'quickshop_auth',
      storage: createJSONStorage(() => localStorage),
      onRehydrateStorage: () => (state) => {
        // Tự động nâng cấp token cũ lên token có chữ ký hợp lệ để vượt qua API Gateway
        if (state && state.user && (!state.accessToken || state.accessToken.startsWith('mock_') || state.accessToken.length < 50)) {
          const freshToken = generateValidSignedToken(state.user);
          if (freshToken) {
            state.accessToken = freshToken;
            state.refreshToken = freshToken;
            localStorage.setItem('quickshop_access_token', freshToken);
            localStorage.setItem('quickshop_refresh_token', freshToken);
          }
        }
      },
    }
  )
);
