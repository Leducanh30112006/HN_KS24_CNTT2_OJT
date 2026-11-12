import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

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
        const { accessToken, refreshToken, userId, username, email, fullName, role } = authData;

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
    }
  )
);
