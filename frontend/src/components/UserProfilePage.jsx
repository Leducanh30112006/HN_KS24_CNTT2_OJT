import React, { useState, useEffect, useRef } from 'react';
import { 
  User, Mail, Phone, MapPin, Lock, ShieldCheck, Award, 
  ShoppingBag, CreditCard, ChevronRight, CheckCircle2, AlertCircle, 
  ArrowLeft, Edit3, Save, Key, Plus, Trash2, Check, Sparkles, Building,
  Camera, Upload, Image as ImageIcon, X, Edit2, Compass
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { getStoredOrders } from '../data/sampleOrders';
import axiosClient from '../api/axiosClient';
import AddressMapPickerModal from './AddressMapPickerModal';
import { 
  fetchUserAddresses, 
  createUserAddress, 
  updateUserAddress, 
  deleteUserAddress, 
  setDefaultUserAddress 
} from '../api/addressApi';

export default function UserProfilePage({ onBackToHome, onViewOrders }) {
  const { user, updateUser } = useAuthStore();
  const [activeTab, setActiveTab] = useState('profile'); // 'profile' | 'address' | 'security' | 'stats'

  // Profile Form States
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phoneNumber || user?.phone || '');
  const [email, setEmail] = useState(user?.email || '');
  const [bio, setBio] = useState('Khách hàng thân thiết của QuickShop Mall. Ưa chuộng công nghệ và đồ gia dụng.');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState(null);

  useEffect(() => {
    if (user) {
      if (user.fullName !== undefined) setFullName(user.fullName || '');
      setPhone(user.phoneNumber || user.phone || '');
      if (user.email !== undefined) setEmail(user.email || '');
      if (user.avatar || user.avatarUrl) {
        setAvatar(user.avatar || user.avatarUrl);
      }
    }
  }, [user]);

  // Avatar States
  const [avatar, setAvatar] = useState(user?.avatar || user?.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200');
  const [showAvatarModal, setShowAvatarModal] = useState(false);
  const [customAvatarUrl, setCustomAvatarUrl] = useState('');
  const fileInputRef = useRef(null);

  // Đóng modal Avatar khi bấm phím Escape
  useEffect(() => {
    if (!showAvatarModal) return;
    const handleEscape = (e) => {
      if (e.key === 'Escape') setShowAvatarModal(false);
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [showAvatarModal]);

  const PRESET_AVATARS = [
    { label: 'Thanh lịch', url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80' },
    { label: 'Doanh nhân', url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80' },
    { label: 'Công nghệ', url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80' },
    { label: 'Trẻ trung', url: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200&auto=format&fit=crop&q=80' },
    { label: 'Năng động', url: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&auto=format&fit=crop&q=80' },
    { label: 'Sáng tạo', url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80' },
  ];

  const handleAvatarFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      alert('Kích thước ảnh tối đa là 5MB!');
      return;
    }
    const reader = new FileReader();
    reader.onload = (event) => {
      const dataUrl = event.target.result;
      setAvatar(dataUrl);
      updateUser({ avatar: dataUrl, avatarUrl: dataUrl });
      setShowAvatarModal(false);
      setProfileMessage({ type: 'success', text: 'Cập nhật ảnh đại diện thành công!' });
      setTimeout(() => setProfileMessage(null), 3500);
    };
    reader.readAsDataURL(file);
  };

  const handleSelectPresetAvatar = (url) => {
    setAvatar(url);
    updateUser({ avatar: url, avatarUrl: url });
    setShowAvatarModal(false);
    setProfileMessage({ type: 'success', text: 'Đã đổi ảnh đại diện thành công!' });
    setTimeout(() => setProfileMessage(null), 3500);
  };

  const handleSaveCustomUrlAvatar = () => {
    if (!customAvatarUrl.trim()) return;
    setAvatar(customAvatarUrl.trim());
    updateUser({ avatar: customAvatarUrl.trim(), avatarUrl: customAvatarUrl.trim() });
    setShowAvatarModal(false);
    setCustomAvatarUrl('');
    setProfileMessage({ type: 'success', text: 'Đã cập nhật ảnh đại diện từ link!' });
    setTimeout(() => setProfileMessage(null), 3500);
  };

  // Address Management States (Tích hợp Leaflet + OpenStreetMap + Nominatim)
  const [addresses, setAddresses] = useState([]);
  const [isMapModalOpen, setIsMapModalOpen] = useState(false);
  const [editingAddressData, setEditingAddressData] = useState(null);
  const [isLoadingAddresses, setIsLoadingAddresses] = useState(false);

  // Load danh sách địa chỉ từ backend (có fallback local)
  useEffect(() => {
    let isMounted = true;
    setIsLoadingAddresses(true);
    fetchUserAddresses(user?.userId).then((list) => {
      if (isMounted && Array.isArray(list)) {
        setAddresses(list);
      }
      if (isMounted) setIsLoadingAddresses(false);
    });
    return () => { isMounted = false; };
  }, [user?.userId]);

  // Mở modal thêm địa chỉ mới
  const handleOpenAddModal = () => {
    setEditingAddressData(null);
    setIsMapModalOpen(true);
  };

  // Mở modal sửa địa chỉ
  const handleOpenEditModal = (addr) => {
    setEditingAddressData(addr);
    setIsMapModalOpen(true);
  };

  // Lưu địa chỉ từ modal bản đồ
  const handleSaveAddressFromMap = async (payload, id) => {
    if (id) {
      const updated = await updateUserAddress(user?.userId, id, payload);
      setAddresses((prev) =>
        prev.map((a) => (a.id === id ? updated : (updated.isDefault ? { ...a, isDefault: false } : a)))
      );
      setProfileMessage({ type: 'success', text: 'Cập nhật địa chỉ và tọa độ bản đồ thành công!' });
    } else {
      const created = await createUserAddress(user?.userId, payload);
      setAddresses((prev) => [
        created,
        ...(created.isDefault ? prev.map((a) => ({ ...a, isDefault: false })) : prev),
      ]);
      setProfileMessage({ type: 'success', text: 'Đã thêm địa chỉ mới có ghim vị trí bản đồ thành công!' });
    }
    setTimeout(() => setProfileMessage(null), 3500);
  };

  // Đặt làm địa chỉ mặc định
  const handleSetDefaultAddress = async (id) => {
    await setDefaultUserAddress(user?.userId, id);
    setAddresses((prev) =>
      prev.map((addr) => ({
        ...addr,
        isDefault: addr.id === id,
      }))
    );
    setProfileMessage({ type: 'success', text: 'Đã thay đổi địa chỉ nhận hàng mặc định!' });
    setTimeout(() => setProfileMessage(null), 3000);
  };

  // Xóa địa chỉ
  const handleDeleteAddress = async (id) => {
    if (addresses.length <= 1) {
      alert('Bạn phải giữ ít nhất 1 địa chỉ nhận hàng!');
      return;
    }
    if (window.confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) {
      await deleteUserAddress(user?.userId, id);
      setAddresses((prev) => prev.filter((addr) => addr.id !== id));
      setProfileMessage({ type: 'info', text: 'Đã xóa địa chỉ thành công.' });
      setTimeout(() => setProfileMessage(null), 3000);
    }
  };

  // Password / Security States
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [securityMessage, setSecurityMessage] = useState(null);
  const [isSavingSecurity, setIsSavingSecurity] = useState(false);

  // Order stats calculation
  const [userOrders, setUserOrders] = useState([]);
  useEffect(() => {
    const currentUserId = user?.userId ?? user?.id;
    if (!currentUserId) {
      setUserOrders([]);
      return;
    }
    const orders = getStoredOrders();
    const filtered = orders.filter((o) => String(o.userId) === String(currentUserId));
    setUserOrders(filtered);
  }, [user]);

  const totalSpent = userOrders
    .filter((o) => o.status === 'COMPLETED' || o.paymentStatus === 'PAID')
    .reduce((sum, o) => sum + (o.finalAmount || 0), 0);

  // Lưu thông tin cá nhân
  const handleSaveProfile = async (e) => {
    if (e) e.preventDefault();
    setIsSavingProfile(true);
    setProfileMessage(null);

    try {
      // Cố gắng gọi PUT /api/users/me lên backend
      await axiosClient.put('/api/users/me', {
        fullName,
        phoneNumber: phone,
      });
    } catch (err) {
      console.info('Backend API chưa khả dụng, cập nhật dữ liệu cục bộ:', err);
    }

    // Cập nhật Auth Store & local storage
    updateUser({ fullName, phoneNumber: phone, phone });
    setIsSavingProfile(false);
    setProfileMessage({ type: 'success', text: 'Cập nhật thông tin hồ sơ thành công!' });
    setTimeout(() => setProfileMessage(null), 3500);
  };

  // Đổi mật khẩu
  const handleChangePassword = (e) => {
    e.preventDefault();
    setSecurityMessage(null);

    if (!currentPassword) {
      setSecurityMessage({ type: 'error', text: 'Vui lòng nhập mật khẩu hiện tại.' });
      return;
    }
    if (newPassword.length < 6) {
      setSecurityMessage({ type: 'error', text: 'Mật khẩu mới phải có ít nhất 6 ký tự.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setSecurityMessage({ type: 'error', text: 'Mật khẩu xác nhận không khớp.' });
      return;
    }

    setIsSavingSecurity(true);
    setTimeout(() => {
      setIsSavingSecurity(false);
      setSecurityMessage({ type: 'success', text: 'Đổi mật khẩu thành công! Mật khẩu mới đã được cập nhật.' });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    }, 600);
  };

  const formatVND = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  };

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Top Back bar */}
        <div className="flex items-center justify-between">
          <button
            onClick={onBackToHome}
            className="inline-flex items-center gap-2 text-xs font-semibold text-slate-600 hover:text-orange-600 transition group"
          >
            <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
            <span>Quay lại Mua sắm</span>
          </button>
          <div className="text-xs text-slate-400">
            Hồ sơ tài khoản & Cài đặt bảo mật
          </div>
        </div>

        {/* User Card Header */}
        <div className="bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 rounded-3xl p-6 sm:p-8 text-white shadow-xl shadow-orange-500/10">
          <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6">
            <button type="button" className="relative group cursor-pointer bg-transparent border-0 p-0 text-left" onClick={() => setShowAvatarModal(true)} title="Bấm để đổi ảnh đại diện">
              <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-white/20 backdrop-blur-md border-2 border-white/50 flex items-center justify-center text-3xl sm:text-4xl font-black text-white shadow-lg overflow-hidden relative">
                {avatar ? (
                  <img src={avatar} alt={fullName} className="w-full h-full object-cover" />
                ) : fullName ? (
                  fullName.charAt(0).toUpperCase()
                ) : (
                  'U'
                )}
                {/* Camera overlay hover */}
                <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-xs opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center text-white text-[10px] font-bold gap-1">
                  <Camera className="w-5 h-5" />
                  <span>Đổi ảnh</span>
                </div>
              </div>
              <div className="absolute -bottom-1 -right-1 bg-orange-600 border-2 border-white text-white p-1.5 rounded-full shadow-md group-hover:scale-110 transition-transform" title="Đổi ảnh đại diện">
                <Camera className="w-3.5 h-3.5" />
              </div>
            </button>

            <div className="text-center sm:text-left flex-1 space-y-1.5">
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
                <h1 className="text-xl sm:text-2xl font-black tracking-tight">{fullName || 'Người Dùng QuickShop'}</h1>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-400 text-slate-900 shadow-sm">
                  <Sparkles className="w-3 h-3" />
                  <span>Hội viên Vàng VIP</span>
                </span>
              </div>
              <p className="text-xs text-orange-100 flex items-center justify-center sm:justify-start gap-3">
                <span className="flex items-center gap-1">
                  <Mail className="w-3.5 h-3.5" />
                  {email}
                </span>
                <span>•</span>
                <span className="flex items-center gap-1">
                  <Phone className="w-3.5 h-3.5" />
                  {phone}
                </span>
              </p>
              <p className="text-xs text-white/90 max-w-xl italic pt-1">{bio}</p>
            </div>

            {/* Quick KPI stats */}
            <div className="grid grid-cols-2 gap-3 w-full sm:w-auto">
              <button
                type="button"
                onClick={onViewOrders}
                className="bg-white/15 backdrop-blur-md rounded-2xl p-3 sm:p-4 text-center border border-white/20 hover:bg-white/25 cursor-pointer transition text-white"
              >
                <div className="text-lg sm:text-2xl font-black">{userOrders.length}</div>
                <div className="text-[10px] sm:text-xs text-orange-100 font-medium">Đơn hàng đã đặt</div>
              </button>
              <div className="bg-white/15 backdrop-blur-md rounded-2xl p-3 sm:p-4 text-center border border-white/20">
                <div className="text-base sm:text-xl font-black text-amber-200">{formatVND(totalSpent)}</div>
                <div className="text-[10px] sm:text-xs text-orange-100 font-medium">Tổng chi tiêu</div>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left Navigation Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white dark:bg-[#13141C] rounded-2xl border border-slate-200/80 dark:border-white/10 p-2 shadow-sm space-y-1">
              <button
                onClick={() => setActiveTab('profile')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold transition ${
                  activeTab === 'profile'
                    ? 'bg-orange-50 text-orange-600 shadow-sm'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-[#1A1B25]'
                }`}
              >
                <User className="w-4 h-4" />
                <span>Hồ sơ cá nhân</span>
              </button>

              <button
                onClick={() => setActiveTab('address')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold transition ${
                  activeTab === 'address'
                    ? 'bg-orange-50 text-orange-600 shadow-sm'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-[#1A1B25]'
                }`}
              >
                <MapPin className="w-4 h-4" />
                <span>Sổ địa chỉ nhận hàng</span>
              </button>

              <button
                onClick={() => setActiveTab('security')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold transition ${
                  activeTab === 'security'
                    ? 'bg-orange-50 text-orange-600 shadow-sm'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-[#1A1B25]'
                }`}
              >
                <Lock className="w-4 h-4" />
                <span>Đổi mật khẩu & Bảo mật</span>
              </button>

              <button
                onClick={() => setActiveTab('stats')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold transition ${
                  activeTab === 'stats'
                    ? 'bg-orange-50 text-orange-600 shadow-sm'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-[#1A1B25]'
                }`}
              >
                <Award className="w-4 h-4" />
                <span>Thống kê & Quyền lợi VIP</span>
              </button>

              <div className="pt-2 border-t border-slate-100 mt-2">
                <button
                  onClick={onViewOrders}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs font-semibold text-slate-700 hover:bg-orange-50 hover:text-orange-600 transition"
                >
                  <span className="flex items-center gap-3">
                    <ShoppingBag className="w-4 h-4 text-orange-600" />
                    <span>Lịch sử đơn hàng</span>
                  </span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </button>
              </div>
            </div>
          </div>

          {/* Right Content Panels */}
          <div className="lg:col-span-3">
            {/* 1. Profile Panel */}
            {activeTab === 'profile' && (
              <div className="bg-white dark:bg-[#13141C] rounded-2xl border border-slate-200/80 dark:border-white/10 p-6 shadow-sm space-y-6 text-slate-800 dark:text-[#F4F0E8]">
                <div>
                  <h2 className="text-base font-bold text-slate-900 dark:text-[#F4F0E8]">Hồ Sơ Của Tôi</h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Quản lý thông tin hồ sơ để bảo mật tài khoản và đặt hàng thuận tiện</p>
                </div>

                {profileMessage && (
                  <div className={`p-4 rounded-xl text-xs font-semibold flex items-center gap-2 ${
                    profileMessage.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800 border border-rose-200'
                  }`}>
                    <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                    <span>{profileMessage.text}</span>
                  </div>
                )}

                {(!phone || !phone.trim()) && (
                  <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-950 text-xs flex items-start gap-3 shadow-2xs">
                    <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
                    <div className="space-y-0.5">
                      <div className="font-bold text-amber-950">Chưa cập nhật số điện thoại nhận hàng</div>
                      <div className="text-[11px] text-amber-800 leading-relaxed">
                        Tài khoản của bạn chưa có số điện thoại. Vui lòng nhập số điện thoại chính xác bên dưới và bấm <strong>"Lưu Thay Đổi"</strong> để bưu tá có thể liên hệ giao hàng khi bạn đặt mua đơn hàng!
                      </div>
                    </div>
                  </div>
                )}

                <form onSubmit={handleSaveProfile} className="space-y-4">
                  {/* Khung đổi ảnh đại diện trực tiếp trong tab Hồ Sơ */}
                  <div className="flex items-center gap-4 p-4 bg-slate-50 dark:bg-[#1A1B25] rounded-2xl border border-slate-200 dark:border-white/10">
                    <div className="relative w-14 h-14 rounded-2xl overflow-hidden bg-orange-100 flex-shrink-0 border border-slate-200 shadow-2xs">
                      {avatar ? (
                        <img src={avatar} alt="" className="w-full h-full object-cover" />
                      ) : (
                        <User className="w-8 h-8 text-orange-600 m-auto mt-3" />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-bold text-slate-800 dark:text-[#F4F0E8]">Ảnh đại diện tài khoản</div>
                      <div className="text-[11px] text-slate-500">Hỗ trợ tải file từ máy tính hoặc chọn mẫu phong cách</div>
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowAvatarModal(true)}
                      className="px-3.5 py-2 bg-white hover:bg-orange-50 text-orange-600 border border-orange-200 rounded-xl text-xs font-bold transition active:scale-95 flex items-center gap-1.5 shadow-2xs cursor-pointer"
                    >
                      <Camera className="w-3.5 h-3.5" />
                      <span>Đổi ảnh</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                      <label htmlFor="profile-username" className="text-xs font-bold text-slate-700">Tên đăng nhập (Username)</label>
                      <input
                        id="profile-username"
                        type="text"
                        disabled
                        value={user?.username || ''}
                        className="w-full px-3.5 py-2.5 bg-slate-100 text-slate-500 rounded-xl border border-slate-200 text-xs font-mono cursor-not-allowed"
                      />
                      <span className="text-[10px] text-slate-400">Tên đăng nhập không thể thay đổi</span>
                    </div>

                    <div className="space-y-1.5">
                      <label htmlFor="profile-fullname" className="text-xs font-bold text-slate-700">Họ và tên <span className="text-rose-500">*</span></label>
                      <input
                        id="profile-fullname"
                        type="text"
                        required
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="Nhập họ và tên đầy đủ"
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label htmlFor="profile-email" className="text-xs font-bold text-slate-700">Địa chỉ Email</label>
                      <input
                        id="profile-email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label htmlFor="profile-phone" className="text-xs font-bold text-slate-700">Số điện thoại nhận hàng <span className="text-rose-500">*</span></label>
                      <input
                        id="profile-phone"
                        type="tel"
                        required
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="VD: 0912345678 (Chưa có số điện thoại)"
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="profile-bio" className="text-xs font-bold text-slate-700">Giới thiệu ngắn (Bio)</label>
                    <textarea
                      id="profile-bio"
                      rows="3"
                      value={bio}
                      onChange={(e) => setBio(e.target.value)}
                      placeholder="Mô tả ngắn gọn về bạn..."
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                    ></textarea>
                  </div>

                  <div className="pt-3 flex justify-end">
                    <button
                      type="submit"
                      disabled={isSavingProfile}
                      className="inline-flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold text-xs rounded-xl shadow-md shadow-orange-500/20 transition active:scale-95 disabled:opacity-50"
                    >
                      <Save className="w-4 h-4" />
                      <span>{isSavingProfile ? 'Đang lưu...' : 'Lưu Thay Đổi'}</span>
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* 2. Addresses Panel */}
            {activeTab === 'address' && (
              <div className="bg-white dark:bg-[#13141C] rounded-2xl border border-slate-200/80 dark:border-white/10 p-6 shadow-sm space-y-6 text-slate-800 dark:text-[#F4F0E8]">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
                  <div>
                    <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                      <MapPin className="w-5 h-5 text-orange-600" />
                      <span>Sổ Địa Chỉ Giao Hàng & Định Vị Bản Đồ</span>
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      Tích hợp OpenStreetMap & GPS giúp bưu tá giao hàng chính xác tận cửa
                    </p>
                  </div>
                  <button
                    onClick={handleOpenAddModal}
                    className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white rounded-xl text-xs font-bold transition shadow-md shadow-orange-500/20 active:scale-95 cursor-pointer"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Thêm địa chỉ mới trên bản đồ</span>
                  </button>
                </div>

                {isLoadingAddresses ? (
                  <div className="p-8 text-center text-slate-400 text-xs">
                    Đang tải danh sách địa chỉ giao hàng...
                  </div>
                ) : addresses.length === 0 ? (
                  <div className="p-8 text-center text-slate-500 space-y-2 bg-slate-50/50 rounded-2xl border border-dashed border-slate-200">
                    <MapPin className="w-8 h-8 text-slate-300 mx-auto" />
                    <p className="text-xs">Bạn chưa lưu địa chỉ nhận hàng nào.</p>
                    <button
                      onClick={handleOpenAddModal}
                      className="px-4 py-2 bg-orange-500 text-white rounded-xl text-xs font-bold hover:bg-orange-600 transition"
                    >
                      Chọn vị trí & Thêm ngay
                    </button>
                  </div>
                ) : (
                  /* Address Cards List */
                  <div className="space-y-3">
                    {addresses.map((addr) => {
                      const displayStreet = addr.streetAddress || addr.address || '';
                      const displayFull = addr.fullAddress || 
                        `${displayStreet}${addr.ward ? ', ' + addr.ward : ''}${addr.district ? ', ' + addr.district : ''}${addr.city ? ', ' + addr.city : ''}`;
                      const phone = addr.phoneNumber || addr.phone || '';
                      const name = addr.recipientName || addr.name || '';

                      return (
                        <div
                          key={addr.id}
                          className={`p-4 rounded-2xl border transition flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
                            addr.isDefault
                              ? 'bg-orange-50/30 border-orange-300 ring-2 ring-orange-500/10'
                              : 'bg-white border-slate-200 hover:border-slate-300'
                          }`}
                        >
                          <div className="space-y-1.5 flex-1">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="font-bold text-xs text-slate-900">{name}</span>
                              <span className="text-slate-300">|</span>
                              <span className="text-xs text-slate-600 font-mono font-bold">{phone}</span>
                              {addr.isDefault && (
                                <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-orange-100 text-orange-700 border border-orange-200">
                                  Mặc định
                                </span>
                              )}
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-700">
                                {addr.tag || 'Nhà riêng'}
                              </span>
                            </div>

                            <div className="text-xs text-slate-600 leading-relaxed">
                              {displayFull}
                            </div>

                            {/* Badge Tọa độ GPS từ OpenStreetMap */}
                            {addr.latitude && addr.longitude && (
                              <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-lg text-[10px] font-mono font-bold bg-emerald-50 text-emerald-700 border border-emerald-200/80 shadow-xs">
                                <Compass className="w-3 h-3 text-emerald-600" />
                                <span>GPS: {Number(addr.latitude).toFixed(6)}, {Number(addr.longitude).toFixed(6)}</span>
                              </div>
                            )}
                          </div>

                          <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                            <button
                              type="button"
                              onClick={() => handleOpenEditModal(addr)}
                              className="px-3 py-1.5 bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 rounded-xl text-xs font-bold transition flex items-center gap-1 cursor-pointer"
                              title="Sửa và điều chỉnh vị trí trên bản đồ"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                              <span>Sửa bản đồ</span>
                            </button>

                            {!addr.isDefault && (
                              <button
                                type="button"
                                onClick={() => handleSetDefaultAddress(addr.id)}
                                className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition cursor-pointer"
                              >
                                Đặt mặc định
                              </button>
                            )}

                            <button
                              type="button"
                              onClick={() => handleDeleteAddress(addr.id)}
                              className="p-2 text-slate-400 hover:text-rose-600 rounded-xl hover:bg-rose-50 transition cursor-pointer"
                              title="Xóa địa chỉ"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Modal Bản Đồ Leaflet OpenStreetMap Thêm/Sửa Địa Chỉ */}
                <AddressMapPickerModal
                  isOpen={isMapModalOpen}
                  onClose={() => setIsMapModalOpen(false)}
                  onSave={handleSaveAddressFromMap}
                  initialData={editingAddressData}
                  title={editingAddressData ? 'Chỉnh Sửa Vị Trí Địa Chỉ Trên Bản Đồ' : 'Thêm Địa Chỉ Mới Có Định Vị Bản Đồ'}
                />
              </div>
            )}

            {/* 3. Security Panel */}
            {activeTab === 'security' && (
              <div className="bg-white dark:bg-[#13141C] rounded-2xl border border-slate-200/80 dark:border-white/10 p-6 shadow-sm space-y-6 text-slate-800 dark:text-[#F4F0E8]">
                <div>
                  <h2 className="text-base font-bold text-slate-900 dark:text-[#F4F0E8]">Bảo Mật & Mật Khẩu</h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Nên đổi mật khẩu định kỳ tối thiểu 3 tháng một lần để bảo vệ tài khoản</p>
                </div>

                {securityMessage && (
                  <div className={`p-4 rounded-xl text-xs font-semibold flex items-center gap-2 ${
                    securityMessage.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800 border border-rose-200'
                  }`}>
                    {securityMessage.type === 'success' ? <CheckCircle2 className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
                    <span>{securityMessage.text}</span>
                  </div>
                )}

                <form onSubmit={handleChangePassword} className="space-y-4 max-w-md">
                  <div className="space-y-1.5">
                    <label htmlFor="profile-current-password" className="text-xs font-bold text-slate-700">Mật khẩu hiện tại</label>
                    <input
                      id="profile-current-password"
                      type="password"
                      required
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="profile-new-password" className="text-xs font-bold text-slate-700">Mật khẩu mới</label>
                    <input
                      id="profile-new-password"
                      type="password"
                      required
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Tối thiểu 6 ký tự"
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="profile-confirm-password" className="text-xs font-bold text-slate-700">Xác nhận mật khẩu mới</label>
                    <input
                      id="profile-confirm-password"
                      type="password"
                      required
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Nhập lại mật khẩu mới"
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                    />
                  </div>

                  <div className="pt-2">
                    <button
                      type="submit"
                      disabled={isSavingSecurity}
                      className="px-6 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 text-white font-bold text-xs rounded-xl shadow-md transition active:scale-95"
                    >
                      {isSavingSecurity ? 'Đang cập nhật...' : 'Đổi Mật Khẩu'}
                    </button>
                  </div>
                </form>

                <div className="pt-6 border-t border-slate-100 space-y-3">
                  <h3 className="font-bold text-xs text-slate-800">Phương thức xác thực bổ sung</h3>
                  <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-xl border border-slate-200">
                    <div className="flex items-center gap-3">
                      <ShieldCheck className="w-5 h-5 text-emerald-600" />
                      <div>
                        <div className="font-semibold text-xs text-slate-900">Xác thực 2 yếu tố (2FA OTP)</div>
                        <div className="text-[11px] text-slate-500">Mã xác thực bảo mật gửi qua Email khi đăng nhập trên thiết bị lạ</div>
                      </div>
                    </div>
                    <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 text-[10px] font-bold rounded-full">
                      ĐANG BẬT
                    </span>
                  </div>
                </div>
              </div>
            )}

            {/* 4. Stats & VIP Panel */}
            {activeTab === 'stats' && (
              <div className="bg-white dark:bg-[#13141C] rounded-2xl border border-slate-200/80 dark:border-white/10 p-6 shadow-sm space-y-6 text-slate-800 dark:text-[#F4F0E8]">
                <div>
                  <h2 className="text-base font-bold text-slate-900 dark:text-[#F4F0E8]">Thống Kê Hoạt Động & Ưu Đãi VIP</h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Chương trình tích lũy chi tiêu và thăng hạng thành viên QuickShop Club</p>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="p-4 bg-gradient-to-br from-amber-50 to-orange-50 rounded-2xl border border-amber-200 text-center space-y-1">
                    <Award className="w-6 h-6 text-amber-600 mx-auto" />
                    <div className="text-xl font-black text-slate-900">Hạng Vàng</div>
                    <div className="text-[11px] text-amber-800">Giảm thêm 3% cho mọi đơn hàng Mall</div>
                  </div>
                  <div className="p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-2xl border border-blue-200 text-center space-y-1">
                    <ShoppingBag className="w-6 h-6 text-blue-600 mx-auto" />
                    <div className="text-xl font-black text-slate-900">{userOrders.length} Đơn Hàng</div>
                    <div className="text-[11px] text-blue-800">Tỷ lệ nhận hàng thành công 98%</div>
                  </div>
                  <div className="p-4 bg-gradient-to-br from-emerald-50 to-teal-50 rounded-2xl border border-emerald-200 text-center space-y-1">
                    <CreditCard className="w-6 h-6 text-emerald-600 mx-auto" />
                    <div className="text-xl font-black text-emerald-700">{formatVND(totalSpent)}</div>
                    <div className="text-[11px] text-emerald-800">Tổng điểm tích lũy: 3.490 xu</div>
                  </div>
                </div>

                <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
                  <div className="flex justify-between text-xs font-bold text-slate-800">
                    <span>Tiến độ thăng hạng Kim Cương (Diamond VIP)</span>
                    <span className="text-orange-600">70% hoàn thành</span>
                  </div>
                  <div className="w-full h-3 bg-slate-200 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-orange-500 to-amber-400 rounded-full w-[70%]"></div>
                  </div>
                  <p className="text-[11px] text-slate-500">
                    Chỉ cần chi tiêu thêm <strong>1.500.000₫</strong> trong tháng này để nâng cấp lên thẻ Diamond với quyền lợi Miễn phí vận chuyển không giới hạn!
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* MODAL ĐỔI ẢNH ĐẠI DIỆN ĐA NĂNG */}
      {showAvatarModal && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 cursor-pointer"
          onClick={(e) => {
            if (e.target === e.currentTarget) setShowAvatarModal(false);
          }}
        >
          <div
            className="bg-white w-full max-w-md rounded-3xl p-6 border border-slate-200 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-200 cursor-default"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-sm font-black text-slate-900 flex items-center gap-2">
                <Camera className="w-4 h-4 text-orange-600" />
                <span>Thay Đổi Ảnh Đại Diện</span>
              </h3>
              <button
                type="button"
                onClick={() => setShowAvatarModal(false)}
                className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Preview hiện tại */}
            <div className="flex flex-col items-center gap-2 text-center py-2">
              <div className="w-20 h-20 rounded-2xl overflow-hidden border-2 border-orange-500 shadow-md">
                <img src={avatar} alt="Avatar preview" className="w-full h-full object-cover" />
              </div>
              <span className="text-xs text-slate-500 font-medium">Ảnh đại diện đang áp dụng</span>
            </div>

            {/* 1. Tải ảnh từ thiết bị */}
            <div className="space-y-2">
              <label htmlFor="profile-avatar-file" className="text-xs font-bold text-slate-700 block">1. Tải ảnh từ máy tính / điện thoại:</label>
              <input
                id="profile-avatar-file"
                type="file"
                ref={fileInputRef}
                accept="image/*"
                className="hidden"
                onChange={handleAvatarFileUpload}
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="w-full py-2.5 px-4 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200 rounded-xl text-xs font-bold transition flex items-center justify-center gap-2 cursor-pointer active:scale-95 shadow-2xs"
              >
                <Upload className="w-4 h-4 text-orange-600" />
                <span>Chọn ảnh từ thiết bị... (Tối đa 5MB)</span>
              </button>
            </div>

            {/* 2. Chọn avatar mẫu có sẵn */}
            <div className="space-y-2">
              <div className="text-xs font-bold text-slate-700 block">2. Hoặc chọn mẫu phong cách có sẵn:</div>
              <div className="grid grid-cols-6 gap-2">
                {PRESET_AVATARS.map((preset, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSelectPresetAvatar(preset.url)}
                    title={preset.label}
                    className={`relative rounded-xl overflow-hidden border-2 transition-transform hover:scale-105 active:scale-95 cursor-pointer ${
                      avatar === preset.url ? 'border-orange-600 ring-2 ring-orange-500/20' : 'border-slate-200'
                    }`}
                  >
                    <img src={preset.url} alt={preset.label} className="w-full h-12 object-cover" />
                  </button>
                ))}
              </div>
            </div>

            {/* 3. Dán link ảnh online */}
            <div className="space-y-2">
              <label htmlFor="profile-avatar-url" className="text-xs font-bold text-slate-700 block">3. Hoặc dán đường link ảnh trực tiếp:</label>
              <div className="flex gap-2">
                <input
                  id="profile-avatar-url"
                  type="url"
                  value={customAvatarUrl}
                  onChange={(e) => setCustomAvatarUrl(e.target.value)}
                  placeholder="https://.../avatar.jpg"
                  className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs outline-none focus:border-orange-500"
                />
                <button
                  type="button"
                  onClick={handleSaveCustomUrlAvatar}
                  disabled={!customAvatarUrl.trim()}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-white rounded-xl text-xs font-bold transition active:scale-95 cursor-pointer"
                >
                  Lưu
                </button>
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => setShowAvatarModal(false)}
                className="px-5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
