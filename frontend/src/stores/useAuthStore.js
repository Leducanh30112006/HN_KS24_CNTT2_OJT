import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set) => ({
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

      // Action: Xóa thông tin đăng nhập
      logout: () => {
        localStorage.removeItem('quickshop_access_token');
        localStorage.removeItem('quickshop_refresh_token');

        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });
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
