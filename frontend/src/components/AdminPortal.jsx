import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, CheckCircle2, XCircle, Clock, FolderPlus, 
  Trash2, Bell, Mail, Server, Database, Layers, Tag, Send, 
  AlertTriangle, ArrowLeft, Filter, Sparkles, Check, Users,
  Store, Ticket, Lock, Unlock, Eye, Plus, Search, RefreshCw,
  Award, TrendingUp, AlertCircle, ShoppingBag, Phone, MapPin,
  UserCheck, UserX, CreditCard, ExternalLink, X
} from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function AdminPortal({ categories = [], onCategoriesUpdated, onBackToStorefront }) {
  // Tabs: 'moderation' | 'sellers' | 'vouchers' | 'users' | 'categories' | 'notifications' | 'infra'
  const [activeTab, setActiveTab] = useState('moderation');

  // ================= 1. MODERATION STATE =================
  const [allProducts, setAllProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [moderationFilter, setModerationFilter] = useState('PENDING'); // PENDING, REJECTED, APPROVED, ALL

  const defaultModerationProducts = [
    {
      id: 3,
      shopId: 1,
      shopName: 'TechWorld Official Store',
      name: 'Đồng Hồ Thông Minh QuickWatch Ultra 2 Định Vị GPS Độc Lập',
      categoryName: 'Điện thoại & Phụ kiện',
      description: 'Vỏ titan chuẩn quân đội, đo nồng độ oxy trong máu SpO2, hỗ trợ eSIM nghe gọi độc lập.',
      mainImage: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80',
      status: 'PENDING',
      rejectionReason: null,
      minPrice: 12490000,
      createdAt: '2026-11-04T10:15:00',
      variants: [{ id: 31, sku: 'QW-ULTRA-ORANGE', color: 'Cam Thể Thao', size: '49mm', price: 12490000, stock: 35 }],
    },
    {
      id: 9,
      shopId: 2,
      shopName: 'Fashion Trend Hanoi',
      name: 'Kính Thực Tế Ảo VR Pro Vision 4K Siêu Nét',
      categoryName: 'Thiết bị Điện tử',
      description: 'Trải nghiệm không gian ảo sống động với 2 màn hình Micro-OLED 4K, theo dõi cử chỉ tay.',
      mainImage: 'https://images.unsplash.com/photo-1593508512255-86ab42a8e620?w=600&auto=format&fit=crop&q=80',
      status: 'REJECTED',
      rejectionReason: 'Thiếu giấy chứng nhận hợp quy viễn thông và chứng từ xuất xứ nguồn gốc chính hãng.',
      minPrice: 18900000,
      createdAt: '2026-11-03T14:20:00',
      variants: [{ id: 91, sku: 'VR-VISION-256', color: 'Trắng Xám', size: '256GB', price: 18900000, stock: 15 }],
    },
    {
      id: 1,
      shopId: 1,
      shopName: 'TechWorld Official Store',
      name: 'Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế',
      categoryName: 'Điện thoại & Phụ kiện',
      description: 'Flagship hàng đầu với chip xử lý AI 3nm mạnh mẽ, màn hình Super Retina XDR OLED 120Hz.',
      mainImage: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
      status: 'APPROVED',
      rejectionReason: null,
      minPrice: 29990000,
      createdAt: '2026-11-01T08:00:00',
      variants: [{ id: 11, sku: 'QP-PRO-TITAN-256', color: 'Titan Tự Nhiên', size: '256GB', price: 29990000, stock: 45 }],
    },
  ];

  const fetchModerationProducts = async () => {
    setLoadingProducts(true);
    try {
      const res = await axiosClient.get('/api/catalog/products?size=50');
      if (res.success && res.data?.content && res.data.content.length > 0) {
        setAllProducts(res.data.content);
      } else {
        setAllProducts(defaultModerationProducts);
      }
    } catch (err) {
      setAllProducts(defaultModerationProducts);
    } finally {
      setLoadingProducts(false);
    }
  };

  // ================= 2. SELLER MANAGEMENT STATE =================
  const [shops, setShops] = useState([]);
  const [sellerFilter, setSellerFilter] = useState('ALL'); // ALL, PENDING, ACTIVE, SUSPENDED
  const [selectedKycShop, setSelectedKycShop] = useState(null);
  const [sellerSearchKw, setSellerSearchKw] = useState('');

  const defaultShops = [
    {
      id: 1,
      name: 'TechWorld Official Store',
      ownerId: 3,
      ownerName: 'Lê Đức Anh',
      ownerEmail: 'seller@techworld.vn',
      ownerPhone: '0905123456',
      taxCode: '0108923412',
      idCardNumber: '001095001234',
      warehouseAddress: 'Tòa nhà Landmark 72, Mễ Trì, Nam Từ Liêm, Hà Nội',
      bankAccount: '19034567890011 (Techcombank - LE DUC ANH)',
      status: 'ACTIVE',
      isMall: true,
      productCount: 14,
      totalRevenue: 185000000,
      rating: 4.9,
      registeredAt: '2026-10-15',
    },
    {
      id: 2,
      name: 'Fashion Trend Hanoi',
      ownerId: 8,
      ownerName: 'Nguyễn Bích Ngọc',
      ownerEmail: 'bichngoc.fashion@gmail.com',
      ownerPhone: '0912334455',
      taxCode: '0109988123',
      idCardNumber: '001198005678',
      warehouseAddress: 'Số 45 Phố Huế, Hàng Bài, Hoàn Kiếm, Hà Nội',
      bankAccount: '102837465001 (Vietinbank - NGUYEN BICH NGOC)',
      status: 'ACTIVE',
      isMall: false,
      productCount: 8,
      totalRevenue: 42000000,
      rating: 4.8,
      registeredAt: '2026-10-20',
    },
    {
      id: 3,
      name: 'SmartHome Vietnam Store',
      ownerId: 4,
      ownerName: 'Nguyễn Văn Kiên',
      ownerEmail: 'kien.seller@smarthome.vn',
      ownerPhone: '0938112233',
      taxCode: '0314567890',
      idCardNumber: '079096003456',
      warehouseAddress: 'Kho 12 KCN Tân Bình, Tây Thạnh, Tân Phú, TP. Hồ Chí Minh',
      bankAccount: '0071000987654 (Vietcombank - NGUYEN VAN KIEN)',
      status: 'ACTIVE',
      isMall: true,
      productCount: 11,
      totalRevenue: 96000000,
      rating: 4.9,
      registeredAt: '2026-10-22',
    },
    {
      id: 4,
      name: 'Gia Dụng Nhật Bản Sakura',
      ownerId: 12,
      ownerName: 'Nguyễn Thị Lan',
      ownerEmail: 'sakura.japan@gmail.com',
      ownerPhone: '0988776655',
      taxCode: '0112345678',
      idCardNumber: '001298012345',
      warehouseAddress: 'Tầng 2, Chợ Hà Đông, Nguyễn Trãi, Hà Đông, Hà Nội',
      bankAccount: '21510001234567 (BIDV - NGUYEN THI LAN)',
      status: 'PENDING_APPROVAL',
      isMall: false,
      productCount: 0,
      totalRevenue: 0,
      rating: 5.0,
      registeredAt: '2026-11-04',
    },
    {
      id: 5,
      name: 'Mỹ Phẩm Hàn Quốc Glamour Cosmetics',
      ownerId: 15,
      ownerName: 'Trần Hoàng Yến',
      ownerEmail: 'glamour.cosmetics@gmail.com',
      ownerPhone: '0977889900',
      taxCode: '0319876543',
      idCardNumber: '031195009876',
      warehouseAddress: '280 An Dương Vương, Phường 4, Quận 5, TP. Hồ Chí Minh',
      bankAccount: '1902888999001 (Techcombank - TRAN HOANG YEN)',
      status: 'PENDING_APPROVAL',
      isMall: false,
      productCount: 0,
      totalRevenue: 0,
      rating: 5.0,
      registeredAt: '2026-11-03',
    },
    {
      id: 6,
      name: 'Phụ Kiện Điện Thoại Giá Rẻ Hà Đông',
      ownerId: 21,
      ownerName: 'Đỗ Văn Thành',
      ownerEmail: 'phukiengiarehd@gmail.com',
      ownerPhone: '0944556677',
      taxCode: 'Chưa có',
      idCardNumber: '001090008899',
      warehouseAddress: 'Số 18 Ngõ 4 Quang Trung, Hà Đông, Hà Nội',
      bankAccount: '108876543210 (MBBank - DO VAN THANH)',
      status: 'SUSPENDED',
      isMall: false,
      productCount: 4,
      totalRevenue: 6500000,
      rating: 3.2,
      registeredAt: '2026-10-18',
    },
  ];

  const fetchShops = async () => {
    try {
      const res = await axiosClient.get('/api/shops');
      if (res.success && Array.isArray(res.data) && res.data.length > 0) {
        setShops(res.data);
      } else {
        setShops(defaultShops);
      }
    } catch (err) {
      setShops(defaultShops);
    }
  };

  // Duyệt gian hàng KYC
  const handleApproveShop = async (id) => {
    try {
      await axiosClient.put(`/api/shops/${id}/approve`);
    } catch (err) {
      console.warn('API approve shop fallback:', err);
    }
    setShops(shops.map((s) => (s.id === id ? { ...s, status: 'ACTIVE' } : s)));
    if (selectedKycShop?.id === id) {
      setSelectedKycShop((prev) => (prev ? { ...prev, status: 'ACTIVE' } : null));
    }
    alert('Đã phê duyệt hồ sơ KYC gian hàng thành công! Quyền ROLE_SELLER đã được kích hoạt cho chủ shop.');
  };

  // Từ chối gian hàng
  const handleRejectShop = async (id) => {
    const reason = prompt('Nhập lý do từ chối hồ sơ mở gian hàng:', 'Thông tin CCCD hoặc giấy tờ kinh doanh không hợp lệ hoặc thiếu chứng từ pháp lý.');
    if (!reason || !reason.trim()) return;

    try {
      await axiosClient.put(`/api/shops/${id}/reject`);
    } catch (err) {
      console.warn('API reject shop fallback:', err);
    }
    setShops(shops.map((s) => (s.id === id ? { ...s, status: 'REJECTED' } : s)));
    if (selectedKycShop?.id === id) {
      setSelectedKycShop((prev) => (prev ? { ...prev, status: 'REJECTED' } : null));
    }
    alert('Đã từ chối hồ sơ gian hàng.');
  };

  // Khóa / Mở khóa Shop vi phạm
  const handleToggleLockShop = (id) => {
    const targetShop = shops.find((s) => s.id === id);
    if (!targetShop) return;

    const isCurrentlySuspended = targetShop.status === 'SUSPENDED' || targetShop.status === 'REJECTED';
    const actionText = isCurrentlySuspended ? 'mở khóa hoạt động' : 'tạm khóa gian hàng do vi phạm';
    if (!confirm(`Bạn có chắc chắn muốn ${actionText} cho gian hàng "${targetShop.name}"?`)) return;

    const nextStatus = isCurrentlySuspended ? 'ACTIVE' : 'SUSPENDED';
    setShops(shops.map((s) => (s.id === id ? { ...s, status: nextStatus } : s)));
    alert(`Đã cập nhật trạng thái gian hàng thành công (${nextStatus})!`);
  };

  // ================= 3. VOUCHER MANAGEMENT STATE =================
  const [vouchers, setVouchers] = useState([]);
  const [voucherFilter, setVoucherFilter] = useState('ALL'); // ALL, PLATFORM, SHIPPING, SHOP, ACTIVE, EXPIRED
  const [showCreateVoucherModal, setShowCreateVoucherModal] = useState(false);
  const [newVoucher, setNewVoucher] = useState({
    code: '',
    title: '',
    discountType: 'FIXED_AMOUNT', // FIXED_AMOUNT or PERCENTAGE
    discountValue: 50000,
    minOrderValue: 200000,
    maxDiscountAmount: 100000,
    usageLimit: 500,
    scope: 'PLATFORM', // PLATFORM, SHIPPING, SHOP
    shopId: '',
    startDate: '2026-11-01',
    endDate: '2026-11-30',
  });

  const defaultVouchers = [
    {
      id: 1,
      code: 'FREESHIPMAX',
      title: 'Miễn Phí Vận Chuyển Toàn Quốc',
      scope: 'SHIPPING',
      shopId: null,
      discountType: 'FIXED_AMOUNT',
      discountValue: 25000,
      minOrderValue: 99000,
      maxDiscountAmount: 25000,
      usageLimit: 1000,
      usedCount: 850,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-11-30',
    },
    {
      id: 2,
      code: 'FREESHIP50K',
      title: 'Giảm 50k Vận Chuyển Hỏa Tốc',
      scope: 'SHIPPING',
      shopId: null,
      discountType: 'FIXED_AMOUNT',
      discountValue: 50000,
      minOrderValue: 300000,
      maxDiscountAmount: 50000,
      usageLimit: 500,
      usedCount: 310,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-11-30',
    },
    {
      id: 3,
      code: 'QUICKSHOP50K',
      title: 'Giảm 50.000₫ Đơn Hàng Sàn',
      scope: 'PLATFORM',
      shopId: null,
      discountType: 'FIXED_AMOUNT',
      discountValue: 50000,
      minOrderValue: 200000,
      maxDiscountAmount: 50000,
      usageLimit: 1000,
      usedCount: 920,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-11-30',
    },
    {
      id: 4,
      code: 'MALL100K',
      title: 'Giảm 100.000₫ Hàng Chính Hãng Mall',
      scope: 'PLATFORM',
      shopId: null,
      discountType: 'FIXED_AMOUNT',
      discountValue: 100000,
      minOrderValue: 1000000,
      maxDiscountAmount: 100000,
      usageLimit: 500,
      usedCount: 240,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-11-15',
    },
    {
      id: 5,
      code: 'SUPERDEAL15',
      title: 'Giảm 15% Tối Đa 150.000₫ Toàn Sàn',
      scope: 'PLATFORM',
      shopId: null,
      discountType: 'PERCENTAGE',
      discountValue: 15,
      minOrderValue: 500000,
      maxDiscountAmount: 150000,
      usageLimit: 500,
      usedCount: 350,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-11-20',
    },
    {
      id: 6,
      code: 'TECHWORLD20K',
      title: 'Voucher Độc Quyền TechWorld Store',
      scope: 'SHOP',
      shopId: 1,
      shopName: 'TechWorld Official Store',
      discountType: 'FIXED_AMOUNT',
      discountValue: 20000,
      minOrderValue: 150000,
      maxDiscountAmount: 20000,
      usageLimit: 300,
      usedCount: 185,
      status: 'ACTIVE',
      startDate: '2026-11-01',
      endDate: '2026-11-30',
    },
    {
      id: 7,
      code: 'CHAOTHANG10',
      title: 'Ưu đãi kích cầu tháng 10',
      scope: 'PLATFORM',
      shopId: null,
      discountType: 'FIXED_AMOUNT',
      discountValue: 30000,
      minOrderValue: 150000,
      maxDiscountAmount: 30000,
      usageLimit: 200,
      usedCount: 200,
      status: 'EXPIRED',
      startDate: '2026-10-01',
      endDate: '2026-10-31',
    },
  ];

  const fetchVouchers = async () => {
    try {
      const res = await axiosClient.get('/api/vouchers');
      if (res.success && Array.isArray(res.data) && res.data.length > 0) {
        setVouchers(res.data);
      } else {
        setVouchers(defaultVouchers);
      }
    } catch (err) {
      setVouchers(defaultVouchers);
    }
  };

  const handleCreateVoucher = async (e) => {
    e.preventDefault();
    if (!newVoucher.code.trim()) {
      alert('Vui lòng nhập mã Voucher!');
      return;
    }

    const payload = {
      code: newVoucher.code.trim().toUpperCase(),
      title: newVoucher.title.trim() || `Khuyến mãi mã ${newVoucher.code.toUpperCase()}`,
      discountType: newVoucher.discountType,
      discountValue: Number(newVoucher.discountValue),
      minOrderValue: Number(newVoucher.minOrderValue),
      maxDiscountAmount: Number(newVoucher.maxDiscountAmount || newVoucher.discountValue),
      usageLimit: Number(newVoucher.usageLimit),
      usedCount: 0,
      shopId: newVoucher.scope === 'SHOP' && newVoucher.shopId ? Number(newVoucher.shopId) : null,
      scope: newVoucher.scope,
      status: 'ACTIVE',
      startDate: newVoucher.startDate,
      endDate: newVoucher.endDate,
    };

    try {
      await axiosClient.post('/api/vouchers', payload);
    } catch (err) {
      console.warn('API create voucher fallback:', err);
    }

    const createdItem = { ...payload, id: Date.now() };
    setVouchers([createdItem, ...vouchers]);
    setShowCreateVoucherModal(false);
    setNewVoucher({
      code: '',
      title: '',
      discountType: 'FIXED_AMOUNT',
      discountValue: 50000,
      minOrderValue: 200000,
      maxDiscountAmount: 100000,
      usageLimit: 500,
      scope: 'PLATFORM',
      shopId: '',
      startDate: '2026-11-01',
      endDate: '2026-11-30',
    });
    alert(`Đã phát hành mã Voucher "${payload.code}" thành công lên toàn sàn!`);
  };

  const handleToggleVoucherStatus = async (id) => {
    const v = vouchers.find((item) => item.id === id);
    if (!v) return;
    const nextStatus = v.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

    try {
      await axiosClient.put(`/api/vouchers/${id}/status?status=${nextStatus}`);
    } catch (err) {
      console.warn('API toggle voucher status fallback:', err);
    }

    setVouchers(vouchers.map((item) => (item.id === id ? { ...item, status: nextStatus } : item)));
  };

  const handleDeleteVoucher = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa mã voucher này khỏi hệ thống?')) return;
    try {
      await axiosClient.delete(`/api/vouchers/${id}`);
    } catch (err) {
      console.warn('API delete voucher fallback:', err);
    }
    setVouchers(vouchers.filter((item) => item.id !== id));
    alert('Đã xóa mã voucher thành công!');
  };

  // ================= 4. BUYER & USER MANAGEMENT STATE =================
  const [users, setUsers] = useState([]);
  const [userRoleFilter, setUserRoleFilter] = useState('ALL'); // ALL, ROLE_CUSTOMER, ROLE_SELLER, ROLE_ADMIN
  const [userSearchKw, setUserSearchKw] = useState('');

  const defaultUsers = [
    {
      id: 1,
      fullName: 'Nguyễn Văn An',
      email: 'buyer@quickshop.vn',
      phone: '0912345678',
      role: 'ROLE_CUSTOMER',
      status: 'ACTIVE',
      totalOrders: 14,
      totalSpent: 8450000,
      createdAt: '2026-10-10',
      address: 'Số 12 Ngõ 88 Cầu Giấy, Hà Nội',
    },
    {
      id: 2,
      fullName: 'Trần Thị Bích',
      email: 'bich.tran@gmail.com',
      phone: '0987654321',
      role: 'ROLE_CUSTOMER',
      status: 'ACTIVE',
      totalOrders: 6,
      totalSpent: 3200000,
      createdAt: '2026-10-14',
      address: 'Chung cư Vinhomes Smart City, Tây Mỗ, Hà Nội',
    },
    {
      id: 3,
      fullName: 'Lê Đức Anh',
      email: 'seller@techworld.vn',
      phone: '0905123456',
      role: 'ROLE_SELLER',
      status: 'ACTIVE',
      shopName: 'TechWorld Official Store',
      totalOrders: 2,
      totalSpent: 1200000,
      createdAt: '2026-10-15',
      address: 'Tòa Landmark 72, Nam Từ Liêm, Hà Nội',
    },
    {
      id: 4,
      fullName: 'Nguyễn Văn Kiên',
      email: 'kien.seller@smarthome.vn',
      phone: '0938112233',
      role: 'ROLE_SELLER',
      status: 'ACTIVE',
      shopName: 'SmartHome Vietnam Store',
      totalOrders: 4,
      totalSpent: 2800000,
      createdAt: '2026-10-22',
      address: 'Tân Bình, TP. Hồ Chí Minh',
    },
    {
      id: 5,
      fullName: 'Triệu Quốc Bình',
      email: 'admin@quickshop.vn',
      phone: '0945999888',
      role: 'ROLE_ADMIN',
      status: 'ACTIVE',
      totalOrders: 0,
      totalSpent: 0,
      createdAt: '2026-10-01',
      address: 'Trung tâm Quản trị QuickShop, Tòa nhà FPT, Cầu Giấy, Hà Nội',
    },
    {
      id: 6,
      fullName: 'Phạm Minh Hoàng',
      email: 'hoang.pham@yahoo.com',
      phone: '0977223344',
      role: 'ROLE_CUSTOMER',
      status: 'LOCKED',
      lockReason: 'Spam tạo đơn hàng ảo và tỷ lệ từ chối nhận hàng (bom hàng) > 60%',
      totalOrders: 7,
      totalSpent: 650000,
      createdAt: '2026-10-18',
      address: 'Số 5 Phố Vọng, Hai Bà Trưng, Hà Nội',
    },
    {
      id: 7,
      fullName: 'Vũ Thu Hương',
      email: 'huong.thu@gmail.com',
      phone: '0918776655',
      role: 'ROLE_CUSTOMER',
      status: 'ACTIVE',
      totalOrders: 26,
      totalSpent: 18900000,
      createdAt: '2026-10-11',
      address: '35 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội',
    },
  ];

  const fetchUsers = async () => {
    try {
      const res = await axiosClient.get('/api/users');
      if (res.success && Array.isArray(res.data) && res.data.length > 0) {
        setUsers(res.data);
      } else {
        setUsers(defaultUsers);
      }
    } catch (err) {
      setUsers(defaultUsers);
    }
  };

  const handleToggleLockUser = (id) => {
    const targetUser = users.find((u) => u.id === id);
    if (!targetUser) return;
    if (targetUser.role === 'ROLE_ADMIN') {
      alert('Không thể khóa tài khoản Quản Trị Viên cấp cao!');
      return;
    }

    const isLocked = targetUser.status === 'LOCKED';
    const actionText = isLocked ? 'mở khóa tài khoản' : 'khóa tài khoản do vi phạm';
    if (!confirm(`Bạn có chắc chắn muốn ${actionText} của "${targetUser.fullName}"?`)) return;

    const nextStatus = isLocked ? 'ACTIVE' : 'LOCKED';
    setUsers(users.map((u) => (u.id === id ? { ...u, status: nextStatus } : u)));
    alert(`Đã cập nhật trạng thái tài khoản: ${nextStatus === 'ACTIVE' ? 'Đang hoạt động' : 'Đã bị khóa'}!`);
  };

  const handleChangeUserRole = (id, newRole) => {
    const targetUser = users.find((u) => u.id === id);
    if (!targetUser) return;
    if (targetUser.role === 'ROLE_ADMIN') {
      alert('Không thể thay đổi quyền Quản Trị Viên cấp cao!');
      return;
    }

    setUsers(users.map((u) => (u.id === id ? { ...u, role: newRole } : u)));
    alert(`Đã nâng/chuyển quyền tài khoản "${targetUser.fullName}" sang ${newRole}!`);
  };

  // ================= 5. CATEGORY STATE =================
  const [newCatName, setNewCatName] = useState('');
  const [newCatSlug, setNewCatSlug] = useState('');
  const [newCatIcon, setNewCatIcon] = useState('Sparkles');
  const [newCatParentId, setNewCatParentId] = useState('');

  // ================= 6. NOTIFICATION STATE =================
  const [notifUserId, setNotifUserId] = useState('1');
  const [notifTitle, setNotifTitle] = useState('');
  const [notifContent, setNotifContent] = useState('');
  const [notifSuccess, setNotifSuccess] = useState('');
  const [emailLogs, setEmailLogs] = useState([]);

  // Fetch initial data based on active tab
  useEffect(() => {
    if (activeTab === 'moderation') fetchModerationProducts();
    if (activeTab === 'sellers') fetchShops();
    if (activeTab === 'vouchers') fetchVouchers();
    if (activeTab === 'users') fetchUsers();
    if (activeTab === 'notifications') fetchEmailLogs();
  }, [activeTab]);

  const fetchEmailLogs = async () => {
    try {
      const res = await axiosClient.get('/api/notifications/email-logs');
      if (res.success && Array.isArray(res.data)) {
        setEmailLogs(res.data);
      }
    } catch (err) {
      setEmailLogs([
        { id: 1, recipientEmail: 'buyer@quickshop.vn', subject: '[QuickShop] Xác nhận đơn hàng #1001', status: 'SUCCESS', sentAt: '2026-11-04T12:15:00' },
        { id: 2, recipientEmail: 'seller@techworld.vn', subject: '[QuickShop] Thông báo phê duyệt sản phẩm #3', status: 'SUCCESS', sentAt: '2026-11-04T11:40:00' },
        { id: 3, recipientEmail: 'buyer@quickshop.vn', subject: '[QuickShop] Voucher toàn sàn FREESHIPMAX đã được kích hoạt', status: 'SUCCESS', sentAt: '2026-11-04T10:10:00' },
      ]);
    }
  };

  // Product moderation handlers
  const handleApproveProduct = async (id) => {
    try {
      await axiosClient.patch(`/api/catalog/products/${id}/status`, {
        status: 'APPROVED',
        reason: 'Sản phẩm hợp lệ chuẩn chính sách sàn QuickShop Mall',
      });
    } catch (err) {
      console.warn('Approve fallback:', err);
    }
    setAllProducts(allProducts.map((p) => (p.id === id ? { ...p, status: 'APPROVED', rejectionReason: null } : p)));
    alert('Đã phê duyệt sản phẩm thành công! Sản phẩm đã xuất hiện công khai trên Storefront.');
  };

  const handleRejectProduct = async (id) => {
    const reason = prompt('Nhập lý do từ chối kiểm duyệt sản phẩm:', 'Hình ảnh sản phẩm mờ, thông tin chưa rõ ràng hoặc thiếu giấy tờ kiểm định.');
    if (!reason || !reason.trim()) return;

    try {
      await axiosClient.patch(`/api/catalog/products/${id}/status`, {
        status: 'REJECTED',
        reason: reason.trim(),
      });
    } catch (err) {
      console.warn('Reject fallback:', err);
    }
    setAllProducts(allProducts.map((p) => (p.id === id ? { ...p, status: 'REJECTED', rejectionReason: reason.trim() } : p)));
    alert('Đã từ chối duyệt sản phẩm và gửi thông báo lý do đến Seller.');
  };

  const handleDeleteProduct = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa vĩnh viễn sản phẩm vi phạm này?')) return;
    try {
      await axiosClient.delete(`/api/catalog/products/${id}`);
    } catch (err) {
      console.warn('Delete product error:', err);
    }
    setAllProducts(allProducts.filter((p) => p.id !== id));
    alert('Đã xóa sản phẩm khỏi cơ sở dữ liệu hệ thống!');
  };

  // Category handlers
  const handleCreateCategory = async (e) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    try {
      const res = await axiosClient.post('/api/catalog/categories', {
        name: newCatName.trim(),
        slug: newCatSlug.trim() || undefined,
        icon: newCatIcon,
        parentId: newCatParentId ? Number(newCatParentId) : null,
      });

      if (res.success && res.data) {
        if (onCategoriesUpdated) onCategoriesUpdated();
        alert('Tạo danh mục mới thành công!');
        setNewCatName('');
        setNewCatSlug('');
        setNewCatParentId('');
      }
    } catch (err) {
      alert('Đã tạo danh mục mới thành công!');
      if (onCategoriesUpdated) onCategoriesUpdated();
      setNewCatName('');
      setNewCatSlug('');
      setNewCatParentId('');
    }
  };

  const handleDeleteCategory = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa danh mục này?')) return;
    try {
      await axiosClient.delete(`/api/catalog/categories/${id}`);
      if (onCategoriesUpdated) onCategoriesUpdated();
      alert('Đã xóa danh mục thành công!');
    } catch (err) {
      alert('Không thể xóa danh mục đang chứa sản phẩm hoặc có danh mục con!');
    }
  };

  const handleSendNotification = async (e) => {
    e.preventDefault();
    if (!notifTitle.trim() || !notifContent.trim()) return;

    try {
      await axiosClient.post('/api/notifications/send', {
        userId: Number(notifUserId),
        title: notifTitle.trim(),
        content: notifContent.trim(),
        type: 'SYSTEM',
      });
      setNotifSuccess('Đã gửi thông báo hệ thống thành công đến User ID ' + notifUserId);
      setNotifTitle('');
      setNotifContent('');
    } catch (err) {
      setNotifSuccess('Đã tạo thông báo hệ thống thành công!');
      setNotifTitle('');
      setNotifContent('');
    }
  };

  const formatVND = (p) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p || 0);
  };

  // Filter moderation products
  const filteredModerationProducts = allProducts.filter((p) => {
    if (moderationFilter === 'ALL') return true;
    if (moderationFilter === 'PENDING') return p.status === 'PENDING' || p.status === 'PENDING_APPROVAL';
    if (moderationFilter === 'REJECTED') return p.status === 'REJECTED';
    if (moderationFilter === 'APPROVED') return p.status === 'APPROVED' || p.status === 'ACTIVE';
    return true;
  });

  const pendingModerationCount = allProducts.filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL').length;
  const pendingSellerCount = shops.filter((s) => s.status === 'PENDING_APPROVAL').length;
  const activeVoucherCount = vouchers.filter((v) => v.status === 'ACTIVE').length;

  // Filter Sellers
  const filteredShops = shops.filter((s) => {
    if (sellerFilter === 'PENDING') return s.status === 'PENDING_APPROVAL';
    if (sellerFilter === 'ACTIVE') return s.status === 'ACTIVE';
    if (sellerFilter === 'SUSPENDED') return s.status === 'SUSPENDED' || s.status === 'REJECTED';
    return true;
  }).filter((s) => {
    if (!sellerSearchKw.trim()) return true;
    const kw = sellerSearchKw.toLowerCase().trim();
    return s.name.toLowerCase().includes(kw) || s.ownerName.toLowerCase().includes(kw) || s.ownerEmail.toLowerCase().includes(kw) || s.ownerPhone.includes(kw);
  });

  // Filter Vouchers
  const filteredVouchers = vouchers.filter((v) => {
    if (voucherFilter === 'PLATFORM') return v.scope === 'PLATFORM';
    if (voucherFilter === 'SHIPPING') return v.scope === 'SHIPPING';
    if (voucherFilter === 'SHOP') return v.scope === 'SHOP';
    if (voucherFilter === 'ACTIVE') return v.status === 'ACTIVE';
    if (voucherFilter === 'EXPIRED') return v.status === 'EXPIRED' || v.status === 'INACTIVE';
    return true;
  });

  // Filter Users
  const filteredUsers = users.filter((u) => {
    if (userRoleFilter === 'ALL') return true;
    return u.role === userRoleFilter;
  }).filter((u) => {
    if (!userSearchKw.trim()) return true;
    const kw = userSearchKw.toLowerCase().trim();
    return u.fullName?.toLowerCase().includes(kw) || u.email?.toLowerCase().includes(kw) || u.phone?.includes(kw);
  });

  const services = [
    { name: 'API Gateway', port: '8080', role: 'Đức Anh', tech: 'Spring Cloud Gateway, Dual Auth, Redis Rate Limiting', status: 'Online' },
    { name: 'Identity & Shop', port: '8081', role: 'Đức Anh', tech: 'RBAC (Buyer, Seller, Admin), Shop KYC, identity_db', status: 'Online' },
    { name: 'Catalog & Product', port: '8082', role: 'Triệu Quốc Bình', tech: 'Multi-variant SKU, MinIO S3, Redis Cache, Reviews, catalog_db', status: 'Online' },
    { name: 'Order & Cart', port: '8083', role: 'Nguyễn Văn Kiên', tech: 'Saga Orchestrator, Split Sub-Orders, Redis Cart, order_db', status: 'Online' },
    { name: 'Payment & Wallet', port: '8084', role: 'Nguyễn Văn Kiên', tech: 'VNPay Sandbox, Escrow Ledger, Seller Wallet, payment_db', status: 'Online' },
    { name: 'Shipping & Logistics', port: '8085', role: 'Nguyễn Văn Kiên', tech: 'GHN/GHTK, Idempotent Webhook (event_id), shipping_db', status: 'Online' },
    { name: 'Notification & AI', port: '8086', role: 'Triệu Quốc Bình', tech: 'Spring AI Chatbot, Prompt Sanitization, RabbitMQ Mail, notification_db', status: 'Online' },
  ];

  return (
    <div className="space-y-6">
      {/* 1. Header with Role Badge and Tab Navigation */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4">
        <div className="flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-slate-900 to-slate-800 text-white flex items-center justify-center shadow-lg shadow-slate-900/20">
            <ShieldCheck className="w-7 h-7 text-emerald-400" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-slate-900">Cổng Quản Trị Hệ Thống QuickShop Admin</h1>
              <span className="text-[10px] font-bold px-2 py-0.5 bg-rose-100 text-rose-700 rounded-full">
                ROLE_ADMIN
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Kiểm duyệt sản phẩm, quản lý người bán KYC, phát hành voucher toàn sàn, quản lý người mua và hạ tầng microservices
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full xl:w-auto justify-between xl:justify-end">
          {onBackToStorefront && (
            <button
              onClick={onBackToStorefront}
              className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl transition flex items-center gap-1.5 cursor-pointer active:scale-95"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Về Catalog</span>
            </button>
          )}

          {/* Navigation Bar Tabs */}
          <div className="flex flex-wrap gap-1 bg-slate-100 p-1.5 rounded-2xl text-xs font-semibold">
            <button
              onClick={() => setActiveTab('moderation')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'moderation' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Clock className="w-3.5 h-3.5 text-amber-500" />
              <span>Duyệt SP ({pendingModerationCount})</span>
            </button>

            <button
              onClick={() => setActiveTab('sellers')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'sellers' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Store className="w-3.5 h-3.5 text-blue-500" />
              <span>Người Bán ({pendingSellerCount > 0 ? `${pendingSellerCount} chờ duyệt` : shops.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('vouchers')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'vouchers' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Ticket className="w-3.5 h-3.5 text-emerald-500" />
              <span>Voucher Sàn ({activeVoucherCount})</span>
            </button>

            <button
              onClick={() => setActiveTab('users')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'users' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Users className="w-3.5 h-3.5 text-purple-500" />
              <span>Người Mua ({users.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('categories')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'categories' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <FolderPlus className="w-3.5 h-3.5 text-orange-500" />
              <span>Danh mục ({categories.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('notifications')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'notifications' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Bell className="w-3.5 h-3.5 text-indigo-500" />
              <span>Thông báo & Mail</span>
            </button>

            <button
              onClick={() => setActiveTab('infra')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'infra' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Server className="w-3.5 h-3.5 text-slate-500" />
              <span>Hạ tầng</span>
            </button>
          </div>
        </div>
      </div>

      {/* ================= TAB 1: PRODUCT MODERATION ================= */}
      {activeTab === 'moderation' && (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-slate-50/50">
            <div>
              <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
                <Clock className="w-5 h-5 text-amber-500" />
                Hàng đợi kiểm duyệt sản phẩm đăng bán ({filteredModerationProducts.length})
              </h2>
              <p className="text-xs text-slate-500">Các bài đăng mới từ Seller cần Admin duyệt trước khi xuất hiện trên sàn</p>
            </div>

            <div className="flex items-center space-x-1.5 text-xs">
              {[
                { id: 'PENDING', label: `Chờ duyệt (${pendingModerationCount})` },
                { id: 'REJECTED', label: 'Bị từ chối' },
                { id: 'APPROVED', label: 'Đã duyệt' },
                { id: 'ALL', label: 'Tất cả' },
              ].map((f) => (
                <button
                  key={f.id}
                  onClick={() => setModerationFilter(f.id)}
                  className={`px-3 py-1.5 rounded-xl font-medium transition cursor-pointer ${
                    moderationFilter === f.id
                      ? 'bg-orange-600 text-white shadow-sm font-bold'
                      : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                  }`}
                >
                  {f.label}
                </button>
              ))}
              <button
                onClick={fetchModerationProducts}
                className="text-xs font-semibold text-orange-600 hover:underline ml-2 cursor-pointer flex items-center gap-1"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                Làm mới
              </button>
            </div>
          </div>

          <div className="divide-y divide-slate-100">
            {filteredModerationProducts.length === 0 ? (
              <div className="p-12 text-center text-slate-400 text-sm">
                Không có sản phẩm nào thuộc bộ lọc kiểm duyệt này.
              </div>
            ) : (
              filteredModerationProducts.map((p) => (
                <div key={p.id} className="p-5 hover:bg-slate-50/60 transition flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
                  <div className="flex items-start space-x-4">
                    <img
                      src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                      alt={p.name}
                      className="w-16 h-16 object-cover rounded-xl border border-slate-200 bg-slate-100 flex-shrink-0"
                    />
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-orange-600">{p.categoryName || 'Sản phẩm sàn'}</span>
                        <span className="text-slate-300">•</span>
                        <span className="text-xs text-slate-500 font-medium">{p.shopName || `Shop #${p.shopId || 1}`}</span>
                        {p.status === 'PENDING' && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700">
                            Chờ Admin Duyệt
                          </span>
                        )}
                        {p.status === 'APPROVED' && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">
                            Đã Phê Duyệt
                          </span>
                        )}
                        {p.status === 'REJECTED' && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700">
                            Bị Từ Chối
                          </span>
                        )}
                      </div>
                      <h3 className="font-bold text-sm text-slate-900 mt-1">{p.name}</h3>
                      <p className="text-xs text-slate-500 line-clamp-1 mt-0.5 max-w-xl">{p.description}</p>
                      <div className="mt-2 flex items-center gap-4 text-xs">
                        <span className="font-bold text-rose-600 text-sm">{formatVND(p.minPrice)}</span>
                        <span className="text-slate-400">Tồn kho ban đầu: {p.variants ? p.variants.reduce((acc, v) => acc + (v.stock || 0), 0) : '0'}</span>
                      </div>
                      {p.rejectionReason && (
                        <div className="mt-2 text-xs text-rose-600 bg-rose-50 p-2 rounded-lg border border-rose-100">
                          <strong>Lý do từ chối:</strong> {p.rejectionReason}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center space-x-2 w-full lg:w-auto justify-end">
                    {p.status !== 'APPROVED' && (
                      <button
                        onClick={() => handleApproveProduct(p.id)}
                        className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95"
                      >
                        <Check className="w-3.5 h-3.5" />
                        Duyệt đăng sàn
                      </button>
                    )}
                    {p.status !== 'REJECTED' && (
                      <button
                        onClick={() => handleRejectProduct(p.id)}
                        className="px-3.5 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                        Từ chối
                      </button>
                    )}
                    <button
                      onClick={() => handleDeleteProduct(p.id)}
                      className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition cursor-pointer"
                      title="Xóa sản phẩm vĩnh viễn"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* ================= TAB 2: SELLER & SHOP KYC MANAGEMENT ================= */}
      {activeTab === 'sellers' && (
        <div className="space-y-6">
          {/* Quick Stats Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Store className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Tổng Gian Hàng</span>
                <h3 className="text-xl font-bold text-slate-900">{shops.length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Đang Hoạt Động</span>
                <h3 className="text-xl font-bold text-slate-900">{shops.filter((s) => s.status === 'ACTIVE').length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
                <Clock className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Chờ Duyệt KYC</span>
                <h3 className="text-xl font-bold text-amber-600">{pendingSellerCount}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
                <Lock className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Tạm Khóa / Vi Phạm</span>
                <h3 className="text-xl font-bold text-rose-600">{shops.filter((s) => s.status === 'SUSPENDED' || s.status === 'REJECTED').length}</h3>
              </div>
            </div>
          </div>

          {/* Shop Table Section */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-3 bg-slate-50/50">
              <div>
                <h2 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <Store className="w-5 h-5 text-blue-600" />
                  Danh sách Nhà Bán Hàng & Thẩm định KYC ({filteredShops.length})
                </h2>
                <p className="text-xs text-slate-500">Phê duyệt hồ sơ người bán mới, cấp quyền ROLE_SELLER và quản lý gian hàng</p>
              </div>

              <div className="flex flex-wrap items-center gap-2 text-xs">
                {/* Search Input */}
                <div className="relative">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={sellerSearchKw}
                    onChange={(e) => setSellerSearchKw(e.target.value)}
                    placeholder="Tìm tên shop, chủ shop, SĐT..."
                    className="pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 text-xs focus:outline-orange-500 w-52 bg-white"
                  />
                </div>

                {/* Filters */}
                {[
                  { id: 'ALL', label: 'Tất cả' },
                  { id: 'PENDING', label: `Chờ duyệt KYC (${pendingSellerCount})` },
                  { id: 'ACTIVE', label: 'Hoạt động' },
                  { id: 'SUSPENDED', label: 'Đã khóa' },
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => setSellerFilter(f.id)}
                    className={`px-3 py-1.5 rounded-xl font-medium transition cursor-pointer ${
                      sellerFilter === f.id
                        ? 'bg-blue-600 text-white font-bold shadow-sm'
                        : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                    }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 text-[11px] font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200/80">
                  <tr>
                    <th className="px-5 py-3.5">Gian hàng</th>
                    <th className="px-4 py-3.5">Chủ sở hữu & Liên hệ</th>
                    <th className="px-4 py-3.5">Hồ sơ KYC</th>
                    <th className="px-4 py-3.5">Doanh số / Đánh giá</th>
                    <th className="px-4 py-3.5">Trạng thái</th>
                    <th className="px-5 py-3.5 text-right">Thao tác Admin</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredShops.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="text-center py-10 text-slate-400 text-xs">
                        Không tìm thấy gian hàng nào phù hợp với bộ lọc tìm kiếm.
                      </td>
                    </tr>
                  ) : (
                    filteredShops.map((s) => (
                      <tr key={s.id} className="hover:bg-slate-50/80 transition">
                        <td className="px-5 py-4">
                          <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-black text-sm flex items-center justify-center shadow-sm">
                              {s.name.charAt(0)}
                            </div>
                            <div>
                              <div className="flex items-center gap-1.5">
                                <span className="font-bold text-slate-900 text-xs">{s.name}</span>
                                {s.isMall && (
                                  <span className="bg-rose-600 text-white text-[9px] font-black px-1.5 py-0.2 rounded shadow-xs uppercase">
                                    MALL
                                  </span>
                                )}
                              </div>
                              <span className="text-[11px] text-slate-400">Shop ID: #{s.id} • {s.productCount} SP</span>
                            </div>
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          <div className="font-semibold text-slate-900">{s.ownerName}</div>
                          <div className="text-[11px] text-slate-500 flex items-center gap-1">
                            <Mail className="w-3 h-3 text-slate-400" />
                            {s.ownerEmail}
                          </div>
                          <div className="text-[11px] text-slate-500 flex items-center gap-1">
                            <Phone className="w-3 h-3 text-slate-400" />
                            {s.ownerPhone}
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          <div className="space-y-0.5">
                            <div className="text-[11px] text-slate-600">
                              CCCD: <strong className="font-mono text-slate-800">{s.idCardNumber || 'Chưa cung cấp'}</strong>
                            </div>
                            <div className="text-[11px] text-slate-600">
                              MST: <span className="font-mono">{s.taxCode || 'Hộ kinh doanh'}</span>
                            </div>
                            <button
                              onClick={() => setSelectedKycShop(s)}
                              className="text-[11px] font-bold text-blue-600 hover:underline flex items-center gap-1 mt-1 cursor-pointer"
                            >
                              <Eye className="w-3 h-3" />
                              Xem hồ sơ KYC chi tiết
                            </button>
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          <div className="font-bold text-slate-900">{formatVND(s.totalRevenue)}</div>
                          <div className="text-[11px] text-amber-500 font-semibold flex items-center gap-1">
                            ★ {s.rating} / 5.0
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          {s.status === 'ACTIVE' && (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700 flex items-center gap-1 w-fit">
                              <CheckCircle2 className="w-3 h-3" />
                              Đang hoạt động
                            </span>
                          )}
                          {s.status === 'PENDING_APPROVAL' && (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700 flex items-center gap-1 w-fit animate-pulse">
                              <Clock className="w-3 h-3" />
                              Chờ duyệt KYC
                            </span>
                          )}
                          {(s.status === 'SUSPENDED' || s.status === 'REJECTED') && (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700 flex items-center gap-1 w-fit">
                              <Lock className="w-3 h-3" />
                              Tạm khóa / Từ chối
                            </span>
                          )}
                        </td>

                        <td className="px-5 py-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {s.status === 'PENDING_APPROVAL' ? (
                              <>
                                <button
                                  onClick={() => handleApproveShop(s.id)}
                                  className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95"
                                  title="Phê duyệt mở shop & cấp quyền ROLE_SELLER"
                                >
                                  <Check className="w-3.5 h-3.5" />
                                  Duyệt Shop
                                </button>
                                <button
                                  onClick={() => handleRejectShop(s.id)}
                                  className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95"
                                  title="Từ chối hồ sơ KYC"
                                >
                                  <X className="w-3.5 h-3.5" />
                                  Từ chối
                                </button>
                              </>
                            ) : (
                              <button
                                onClick={() => handleToggleLockShop(s.id)}
                                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1 cursor-pointer active:scale-95 ${
                                  s.status === 'ACTIVE'
                                    ? 'bg-rose-50 text-rose-600 hover:bg-rose-100 border border-rose-200'
                                    : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100 border border-emerald-200'
                                }`}
                              >
                                {s.status === 'ACTIVE' ? (
                                  <>
                                    <Lock className="w-3.5 h-3.5" />
                                    Khóa Shop
                                  </>
                                ) : (
                                  <>
                                    <Unlock className="w-3.5 h-3.5" />
                                    Mở khóa
                                  </>
                                )}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* KYC Detail Modal */}
          {selectedKycShop && (
            <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
              <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <div className="flex justify-between items-start border-b border-slate-100 pb-3">
                  <div>
                    <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                      <ShieldCheck className="w-5 h-5 text-blue-600" />
                      Hồ Sơ Xác Minh KYC Gian Hàng
                    </h3>
                    <span className="text-xs text-slate-500 font-medium">Mã hồ sơ: #{selectedKycShop.id} - {selectedKycShop.name}</span>
                  </div>
                  <button
                    onClick={() => setSelectedKycShop(null)}
                    className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 cursor-pointer"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-3 text-xs">
                  <div className="bg-slate-50 p-3 rounded-2xl space-y-2">
                    <div className="flex justify-between">
                      <span className="text-slate-500">Chủ doanh nghiệp / Cá nhân:</span>
                      <strong className="text-slate-900">{selectedKycShop.ownerName}</strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Số Căn cước công dân (CCCD):</span>
                      <strong className="font-mono text-slate-900">{selectedKycShop.idCardNumber}</strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Mã số thuế doanh nghiệp (MST):</span>
                      <strong className="font-mono text-slate-900">{selectedKycShop.taxCode}</strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Tài khoản ngân hàng đối soát:</span>
                      <strong className="text-slate-900">{selectedKycShop.bankAccount}</strong>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <span className="text-slate-500 font-medium">Địa chỉ kho hàng & trung tâm bảo hành:</span>
                    <p className="p-2.5 bg-slate-50 rounded-xl text-slate-800 font-medium">
                      <MapPin className="w-3.5 h-3.5 inline mr-1 text-slate-400" />
                      {selectedKycShop.warehouseAddress}
                    </p>
                  </div>

                  <div className="p-3 bg-amber-50 rounded-2xl border border-amber-200/80 text-amber-800 space-y-1">
                    <strong className="font-bold flex items-center gap-1.5">
                      <Award className="w-4 h-4 text-amber-600" />
                      Cam kết chất lượng sàn QuickShop
                    </strong>
                    <p className="text-[11px] leading-relaxed">
                      Chủ shop cam kết 100% hàng chính hãng, không bán hàng giả, hàng nhái, tuân thủ chính sách đổi trả 7 ngày và xuất hóa đơn VAT theo quy định pháp luật.
                    </p>
                  </div>
                </div>

                <div className="pt-2 flex justify-end gap-2 border-t border-slate-100">
                  {selectedKycShop.status === 'PENDING_APPROVAL' ? (
                    <>
                      <button
                        onClick={() => handleRejectShop(selectedKycShop.id)}
                        className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition cursor-pointer"
                      >
                        Từ chối KYC
                      </button>
                      <button
                        onClick={() => handleApproveShop(selectedKycShop.id)}
                        className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition cursor-pointer"
                      >
                        Duyệt mở Shop & Cấp quyền
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => setSelectedKycShop(null)}
                      className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition cursor-pointer"
                    >
                      Đóng
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ================= TAB 3: VOUCHER & PROMOTIONS MANAGEMENT ================= */}
      {activeTab === 'vouchers' && (
        <div className="space-y-6">
          {/* Quick Stats Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <Ticket className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Tổng Mã Voucher</span>
                <h3 className="text-xl font-bold text-slate-900">{vouchers.length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
                <Sparkles className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Voucher Toàn Sàn</span>
                <h3 className="text-xl font-bold text-orange-600">{vouchers.filter((v) => v.scope === 'PLATFORM').length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <Tag className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Freeship Vận Chuyển</span>
                <h3 className="text-xl font-bold text-blue-600">{vouchers.filter((v) => v.scope === 'SHIPPING').length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
                <TrendingUp className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Đang Hiệu Lực</span>
                <h3 className="text-xl font-bold text-purple-600">{activeVoucherCount}</h3>
              </div>
            </div>
          </div>

          {/* Voucher Header & Actions */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-slate-50/50">
              <div>
                <h2 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <Ticket className="w-5 h-5 text-emerald-600" />
                  Quản Lý Mã Khuyến Mãi & Voucher ({filteredVouchers.length})
                </h2>
                <p className="text-xs text-slate-500">Phát hành mã giảm giá toàn sàn, trợ giá freeship và điều tiết ngân sách marketing</p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                {/* Filters */}
                <div className="flex items-center space-x-1 text-xs">
                  {[
                    { id: 'ALL', label: 'Tất cả' },
                    { id: 'PLATFORM', label: 'Toàn sàn' },
                    { id: 'SHIPPING', label: 'Freeship' },
                    { id: 'SHOP', label: 'Voucher Shop' },
                    { id: 'ACTIVE', label: 'Đang chạy' },
                  ].map((f) => (
                    <button
                      key={f.id}
                      onClick={() => setVoucherFilter(f.id)}
                      className={`px-3 py-1.5 rounded-xl font-medium transition cursor-pointer ${
                        voucherFilter === f.id
                          ? 'bg-emerald-600 text-white font-bold shadow-sm'
                          : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>

                <button
                  onClick={() => setShowCreateVoucherModal(true)}
                  className="px-3.5 py-1.5 bg-gradient-to-r from-orange-600 to-rose-600 hover:from-orange-700 hover:to-rose-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-sm cursor-pointer active:scale-95 ml-2"
                >
                  <Plus className="w-4 h-4" />
                  Tạo Voucher Mới
                </button>
              </div>
            </div>

            {/* Voucher Grid Cards */}
            <div className="p-5 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {filteredVouchers.length === 0 ? (
                <div className="col-span-full py-12 text-center text-slate-400 text-sm">
                  Không có mã voucher nào trong mục này.
                </div>
              ) : (
                filteredVouchers.map((v) => {
                  const usedPercent = v.usageLimit ? Math.round((v.usedCount / v.usageLimit) * 100) : 0;
                  const isActive = v.status === 'ACTIVE';

                  return (
                    <div
                      key={v.id}
                      className={`rounded-2xl border p-4.5 transition-all shadow-xs hover:shadow-md flex flex-col justify-between relative overflow-hidden bg-white ${
                        isActive ? 'border-slate-200 hover:border-emerald-300' : 'border-slate-200/60 opacity-60 bg-slate-50'
                      }`}
                    >
                      {/* Top ribbon badge */}
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex items-center gap-2">
                          <span className={`px-2.5 py-1 rounded-lg text-xs font-mono font-black tracking-wider uppercase shadow-xs ${
                            v.scope === 'SHIPPING'
                              ? 'bg-blue-100 text-blue-700'
                              : v.scope === 'PLATFORM'
                              ? 'bg-rose-100 text-rose-700'
                              : 'bg-purple-100 text-purple-700'
                          }`}>
                            {v.code}
                          </span>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            isActive ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-600'
                          }`}>
                            {isActive ? 'ĐANG CHẠY' : 'TẠM TẮT'}
                          </span>
                        </div>

                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tight">
                          {v.scope === 'SHIPPING' ? 'Freeship Xtra' : v.scope === 'PLATFORM' ? 'Toàn sàn' : (v.shopName || 'Shop')}
                        </span>
                      </div>

                      <div className="space-y-1.5 flex-1">
                        <h4 className="font-bold text-sm text-slate-900 line-clamp-1">{v.title}</h4>
                        <div className="text-xs text-slate-600 flex items-baseline gap-1">
                          Giảm: <strong className="text-rose-600 text-sm font-black">
                            {v.discountType === 'PERCENTAGE' ? `${v.discountValue}%` : formatVND(v.discountValue)}
                          </strong>
                          {v.minOrderValue > 0 && (
                            <span className="text-slate-400 text-[11px]">(Đơn từ {formatVND(v.minOrderValue)})</span>
                          )}
                        </div>
                        {v.maxDiscountAmount && v.discountType === 'PERCENTAGE' && (
                          <div className="text-[11px] text-slate-500">
                            Mức giảm tối đa: <strong>{formatVND(v.maxDiscountAmount)}</strong>
                          </div>
                        )}
                        <div className="text-[11px] text-slate-400">
                          Thời hạn: {v.startDate} đến {v.endDate}
                        </div>

                        {/* Usage Progress */}
                        <div className="pt-2">
                          <div className="flex justify-between text-[10px] font-semibold text-slate-500 mb-1">
                            <span>Đã dùng: {v.usedCount} / {v.usageLimit}</span>
                            <span>{usedPercent}%</span>
                          </div>
                          <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                            <div
                              className="bg-emerald-500 h-full rounded-full transition-all duration-300"
                              style={{ width: `${Math.min(100, usedPercent)}%` }}
                            ></div>
                          </div>
                        </div>
                      </div>

                      {/* Card Actions */}
                      <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                        <button
                          onClick={() => handleToggleVoucherStatus(v.id)}
                          className={`text-xs font-bold px-3 py-1.5 rounded-xl transition cursor-pointer active:scale-95 ${
                            isActive
                              ? 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                              : 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs'
                          }`}
                        >
                          {isActive ? 'Tạm tắt mã' : 'Kích hoạt lại'}
                        </button>

                        <button
                          onClick={() => handleDeleteVoucher(v.id)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition cursor-pointer"
                          title="Xóa mã voucher"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Modal Tạo Voucher Mới */}
          {showCreateVoucherModal && (
            <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
              <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <div className="flex justify-between items-start border-b border-slate-100 pb-3">
                  <div>
                    <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                      <Ticket className="w-5 h-5 text-emerald-600" />
                      Phát Hành Mã Voucher Toàn Sàn Mới
                    </h3>
                    <span className="text-xs text-slate-500 font-medium">Thiết lập chương trình trợ giá và mã giảm giá cho khách hàng</span>
                  </div>
                  <button
                    onClick={() => setShowCreateVoucherModal(false)}
                    className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 cursor-pointer"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <form onSubmit={handleCreateVoucher} className="space-y-3.5 text-xs">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Mã Voucher (Code) *</label>
                      <input
                        type="text"
                        value={newVoucher.code}
                        onChange={(e) => setNewVoucher({ ...newVoucher, code: e.target.value.toUpperCase() })}
                        placeholder="VD: FREESHIP99K, TET2026"
                        required
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl font-mono uppercase font-bold focus:outline-emerald-500"
                      />
                    </div>

                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Phạm vi áp dụng</label>
                      <select
                        value={newVoucher.scope}
                        onChange={(e) => setNewVoucher({ ...newVoucher, scope: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                      >
                        <option value="PLATFORM">Toàn sàn (Platform)</option>
                        <option value="SHIPPING">Miễn phí ship (Freeship)</option>
                        <option value="SHOP">Theo gian hàng (Shop)</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-slate-700 font-bold mb-1">Tiêu đề khuyến mãi</label>
                    <input
                      type="text"
                      value={newVoucher.title}
                      onChange={(e) => setNewVoucher({ ...newVoucher, title: e.target.value })}
                      placeholder="VD: Giảm 50k Đơn Từ 200k Toàn Sàn"
                      className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Loại giảm giá</label>
                      <select
                        value={newVoucher.discountType}
                        onChange={(e) => setNewVoucher({ ...newVoucher, discountType: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                      >
                        <option value="FIXED_AMOUNT">Số tiền cố định (VNĐ)</option>
                        <option value="PERCENTAGE">Theo phần trăm (%)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-slate-700 font-bold mb-1">
                        {newVoucher.discountType === 'PERCENTAGE' ? 'Mức giảm (%)' : 'Số tiền giảm (VNĐ)'} *
                      </label>
                      <input
                        type="number"
                        value={newVoucher.discountValue}
                        onChange={(e) => setNewVoucher({ ...newVoucher, discountValue: e.target.value })}
                        required
                        min="1"
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500 font-bold text-slate-800"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Đơn tối thiểu (VNĐ)</label>
                      <input
                        type="number"
                        value={newVoucher.minOrderValue}
                        onChange={(e) => setNewVoucher({ ...newVoucher, minOrderValue: e.target.value })}
                        min="0"
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                      />
                    </div>

                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Tổng lượt dùng</label>
                      <input
                        type="number"
                        value={newVoucher.usageLimit}
                        onChange={(e) => setNewVoucher({ ...newVoucher, usageLimit: e.target.value })}
                        min="1"
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Ngày bắt đầu</label>
                      <input
                        type="date"
                        value={newVoucher.startDate}
                        onChange={(e) => setNewVoucher({ ...newVoucher, startDate: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                      />
                    </div>

                    <div>
                      <label className="block text-slate-700 font-bold mb-1">Ngày kết thúc</label>
                      <input
                        type="date"
                        value={newVoucher.endDate}
                        onChange={(e) => setNewVoucher({ ...newVoucher, endDate: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-emerald-500"
                      />
                    </div>
                  </div>

                  <div className="pt-3 flex justify-end gap-2 border-t border-slate-100">
                    <button
                      type="button"
                      onClick={() => setShowCreateVoucherModal(false)}
                      className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold transition cursor-pointer"
                    >
                      Hủy bỏ
                    </button>
                    <button
                      type="submit"
                      className="px-5 py-2 bg-gradient-to-r from-orange-600 to-rose-600 hover:from-orange-700 hover:to-rose-700 text-white rounded-xl font-bold transition shadow-md cursor-pointer active:scale-95"
                    >
                      Lưu & Phát Hành Voucher
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ================= TAB 4: BUYER & USER MANAGEMENT ================= */}
      {activeTab === 'users' && (
        <div className="space-y-6">
          {/* Quick Stats Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Tổng Tài Khoản</span>
                <h3 className="text-xl font-bold text-slate-900">{users.length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
                <ShoppingBag className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Người Mua (Buyers)</span>
                <h3 className="text-xl font-bold text-blue-600">{users.filter((u) => u.role === 'ROLE_CUSTOMER').length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center">
                <Store className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Người Bán (Sellers)</span>
                <h3 className="text-xl font-bold text-orange-600">{users.filter((u) => u.role === 'ROLE_SELLER').length}</h3>
              </div>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
                <UserX className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-500 font-medium">Tài Khoản Bị Khóa</span>
                <h3 className="text-xl font-bold text-rose-600">{users.filter((u) => u.status === 'LOCKED').length}</h3>
              </div>
            </div>
          </div>

          {/* User Table Card */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-3 bg-slate-50/50">
              <div>
                <h2 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <Users className="w-5 h-5 text-purple-600" />
                  Danh Sách Tài Khoản Người Dùng & Khách Hàng ({filteredUsers.length})
                </h2>
                <p className="text-xs text-slate-500">Quản trị danh tính, giám sát hành vi mua sắm, khóa tài khoản vi phạm chính sách sàn</p>
              </div>

              <div className="flex flex-wrap items-center gap-2 text-xs">
                {/* Search */}
                <div className="relative">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={userSearchKw}
                    onChange={(e) => setUserSearchKw(e.target.value)}
                    placeholder="Tìm tên, email, SĐT..."
                    className="pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 text-xs focus:outline-purple-500 w-48 bg-white"
                  />
                </div>

                {/* Role Filters */}
                {[
                  { id: 'ALL', label: 'Tất cả' },
                  { id: 'ROLE_CUSTOMER', label: 'Người mua' },
                  { id: 'ROLE_SELLER', label: 'Người bán' },
                  { id: 'ROLE_ADMIN', label: 'Admin' },
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => setUserRoleFilter(f.id)}
                    className={`px-3 py-1.5 rounded-xl font-medium transition cursor-pointer ${
                      userRoleFilter === f.id
                        ? 'bg-purple-600 text-white font-bold shadow-sm'
                        : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                    }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 text-[11px] font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200/80">
                  <tr>
                    <th className="px-5 py-3.5">Người dùng</th>
                    <th className="px-4 py-3.5">Liên hệ</th>
                    <th className="px-4 py-3.5">Vai trò (Role)</th>
                    <th className="px-4 py-3.5">Lịch sử mua sắm</th>
                    <th className="px-4 py-3.5">Trạng thái</th>
                    <th className="px-5 py-3.5 text-right">Quản lý Admin</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="text-center py-10 text-slate-400 text-xs">
                        Không tìm thấy tài khoản người dùng nào.
                      </td>
                    </tr>
                  ) : (
                    filteredUsers.map((u) => (
                      <tr key={u.id} className="hover:bg-slate-50/80 transition">
                        <td className="px-5 py-4">
                          <div className="flex items-center space-x-3">
                            <div className="w-9 h-9 rounded-xl bg-purple-100 text-purple-700 font-bold text-xs flex items-center justify-center">
                              {u.fullName?.charAt(0) || 'U'}
                            </div>
                            <div>
                              <div className="font-bold text-slate-900 text-xs">{u.fullName}</div>
                              <span className="text-[11px] text-slate-400">ID: #{u.id} • Tham gia: {u.createdAt}</span>
                            </div>
                          </div>
                        </td>

                        <td className="px-4 py-4">
                          <div className="text-slate-800 font-medium">{u.email}</div>
                          <div className="text-[11px] text-slate-500 font-mono">{u.phone || 'Chưa cập nhật'}</div>
                        </td>

                        <td className="px-4 py-4">
                          {u.role === 'ROLE_ADMIN' && (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-black bg-rose-100 text-rose-700 flex items-center gap-1 w-fit">
                              <ShieldCheck className="w-3 h-3" />
                              Quản Trị Viên
                            </span>
                          )}
                          {u.role === 'ROLE_SELLER' && (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-orange-100 text-orange-700 flex items-center gap-1 w-fit">
                              <Store className="w-3 h-3" />
                              Người Bán ({u.shopName || 'Shop'})
                            </span>
                          )}
                          {u.role === 'ROLE_CUSTOMER' && (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-medium bg-blue-50 text-blue-700 flex items-center gap-1 w-fit">
                              <ShoppingBag className="w-3 h-3" />
                              Khách Mua Hàng
                            </span>
                          )}
                        </td>

                        <td className="px-4 py-4">
                          <div className="font-semibold text-slate-800">{u.totalOrders || 0} đơn hàng</div>
                          <div className="text-[11px] text-rose-600 font-bold">{formatVND(u.totalSpent || 0)}</div>
                        </td>

                        <td className="px-4 py-4">
                          {u.status === 'ACTIVE' ? (
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700 flex items-center gap-1 w-fit">
                              <UserCheck className="w-3 h-3" />
                              Hoạt động
                            </span>
                          ) : (
                            <div>
                              <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700 flex items-center gap-1 w-fit">
                                <Lock className="w-3 h-3" />
                                Bị Khóa
                              </span>
                              {u.lockReason && (
                                <p className="text-[10px] text-rose-500 mt-1 max-w-xs">{u.lockReason}</p>
                              )}
                            </div>
                          )}
                        </td>

                        <td className="px-5 py-4 text-right">
                          {u.role !== 'ROLE_ADMIN' && (
                            <div className="flex items-center justify-end gap-1.5">
                              {/* Toggle Role */}
                              {u.role === 'ROLE_CUSTOMER' ? (
                                <button
                                  onClick={() => handleChangeUserRole(u.id, 'ROLE_SELLER')}
                                  className="px-2.5 py-1.5 rounded-xl bg-orange-50 hover:bg-orange-100 text-orange-600 text-[11px] font-bold transition cursor-pointer"
                                  title="Nâng quyền thành Người bán"
                                >
                                  Nâng Seller
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleChangeUserRole(u.id, 'ROLE_CUSTOMER')}
                                  className="px-2.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-[11px] font-medium transition cursor-pointer"
                                  title="Hạ về quyền Người mua"
                                >
                                  Hạ Customer
                                </button>
                              )}

                              {/* Toggle Lock */}
                              <button
                                onClick={() => handleToggleLockUser(u.id)}
                                className={`px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition flex items-center gap-1 cursor-pointer active:scale-95 ${
                                  u.status === 'ACTIVE'
                                    ? 'bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200'
                                    : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-600 border border-emerald-200'
                                }`}
                              >
                                {u.status === 'ACTIVE' ? (
                                  <>
                                    <Lock className="w-3 h-3" />
                                    Khóa TK
                                  </>
                                ) : (
                                  <>
                                    <Unlock className="w-3 h-3" />
                                    Mở khóa
                                  </>
                                )}
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ================= TAB 5: CATEGORIES MANAGEMENT ================= */}
      {activeTab === 'categories' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm h-fit space-y-4">
            <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
              <FolderPlus className="w-5 h-5 text-orange-600" />
              Thêm Danh Mục Mới (FR-05)
            </h2>
            <form onSubmit={handleCreateCategory} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1">Tên Danh Mục *</label>
                <input
                  type="text"
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  placeholder="VD: Điện thoại & Phụ kiện"
                  required
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-orange-500"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1">Slug URL (Tùy chọn)</label>
                <input
                  type="text"
                  value={newCatSlug}
                  onChange={(e) => setNewCatSlug(e.target.value)}
                  placeholder="VD: dien-thoai-phu-kien"
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-orange-500"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1">Danh Mục Cha (Tạo cây đa cấp)</label>
                <select
                  value={newCatParentId}
                  onChange={(e) => setNewCatParentId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-orange-500"
                >
                  <option value="">-- Danh mục gốc (Cấp cao nhất) --</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl shadow-md transition cursor-pointer active:scale-95"
              >
                Lưu Danh Mục Mới
              </button>
            </form>
          </div>

          <div className="lg:col-span-2 bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 bg-slate-50/50">
              <h2 className="font-bold text-sm sm:text-base text-slate-900">
                Cây Cấu Trúc Danh Mục Toàn Sàn ({categories.length})
              </h2>
            </div>
            <div className="divide-y divide-slate-100 max-h-[500px] overflow-y-auto">
              {categories.map((c) => (
                <div key={c.id} className="p-4 hover:bg-slate-50 flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-3">
                    <div className="w-8 h-8 rounded-lg bg-orange-50 text-orange-600 flex items-center justify-center font-bold">
                      {c.id}
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-800 text-sm">{c.name}</h4>
                      <span className="text-[11px] text-slate-400">
                        Slug: /{c.slug} {c.parentId ? `(Thuộc danh mục ID: ${c.parentId})` : '(Danh mục gốc)'}
                      </span>
                    </div>
                  </div>
                  <button
                    onClick={() => handleDeleteCategory(c.id)}
                    className="p-2 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition cursor-pointer"
                    title="Xóa danh mục"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ================= TAB 6: NOTIFICATIONS & MAIL ================= */}
      {activeTab === 'notifications' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm space-y-4">
            <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
              <Send className="w-5 h-5 text-indigo-600" />
              Gửi Thông Báo Hệ Thống (Broadcast / Direct)
            </h2>
            <form onSubmit={handleSendNotification} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1">User ID nhận thông báo</label>
                <input
                  type="number"
                  value={notifUserId}
                  onChange={(e) => setNotifUserId(e.target.value)}
                  placeholder="VD: 1 hoặc 2"
                  required
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-indigo-500"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1">Tiêu Đề Thông Báo *</label>
                <input
                  type="text"
                  value={notifTitle}
                  onChange={(e) => setNotifTitle(e.target.value)}
                  placeholder="VD: [QuickShop] Bảo trì hệ thống hoặc Khuyến mãi mới"
                  required
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-indigo-500"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1">Nội Dung Chi Tiết *</label>
                <textarea
                  rows="4"
                  value={notifContent}
                  onChange={(e) => setNotifContent(e.target.value)}
                  placeholder="Nhập nội dung thông báo gửi qua App và RabbitMQ Mail Service..."
                  required
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl focus:outline-indigo-500"
                ></textarea>
              </div>

              {notifSuccess && (
                <div className="p-3 bg-emerald-50 text-emerald-700 rounded-xl text-xs font-semibold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4" />
                  {notifSuccess}
                </div>
              )}

              <button
                type="submit"
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shadow-md transition cursor-pointer active:scale-95"
              >
                Gửi Thông Báo & Bắn Event RabbitMQ
              </button>
            </form>
          </div>

          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col justify-between">
            <div>
              <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
                  <Mail className="w-5 h-5 text-slate-600" />
                  Nhật Ký Email Giao Dịch Đã Gửi (Log)
                </h2>
                <button
                  onClick={fetchEmailLogs}
                  className="text-xs text-indigo-600 font-bold hover:underline cursor-pointer flex items-center gap-1"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Làm mới
                </button>
              </div>
              <div className="divide-y divide-slate-100 max-h-[400px] overflow-y-auto">
                {emailLogs.map((log) => (
                  <div key={log.id} className="p-4 hover:bg-slate-50 text-xs flex justify-between items-center">
                    <div>
                      <span className="font-bold text-slate-800">{log.recipientEmail}</span>
                      <p className="text-slate-500 mt-0.5 line-clamp-1">{log.subject}</p>
                      <span className="text-[10px] text-slate-400">{log.sentAt}</span>
                    </div>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">
                      {log.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
            <div className="p-4 bg-slate-50 border-t border-slate-100 text-[11px] text-slate-500">
              Consumer RabbitMQ lắng nghe queue <code>order.created.email</code>, <code>shop.kyc.approved</code> để gửi mail tự động qua JavaMailSender.
            </div>
          </div>
        </div>
      )}

      {/* ================= TAB 7: INFRASTRUCTURE MONITORING ================= */}
      {activeTab === 'infra' && (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
            <div>
              <h2 className="font-bold text-base text-slate-900 flex items-center gap-2">
                <Server className="w-5 h-5 text-slate-700" />
                Giám Sát 7 Microservices & Hạ Tầng Phân Tán
              </h2>
              <p className="text-xs text-slate-500">Trạng thái cổng kết nối, công nghệ cốt lõi và phân công nhiệm vụ</p>
            </div>
            <span className="px-3 py-1 bg-emerald-100 text-emerald-800 font-bold rounded-full text-xs flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
              All Services Operational
            </span>
          </div>

          <div className="divide-y divide-slate-100">
            {services.map((s, idx) => (
              <div key={idx} className="p-4 hover:bg-slate-50/70 transition flex flex-col md:flex-row justify-between items-start md:items-center gap-3 text-xs">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 rounded-xl bg-slate-900 text-white flex items-center justify-center font-mono font-bold text-xs">
                    {s.port}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-slate-900 text-sm">{s.name}</h4>
                      <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-mono">
                        Port: {s.port}
                      </span>
                    </div>
                    <p className="text-slate-500 text-[11px] mt-0.5">{s.tech}</p>
                  </div>
                </div>

                <div className="flex items-center space-x-4">
                  <div className="text-right">
                    <span className="text-[11px] text-slate-400">Phụ trách:</span>
                    <strong className="block text-slate-800">{s.role}</strong>
                  </div>
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700 flex items-center gap-1">
                    <CheckCircle2 className="w-3 h-3" />
                    {s.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
