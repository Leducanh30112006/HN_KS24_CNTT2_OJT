import axiosClient from './axiosClient';

const LOCAL_STORAGE_KEY_PREFIX = 'quickshop_user_addresses_';

function getLocalAddresses(userId) {
  try {
    const raw = localStorage.getItem(`${LOCAL_STORAGE_KEY_PREFIX}${userId || 'guest'}`);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveLocalAddresses(userId, list) {
  try {
    localStorage.setItem(`${LOCAL_STORAGE_KEY_PREFIX}${userId || 'guest'}`, JSON.stringify(list));
  } catch (e) {
    console.warn('Lỗi lưu địa chỉ vào localStorage:', e);
  }
}

/**
 * Lấy danh sách địa chỉ giao hàng của người dùng
 */
export async function fetchUserAddresses(userId) {
  try {
    const res = await axiosClient.get('/api/users/addresses');
    if (res.data?.success && Array.isArray(res.data.data)) {
      saveLocalAddresses(userId, res.data.data);
      return res.data.data;
    }
    if (Array.isArray(res.data)) {
      saveLocalAddresses(userId, res.data);
      return res.data;
    }
  } catch (err) {
    console.warn('API /api/users/addresses chưa phản hồi, sử dụng dữ liệu offline:', err?.message);
  }
  return getLocalAddresses(userId);
}

/**
 * Lấy địa chỉ giao hàng mặc định
 */
export async function fetchDefaultAddress(userId) {
  try {
    const res = await axiosClient.get('/api/users/addresses/default');
    if (res.data?.success && res.data.data) {
      return res.data.data;
    }
  } catch {
    // ignore
  }
  const list = getLocalAddresses(userId);
  return list.find((a) => a.isDefault) || list[0] || null;
}

/**
 * Thêm địa chỉ mới kèm tọa độ bản đồ
 */
export async function createUserAddress(userId, payload) {
  try {
    const res = await axiosClient.post('/api/users/addresses', payload);
    if (res.data?.success && res.data.data) {
      const created = res.data.data;
      const list = getLocalAddresses(userId);
      if (created.isDefault) {
        list.forEach((a) => (a.isDefault = false));
      }
      saveLocalAddresses(userId, [created, ...list.filter((a) => a.id !== created.id)]);
      return created;
    }
  } catch (err) {
    console.warn('Không thể gọi API thêm địa chỉ, lưu cục bộ:', err?.message);
  }

  // Fallback lưu cục bộ
  const list = getLocalAddresses(userId);
  const isDefault = payload.isDefault ?? list.length === 0;
  if (isDefault) {
    list.forEach((a) => (a.isDefault = false));
  }
  const newAddr = {
    id: Date.now(),
    userId: userId || 1,
    recipientName: payload.recipientName,
    phoneNumber: payload.phoneNumber,
    streetAddress: payload.streetAddress,
    ward: payload.ward || '',
    district: payload.district || '',
    city: payload.city || '',
    fullAddress: payload.fullAddress || `${payload.streetAddress}, ${payload.ward || ''}, ${payload.district || ''}, ${payload.city || ''}`.replace(/,\s*,/g, ',').trim(),
    tag: payload.tag || 'Nhà riêng',
    isDefault,
    latitude: payload.latitude,
    longitude: payload.longitude,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  list.unshift(newAddr);
  saveLocalAddresses(userId, list);
  return newAddr;
}

/**
 * Cập nhật địa chỉ
 */
export async function updateUserAddress(userId, id, payload) {
  try {
    const res = await axiosClient.put(`/api/users/addresses/${id}`, payload);
    if (res.data?.success && res.data.data) {
      const updated = res.data.data;
      const list = getLocalAddresses(userId);
      const next = list.map((a) => (a.id === id ? updated : (updated.isDefault ? { ...a, isDefault: false } : a)));
      saveLocalAddresses(userId, next);
      return updated;
    }
  } catch (err) {
    console.warn('Không thể gọi API sửa địa chỉ, lưu cục bộ:', err?.message);
  }

  // Fallback cục bộ
  const list = getLocalAddresses(userId);
  const next = list.map((a) => {
    if (a.id === id) {
      return {
        ...a,
        ...payload,
        fullAddress: payload.fullAddress || `${payload.streetAddress}, ${payload.ward || ''}, ${payload.district || ''}, ${payload.city || ''}`.replace(/,\s*,/g, ',').trim(),
        updatedAt: new Date().toISOString(),
      };
    }
    if (payload.isDefault) {
      return { ...a, isDefault: false };
    }
    return a;
  });
  saveLocalAddresses(userId, next);
  return next.find((a) => a.id === id);
}

/**
 * Xóa địa chỉ
 */
export async function deleteUserAddress(userId, id) {
  try {
    await axiosClient.delete(`/api/users/addresses/${id}`);
  } catch (err) {
    console.warn('Không thể gọi API xóa địa chỉ, xóa cục bộ:', err?.message);
  }
  const list = getLocalAddresses(userId);
  const next = list.filter((a) => a.id !== id);
  if (next.length > 0 && !next.some((a) => a.isDefault)) {
    next[0].isDefault = true;
  }
  saveLocalAddresses(userId, next);
}

/**
 * Đặt làm địa chỉ mặc định
 */
export async function setDefaultUserAddress(userId, id) {
  try {
    const res = await axiosClient.patch(`/api/users/addresses/${id}/default`);
    if (res.data?.success && res.data.data) {
      const updated = res.data.data;
      const list = getLocalAddresses(userId);
      const next = list.map((a) => ({ ...a, isDefault: a.id === id }));
      saveLocalAddresses(userId, next);
      return updated;
    }
  } catch (err) {
    console.warn('Không thể gọi API set default, cập nhật cục bộ:', err?.message);
  }
  const list = getLocalAddresses(userId);
  const next = list.map((a) => ({ ...a, isDefault: a.id === id }));
  saveLocalAddresses(userId, next);
  return next.find((a) => a.id === id);
}
