import { create } from 'zustand';
import axiosClient from '../api/axiosClient';

export const useNotificationStore = create((set, get) => ({
  notifications: [
    {
      id: 1,
      title: 'Chào mừng bạn đến với QuickShop!',
      content: 'Nhận ngay Voucher giảm giá 50K cho đơn hàng đầu tiên. Chúc bạn có trải nghiệm mua sắm tuyệt vời!',
      type: 'PROMOTION',
      isRead: false,
      createdAt: new Date().toISOString(),
    },
    {
      id: 2,
      title: 'Hệ thống Microservices đã sẵn sàng',
      content: 'Hạ tầng Catalog Service 8082, Notification Service 8086 và Redis Cache đang hoạt động ổn định.',
      type: 'SYSTEM',
      isRead: true,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
  ],
  unreadCount: 1,
  loading: false,

  fetchNotifications: async () => {
    set({ loading: true });
    try {
      const res = await axiosClient.get('/api/notifications/my');
      if (res.success && Array.isArray(res.data)) {
        const unread = res.data.filter((n) => !n.isRead).length;
        set({ notifications: res.data, unreadCount: unread });
      }
    } catch (e) {
      console.warn('Cannot fetch real notifications, using local state:', e);
    } finally {
      set({ loading: false });
    }
  },

  markAsRead: async (id) => {
    try {
      await axiosClient.patch(`/api/notifications/${id}/read`);
    } catch (e) {
      console.warn('Failed to mark read on server, updating local store:', e);
    }
    const updated = get().notifications.map((n) =>
      n.id === id ? { ...n, isRead: true } : n
    );
    const unread = updated.filter((n) => !n.isRead).length;
    set({ notifications: updated, unreadCount: unread });
  },

  markAllAsRead: async () => {
    try {
      await axiosClient.patch('/api/notifications/read-all');
    } catch (e) {
      console.warn('Failed to mark all read on server, updating local store:', e);
    }
    const updated = get().notifications.map((n) => ({ ...n, isRead: true }));
    set({ notifications: updated, unreadCount: 0 });
  },
}));
