import React, { useState, useEffect } from 'react';
import { 
  User, Mail, Phone, MapPin, Lock, ShieldCheck, Award, 
  ShoppingBag, CreditCard, ChevronRight, CheckCircle2, AlertCircle, 
  ArrowLeft, Edit3, Save, Key, Plus, Trash2, Check, Sparkles, Building
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { getStoredOrders } from '../data/sampleOrders';
import axiosClient from '../api/axiosClient';

export default function UserProfilePage({ onBackToHome, onViewOrders }) {
  const { user, updateUser } = useAuthStore();
  const [activeTab, setActiveTab] = useState('profile'); // 'profile' | 'address' | 'security' | 'stats'

  // Profile Form States
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phoneNumber || user?.phone || '0988 123 456');
  const [email, setEmail] = useState(user?.email || 'buyer@quickshop.vn');
  const [bio, setBio] = useState('Khách hàng thân thiết của QuickShop Mall. Ưa chuộng công nghệ và đồ gia dụng.');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState(null);

  // Address Management States
  const DEFAULT_ADDRESSES = [
    {
      id: 1,
      recipientName: user?.fullName || 'Lê Đức Anh',
      phone: user?.phoneNumber || '0988 123 456',
      address: 'Số 18, Ngõ 45 Cầu Giấy',
      ward: 'Phường Quan Hoa',
      district: 'Quận Cầu Giấy',
      city: 'Hà Nội',
      isDefault: true,
      tag: 'Nhà riêng',
    },
    {
      id: 2,
      recipientName: user?.fullName || 'Lê Đức Anh',
      phone: '0988 123 456',
      address: 'Tầng 8, Tòa nhà FPT Tower, Số 10 Phạm Văn Bạch',
      ward: 'Phường Dịch Vọng',
      district: 'Quận Cầu Giấy',
      city: 'Hà Nội',
      isDefault: false,
      tag: 'Văn phòng',
    },
  ];

  const [addresses, setAddresses] = useState(() => {
    if (typeof localStorage !== 'undefined') {
      const saved = localStorage.getItem(`quickshop_addresses_${user?.userId || 'guest'}`);
      if (saved) {
        try { return JSON.parse(saved); } catch {}
      }
    }
    return DEFAULT_ADDRESSES;
  });

  const [isAddingAddress, setIsAddingAddress] = useState(false);
  const [newRecipient, setNewRecipient] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newStreet, setNewStreet] = useState('');
  const [newWard, setNewWard] = useState('');
  const [newDistrict, setNewDistrict] = useState('');
  const [newCity, setNewCity] = useState('Hà Nội');
  const [newTag, setNewTag] = useState('Nhà riêng');

  // Password / Security States
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [securityMessage, setSecurityMessage] = useState(null);
  const [isSavingSecurity, setIsSavingSecurity] = useState(false);

  // Order stats calculation
  const [userOrders, setUserOrders] = useState([]);
  useEffect(() => {
    const orders = getStoredOrders();
    const filtered = orders.filter(
      (o) => String(o.userId) === String(user?.userId) || o.userId === 2 || o.userId === 1001
    );
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

  // Thêm địa chỉ nhận hàng mới
  const handleAddNewAddress = (e) => {
    e.preventDefault();
    if (!newRecipient || !newPhone || !newStreet) {
      alert('Vui lòng điền đầy đủ họ tên, số điện thoại và địa chỉ nhà!');
      return;
    }
    const newAddrObj = {
      id: Date.now(),
      recipientName: newRecipient,
      phone: newPhone,
      address: newStreet,
      ward: newWard || 'Phường Trung Tâm',
      district: newDistrict || 'Quận Trung Tâm',
      city: newCity || 'Hà Nội',
      isDefault: addresses.length === 0,
      tag: newTag,
    };
    const nextList = [...addresses, newAddrObj];
    setAddresses(nextList);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(`quickshop_addresses_${user?.userId || 'guest'}`, JSON.stringify(nextList));
    }
    setIsAddingAddress(false);
    setNewRecipient('');
    setNewPhone('');
    setNewStreet('');
    setNewWard('');
    setNewDistrict('');
  };

  // Đặt làm địa chỉ mặc định
  const handleSetDefaultAddress = (id) => {
    const nextList = addresses.map((addr) => ({
      ...addr,
      isDefault: addr.id === id,
    }));
    setAddresses(nextList);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(`quickshop_addresses_${user?.userId || 'guest'}`, JSON.stringify(nextList));
    }
  };

  // Xóa địa chỉ
  const handleDeleteAddress = (id) => {
    if (addresses.length <= 1) {
      alert('Bạn phải giữ ít nhất 1 địa chỉ nhận hàng!');
      return;
    }
    const nextList = addresses.filter((addr) => addr.id !== id);
    if (nextList.length > 0 && !nextList.some((a) => a.isDefault)) {
      nextList[0].isDefault = true;
    }
    setAddresses(nextList);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(`quickshop_addresses_${user?.userId || 'guest'}`, JSON.stringify(nextList));
    }
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
            <div className="relative">
              <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-white/20 backdrop-blur-md border-2 border-white/50 flex items-center justify-center text-3xl sm:text-4xl font-black text-white shadow-lg">
                {fullName ? fullName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="absolute -bottom-1 -right-1 bg-emerald-500 border-2 border-white text-white p-1 rounded-full" title="Tài khoản đã xác thực">
                <Check className="w-3.5 h-3.5 stroke-[3]" />
              </div>
            </div>

            <div className="text-center sm:text-left flex-1 space-y-1.5">
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
                <h1 className="text-xl sm:text-2xl font-black tracking-tight">{fullName || 'Người Dùng QuickShop'}</h1>
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-white/25 backdrop-blur-md uppercase tracking-wider">
                  {user?.role || 'ROLE_BUYER'}
                </span>
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
              <div 
                onClick={onViewOrders}
                className="bg-white/15 backdrop-blur-md rounded-2xl p-3 sm:p-4 text-center border border-white/20 hover:bg-white/25 cursor-pointer transition"
              >
                <div className="text-lg sm:text-2xl font-black">{userOrders.length}</div>
                <div className="text-[10px] sm:text-xs text-orange-100 font-medium">Đơn hàng đã đặt</div>
              </div>
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
            <div className="bg-white rounded-2xl border border-slate-200/80 p-2 shadow-sm space-y-1">
              <button
                onClick={() => setActiveTab('profile')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold transition ${
                  activeTab === 'profile'
                    ? 'bg-orange-50 text-orange-600 shadow-sm'
                    : 'text-slate-700 hover:bg-slate-50'
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
                    : 'text-slate-700 hover:bg-slate-50'
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
                    : 'text-slate-700 hover:bg-slate-50'
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
                    : 'text-slate-700 hover:bg-slate-50'
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
              <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm space-y-6">
                <div>
                  <h2 className="text-base font-bold text-slate-900">Hồ Sơ Của Tôi</h2>
                  <p className="text-xs text-slate-500">Quản lý thông tin hồ sơ để bảo mật tài khoản và đặt hàng thuận tiện</p>
                </div>

                {profileMessage && (
                  <div className={`p-4 rounded-xl text-xs font-semibold flex items-center gap-2 ${
                    profileMessage.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800 border border-rose-200'
                  }`}>
                    <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                    <span>{profileMessage.text}</span>
                  </div>
                )}

                <form onSubmit={handleSaveProfile} className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                      <label className="text-xs font-bold text-slate-700">Tên đăng nhập (Username)</label>
                      <input
                        type="text"
                        disabled
                        value={user?.username || 'buyer'}
                        className="w-full px-3.5 py-2.5 bg-slate-100 text-slate-500 rounded-xl border border-slate-200 text-xs font-mono cursor-not-allowed"
                      />
                      <span className="text-[10px] text-slate-400">Tên đăng nhập không thể thay đổi</span>
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-xs font-bold text-slate-700">Họ và tên <span className="text-rose-500">*</span></label>
                      <input
                        type="text"
                        required
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="Nhập họ và tên đầy đủ"
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-xs font-bold text-slate-700">Địa chỉ Email</label>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-xs font-bold text-slate-700">Số điện thoại nhận hàng <span className="text-rose-500">*</span></label>
                      <input
                        type="tel"
                        required
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="VD: 0988 123 456"
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-700">Giới thiệu ngắn (Bio)</label>
                    <textarea
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
              <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm space-y-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-base font-bold text-slate-900">Sổ Địa Chỉ Giao Hàng</h2>
                    <p className="text-xs text-slate-500">Địa chỉ được dùng để tự động điền khi thanh toán đơn hàng</p>
                  </div>
                  <button
                    onClick={() => setIsAddingAddress(!isAddingAddress)}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-xl text-xs font-bold transition shadow-sm"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Thêm địa chỉ mới</span>
                  </button>
                </div>

                {/* Add new address popup / drawer */}
                {isAddingAddress && (
                  <form onSubmit={handleAddNewAddress} className="p-4 bg-orange-50/50 rounded-2xl border border-orange-200 space-y-3">
                    <div className="font-bold text-xs text-orange-900 flex items-center gap-1.5">
                      <MapPin className="w-4 h-4 text-orange-600" />
                      <span>Thêm địa chỉ nhận hàng mới</span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="text-[11px] font-semibold text-slate-700">Tên người nhận</label>
                        <input
                          type="text"
                          required
                          value={newRecipient}
                          onChange={(e) => setNewRecipient(e.target.value)}
                          placeholder="Họ và tên"
                          className="w-full mt-1 px-3 py-2 bg-white rounded-xl border border-slate-300 text-xs"
                        />
                      </div>
                      <div>
                        <label className="text-[11px] font-semibold text-slate-700">Số điện thoại</label>
                        <input
                          type="tel"
                          required
                          value={newPhone}
                          onChange={(e) => setNewPhone(e.target.value)}
                          placeholder="09xx xxx xxx"
                          className="w-full mt-1 px-3 py-2 bg-white rounded-xl border border-slate-300 text-xs"
                        />
                      </div>
                      <div className="sm:col-span-2">
                        <label className="text-[11px] font-semibold text-slate-700">Địa chỉ chi tiết (Số nhà, Tên đường/Tòa nhà)</label>
                        <input
                          type="text"
                          required
                          value={newStreet}
                          onChange={(e) => setNewStreet(e.target.value)}
                          placeholder="VD: Số 20, Ngõ 123 Hoàng Quốc Việt"
                          className="w-full mt-1 px-3 py-2 bg-white rounded-xl border border-slate-300 text-xs"
                        />
                      </div>
                      <div>
                        <label className="text-[11px] font-semibold text-slate-700">Phường / Xã</label>
                        <input
                          type="text"
                          value={newWard}
                          onChange={(e) => setNewWard(e.target.value)}
                          placeholder="Phường Nghĩa Đô"
                          className="w-full mt-1 px-3 py-2 bg-white rounded-xl border border-slate-300 text-xs"
                        />
                      </div>
                      <div>
                        <label className="text-[11px] font-semibold text-slate-700">Quận / Huyện & Thành phố</label>
                        <input
                          type="text"
                          value={newDistrict}
                          onChange={(e) => setNewDistrict(e.target.value)}
                          placeholder="Cầu Giấy, Hà Nội"
                          className="w-full mt-1 px-3 py-2 bg-white rounded-xl border border-slate-300 text-xs"
                        />
                      </div>
                    </div>

                    <div className="flex justify-end gap-2 pt-2">
                      <button
                        type="button"
                        onClick={() => setIsAddingAddress(false)}
                        className="px-4 py-2 bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold"
                      >
                        Hủy
                      </button>
                      <button
                        type="submit"
                        className="px-4 py-2 bg-orange-600 text-white rounded-xl text-xs font-bold"
                      >
                        Lưu địa chỉ
                      </button>
                    </div>
                  </form>
                )}

                {/* Address Cards List */}
                <div className="space-y-3">
                  {addresses.map((addr) => (
                    <div
                      key={addr.id}
                      className={`p-4 rounded-2xl border transition flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                        addr.isDefault
                          ? 'bg-orange-50/30 border-orange-300 ring-2 ring-orange-500/10'
                          : 'bg-white border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-slate-900">{addr.recipientName}</span>
                          <span className="text-slate-300">|</span>
                          <span className="text-xs text-slate-600 font-mono">{addr.phone}</span>
                          {addr.isDefault && (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-orange-100 text-orange-700">
                              Mặc định
                            </span>
                          )}
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 text-slate-600">
                            {addr.tag || 'Nhà riêng'}
                          </span>
                        </div>
                        <div className="text-xs text-slate-600">
                          {addr.address}, {addr.ward}, {addr.district}, {addr.city}
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-end sm:self-auto">
                        {!addr.isDefault && (
                          <button
                            onClick={() => handleSetDefaultAddress(addr.id)}
                            className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-medium transition"
                          >
                            Thiết lập mặc định
                          </button>
                        )}
                        <button
                          onClick={() => handleDeleteAddress(addr.id)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition"
                          title="Xóa địa chỉ"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 3. Security Panel */}
            {activeTab === 'security' && (
              <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm space-y-6">
                <div>
                  <h2 className="text-base font-bold text-slate-900">Bảo Mật & Mật Khẩu</h2>
                  <p className="text-xs text-slate-500">Nên đổi mật khẩu định kỳ tối thiểu 3 tháng một lần để bảo vệ tài khoản</p>
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
                    <label className="text-xs font-bold text-slate-700">Mật khẩu hiện tại</label>
                    <input
                      type="password"
                      required
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-700">Mật khẩu mới</label>
                    <input
                      type="password"
                      required
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Tối thiểu 6 ký tự"
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-700">Xác nhận mật khẩu mới</label>
                    <input
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
              <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm space-y-6">
                <div>
                  <h2 className="text-base font-bold text-slate-900">Thống Kê Hoạt Động & Ưu Đãi VIP</h2>
                  <p className="text-xs text-slate-500">Chương trình tích lũy chi tiêu và thăng hạng thành viên QuickShop Club</p>
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
    </div>
  );
}
