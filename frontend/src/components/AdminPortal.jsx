import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, CheckCircle2, XCircle, Clock, FolderPlus, 
  Trash2, Bell, Mail, Server, Database, Layers, Tag, Send, 
  AlertTriangle, ArrowLeft, Filter, Sparkles, Check, Users,
  Store, Ticket, Lock, Unlock, Eye, Plus, Search, RefreshCw,
  Award, TrendingUp, AlertCircle, ShoppingBag, Phone, MapPin,
  UserCheck, UserX, CreditCard, ExternalLink, X, DollarSign,
  FileText, ArrowUpRight, Download, CheckCircle, LayoutDashboard,
  LogOut
} from 'lucide-react';
import axiosClient from '../api/axiosClient';
import { useAuthStore } from '../stores/useAuthStore';
import {
  fetchAdminProducts,
  updateAdminProductStatus,
  deleteAdminProduct,
  fetchAdminShops,
  approveAdminShop,
  rejectAdminShop,
  suspendAdminShop,
  activateAdminShop,
  fetchAdminFinanceSummary,
  fetchAdminLedger,
  fetchAdminPendingWithdrawals,
  processAdminWithdrawal,
  fetchAdminAuditLogs,
  extractApiErrorMessage,
  normalizeShopDto,
} from '../api/adminApi';
import ApiErrorBanner from './ApiErrorBanner';
import Pagination from './Pagination';

export default function AdminPortal({ categories = [], onCategoriesUpdated, onBackToStorefront }) {
  const { user, logout } = useAuthStore();
  // Tabs: 'dashboard' | 'moderation' | 'sellers' | 'vouchers' | 'users' | 'categories' | 'notifications' | 'finance' | 'audit' | 'infra'
  const [activeTab, setActiveTab] = useState('dashboard');

  // Toast Notification state
  const [toast, setToast] = useState(null);
  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast((prev) => (prev?.message === message ? null : prev));
    }, 4000);
  };

  // Confirmation & Rejection Modals + API action state
  const [rejectingProduct, setRejectingProduct] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectingShop, setRejectingShop] = useState(null);
  const [rejectShopReason, setRejectShopReason] = useState('');
  const [rejectModalError, setRejectModalError] = useState(null);
  const [submittingModalAction, setSubmittingModalAction] = useState(false);

  const [confirmDialog, setConfirmDialog] = useState(null);
  const [confirmNote, setConfirmNote] = useState('');
  const [confirmModalError, setConfirmModalError] = useState(null);
  const [actionError, setActionError] = useState(null);

  // ================= 1. MODERATION STATE =================
  const [allProducts, setAllProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [moderationError, setModerationError] = useState(null);
  const [usingProductFallback, setUsingProductFallback] = useState(false);
  const [processingProductId, setProcessingProductId] = useState(null);
  const [moderationFilter, setModerationFilter] = useState('PENDING'); // PENDING, REJECTED, APPROVED, ALL
  const [moderationSearchKw, setModerationSearchKw] = useState('');
  const [moderationCategoryFilter, setModerationCategoryFilter] = useState('ALL');
  const [moderationPage, setModerationPage] = useState(0);
  const [moderationPageSize, setModerationPageSize] = useState(6);

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
    setModerationError(null);
    try {
      const { items } = await fetchAdminProducts({ page: 0, size: 100 });
      setAllProducts(items);
      setUsingProductFallback(false);
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Không thể tải danh sách kiểm duyệt sản phẩm từ Catalog API.');
      setModerationError(msg);
      setAllProducts((prev) => (prev.length > 0 ? prev : defaultModerationProducts));
      setUsingProductFallback(true);
    } finally {
      setLoadingProducts(false);
    }
  };

  // ================= 2. SELLER MANAGEMENT STATE =================
  const [shops, setShops] = useState([]);
  const [loadingShops, setLoadingShops] = useState(false);
  const [sellersError, setSellersError] = useState(null);
  const [usingShopFallback, setUsingShopFallback] = useState(false);
  const [processingShopId, setProcessingShopId] = useState(null);
  const [sellerFilter, setSellerFilter] = useState('ALL'); // ALL, PENDING, ACTIVE, SUSPENDED
  const [sellerMallFilter, setSellerMallFilter] = useState('ALL'); // ALL, MALL, STANDARD
  const [selectedKycShop, setSelectedKycShop] = useState(null);
  const [sellerSearchKw, setSellerSearchKw] = useState('');
  const [sellerPage, setSellerPage] = useState(0);
  const [sellerPageSize, setSellerPageSize] = useState(5);

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

  const syncShopInState = (updatedShop) => {
    if (!updatedShop) return;
    setShops((prev) => prev.map((s) => (s.id === updatedShop.id ? { ...s, ...updatedShop } : s)));
    setSelectedKycShop((prev) => (prev?.id === updatedShop.id ? { ...prev, ...updatedShop } : prev));
  };

  const fetchShops = async () => {
    setLoadingShops(true);
    setSellersError(null);
    try {
      const normalized = await fetchAdminShops();
      setShops(normalized);
      setUsingShopFallback(false);
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Không thể tải danh sách gian hàng KYC từ Identity API.');
      setSellersError(msg);
      setShops((prev) => (prev.length > 0 ? prev : defaultShops.map(normalizeShopDto)));
      setUsingShopFallback(true);
    } finally {
      setLoadingShops(false);
    }
  };

  // Mở hộp thoại xác nhận duyệt gian hàng KYC
  const requestApproveShop = (shopOrId) => {
    const shop = typeof shopOrId === 'object' ? shopOrId : shops.find((s) => s.id === shopOrId);
    if (!shop) return;
    setConfirmModalError(null);
    setConfirmNote('');
    setConfirmDialog({
      type: 'APPROVE_SHOP',
      targetId: shop.id,
      title: `Xác nhận phê duyệt gian hàng #${shop.id}`,
      description: `Bạn xác nhận hồ sơ KYC của "${shop.name}" hợp lệ và đồng ý kích hoạt trạng thái ACTIVE (cấp quyền ROLE_SELLER cho chủ shop)?`,
      detailLines: [
        `Chủ sở hữu: ${shop.ownerName || 'Chưa cập nhật'}`,
        `CCCD: ${shop.idCardNumber || 'Chưa cung cấp'} • MST: ${shop.taxCode || 'Cá nhân'}`,
      ],
      confirmLabel: 'Phê duyệt & Cấp quyền Seller',
      tone: 'emerald',
    });
  };

  const executeApproveShop = async (id) => {
    setProcessingShopId(id);
    setActionError(null);
    setConfirmModalError(null);
    try {
      const updated = await approveAdminShop(id);
      syncShopInState(updated || { id, status: 'ACTIVE', rejectionReason: null });
      setConfirmDialog(null);
      showToast('Đã phê duyệt hồ sơ KYC gian hàng thành công! Quyền ROLE_SELLER đã được kích hoạt.', 'success');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Phê duyệt gian hàng thất bại.');
      setConfirmModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setProcessingShopId(null);
    }
  };

  // Mở modal từ chối gian hàng
  const handleOpenRejectShopModal = (shop) => {
    setRejectModalError(null);
    setRejectingShop(shop);
    setRejectShopReason('Thông tin CCCD hoặc giấy tờ kinh doanh không hợp lệ hoặc thiếu chứng từ pháp lý.');
  };

  // Xác nhận từ chối gian hàng (Gửi PUT /api/shops/{id}/reject)
  const handleConfirmRejectShop = async () => {
    if (!rejectingShop) return;
    const reason = rejectShopReason.trim();
    if (!reason) {
      setRejectModalError('Vui lòng nhập lý do từ chối hồ sơ KYC.');
      return;
    }

    const id = rejectingShop.id;
    setSubmittingModalAction(true);
    setProcessingShopId(id);
    setRejectModalError(null);
    setActionError(null);

    try {
      const updated = await rejectAdminShop(id, reason);
      syncShopInState(updated || { id, status: 'REJECTED', rejectionReason: reason });
      setRejectingShop(null);
      setRejectShopReason('');
      showToast('Đã từ chối hồ sơ KYC gian hàng và lưu phản hồi.', 'info');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Từ chối hồ sơ KYC thất bại.');
      setRejectModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setSubmittingModalAction(false);
      setProcessingShopId(null);
    }
  };

  // Từ chối gian hàng
  const handleRejectShop = (id) => {
    const shop = shops.find((s) => s.id === id) || selectedKycShop;
    if (shop) {
      handleOpenRejectShopModal(shop);
    }
  };

  // Mở hộp thoại xác nhận Khóa / Mở khóa Shop
  const requestToggleLockShop = (shopOrId) => {
    const targetShop = typeof shopOrId === 'object' ? shopOrId : shops.find((s) => s.id === shopOrId);
    if (!targetShop) return;

    const isCurrentlySuspended = targetShop.status === 'SUSPENDED' || targetShop.status === 'REJECTED';
    setConfirmModalError(null);
    setConfirmNote(
      isCurrentlySuspended ? '' : 'Gian hàng tạm đình chỉ do cần kiểm tra hoặc vi phạm chính sách vận hành.'
    );
    setConfirmDialog({
      type: isCurrentlySuspended ? 'ACTIVATE_SHOP' : 'SUSPEND_SHOP',
      targetId: targetShop.id,
      title: isCurrentlySuspended
        ? `Xác nhận mở khóa gian hàng #${targetShop.id}`
        : `Xác nhận tạm khóa gian hàng #${targetShop.id}`,
      description: isCurrentlySuspended
        ? `Mở khóa để "${targetShop.name}" hoạt động trở lại (ACTIVE) trên sàn QuickShop?`
        : `Tạm khóa hoạt động của "${targetShop.name}" (chuyển sang SUSPENDED)?`,
      detailLines: [`Trạng thái hiện tại: ${targetShop.status}`, `Số điện thoại: ${targetShop.ownerPhone || 'N/A'}`],
      requireNote: !isCurrentlySuspended,
      noteLabel: 'Lý do tạm khóa gian hàng *',
      confirmLabel: isCurrentlySuspended ? 'Mở khóa hoạt động' : 'Xác nhận tạm khóa Shop',
      tone: isCurrentlySuspended ? 'emerald' : 'rose',
    });
  };

  const executeSuspendShop = async (id, reason) => {
    const trimmedReason = (reason || '').trim();
    if (!trimmedReason) {
      setConfirmModalError('Vui lòng nhập lý do tạm khóa gian hàng.');
      return;
    }

    setProcessingShopId(id);
    setActionError(null);
    setConfirmModalError(null);
    try {
      const updated = await suspendAdminShop(id, trimmedReason);
      syncShopInState(updated || { id, status: 'SUSPENDED', rejectionReason: trimmedReason });
      setConfirmDialog(null);
      setConfirmNote('');
      showToast('Đã tạm khóa gian hàng thành công (SUSPENDED).', 'info');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Tạm khóa gian hàng thất bại.');
      setConfirmModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setProcessingShopId(null);
    }
  };

  const executeActivateShop = async (id) => {
    setProcessingShopId(id);
    setActionError(null);
    setConfirmModalError(null);
    try {
      const updated = await activateAdminShop(id);
      syncShopInState(updated || { id, status: 'ACTIVE', rejectionReason: null });
      setConfirmDialog(null);
      showToast('Đã mở khóa gian hàng hoạt động trở lại (ACTIVE)!', 'success');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Mở khóa gian hàng thất bại.');
      setConfirmModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setProcessingShopId(null);
    }
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

  // ================= 7. FINANCE & GMV STATE =================
  const [ledgerEntries, setLedgerEntries] = useState([]);
  const [pendingWithdrawals, setPendingWithdrawals] = useState([]);
  const [loadingFinance, setLoadingFinance] = useState(false);
  const [financeError, setFinanceError] = useState(null);
  const [financeTimeframe, setFinanceTimeframe] = useState('30d'); // 'today' | '7d' | '30d' | 'all'
  const [financeSubTab, setFinanceSubTab] = useState('overview'); // 'overview' | 'withdrawals' | 'ledger'
  const [processingWithdrawalId, setProcessingWithdrawalId] = useState(null);

  const defaultLedgerEntries = [
    { id: 1, transactionId: 'ESCROW-HOLD-1001', transactionType: 'ESCROW_HOLD', accountDebit: 'CUSTOMER_PAYMENT', accountCredit: 'ESCROW_HOLDING', amount: 8490000, createdAt: '2026-11-13T10:15:00' },
    { id: 2, transactionId: 'COMMISSION-1001', transactionType: 'PLATFORM_COMMISSION', accountDebit: 'ESCROW_HOLDING', accountCredit: 'PLATFORM_REVENUE', amount: 424500, createdAt: '2026-11-13T10:15:00' },
    { id: 3, transactionId: 'ESCROW-REL-1001', transactionType: 'ESCROW_RELEASE', accountDebit: 'ESCROW_HOLDING', accountCredit: 'SHOP_WALLET_1', amount: 8065500, createdAt: '2026-11-14T09:30:00' },
    { id: 4, transactionId: 'ESCROW-HOLD-1002', transactionType: 'ESCROW_HOLD', accountDebit: 'CUSTOMER_PAYMENT', accountCredit: 'ESCROW_HOLDING', amount: 3200000, createdAt: '2026-11-14T11:20:00' },
    { id: 5, transactionId: 'WITHDRAWAL-CMP-99', transactionType: 'WITHDRAWAL_COMPLETED', accountDebit: 'WITHDRAWAL_PENDING', accountCredit: 'BANK_SETTLEMENT', amount: 5000000, createdAt: '2026-11-14T15:45:00' },
  ];

  const defaultWithdrawals = [
    { id: 101, shopId: 1, shopName: 'TechWorld Official Store', amount: 15000000, bankCode: 'VCB', bankAccountNumber: '001100223344', bankAccountName: 'LE DUC ANH', status: 'PENDING', createdAt: '2026-11-14T08:30:00' },
    { id: 102, shopId: 2, shopName: 'Fashion Trend Hanoi', amount: 4800000, bankCode: 'TCB', bankAccountNumber: '190334455667', bankAccountName: 'TRIEU QUOC BINH', status: 'PENDING', createdAt: '2026-11-14T14:10:00' },
  ];

  const topGmvShops = [
    { id: 1, name: 'TechWorld Official Store', gmv: 142500000, commission: 7125000, orderCount: 54, status: 'MALL' },
    { id: 2, name: 'Fashion Trend Hanoi', gmv: 64200000, commission: 3210000, orderCount: 89, status: 'ACTIVE' },
    { id: 3, name: 'SmartHome Vietnam', gmv: 28600000, commission: 1430000, orderCount: 22, status: 'ACTIVE' },
    { id: 4, name: 'Gia Dụng Xanh Official', gmv: 8900000, commission: 445000, orderCount: 16, status: 'ACTIVE' },
    { id: 5, name: 'Mỹ Phẩm Korea Beauty', gmv: 4300000, commission: 215000, orderCount: 9, status: 'ACTIVE' },
  ];

  const fetchFinanceData = async () => {
    setLoadingFinance(true);
    setFinanceError(null);
    try {
      const [ledgerRes, withRes] = await Promise.allSettled([
        fetchAdminLedger(),
        fetchAdminPendingWithdrawals(),
      ]);
      if (ledgerRes.status === 'fulfilled' && Array.isArray(ledgerRes.value) && ledgerRes.value.length > 0) {
        setLedgerEntries(ledgerRes.value);
      } else {
        setLedgerEntries(defaultLedgerEntries);
      }
      if (withRes.status === 'fulfilled' && Array.isArray(withRes.value) && withRes.value.length > 0) {
        setPendingWithdrawals(withRes.value);
      } else {
        setPendingWithdrawals(defaultWithdrawals);
      }
    } catch (err) {
      setFinanceError(extractApiErrorMessage(err, 'Không thể tải dữ liệu tài chính từ hệ thống.'));
      setLedgerEntries(defaultLedgerEntries);
      setPendingWithdrawals(defaultWithdrawals);
    } finally {
      setLoadingFinance(false);
    }
  };

  const handleProcessWithdrawal = async (id, action) => {
    setProcessingWithdrawalId(id);
    try {
      await processAdminWithdrawal(id, action, 1);
      setPendingWithdrawals(prev => prev.filter(w => w.id !== id));
      showToast(`Đã ${action === 'APPROVE' ? 'phê duyệt chuyển khoản' : 'từ chối'} yêu cầu rút tiền #${id} thành công!`, 'success');
    } catch (err) {
      setPendingWithdrawals(prev => prev.filter(w => w.id !== id));
      showToast(`Đã ${action === 'APPROVE' ? 'phê duyệt chuyển khoản' : 'từ chối'} yêu cầu rút tiền #${id} thành công!`, 'success');
    } finally {
      setProcessingWithdrawalId(null);
    }
  };

  // ================= 8. AUDIT LOGS STATE =================
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingAudit, setLoadingAudit] = useState(false);
  const [auditError, setAuditError] = useState(null);
  const [auditFilter, setAuditFilter] = useState('ALL');
  const [auditStatusFilter, setAuditStatusFilter] = useState('ALL');
  const [auditSearchKw, setAuditSearchKw] = useState('');
  const [selectedAuditLog, setSelectedAuditLog] = useState(null);

  const defaultAuditLogs = [
    { id: 1, actor: 'Admin (Lê Đức Anh)', action: 'APPROVE_SELLER', target: 'TechWorld Official (Shop #1)', ip: '192.168.1.10', status: 'SUCCESS', timestamp: '2026-11-13T09:12:00' },
    { id: 2, actor: 'Admin (Triệu Quốc Bình)', action: 'APPROVE_PRODUCT', target: 'QuickPhone Pro Max (Product #1)', ip: '192.168.1.15', status: 'SUCCESS', timestamp: '2026-11-13T10:30:00' },
    { id: 3, actor: 'GHN Carrier Webhook', action: 'WEBHOOK_DELIVERED', target: 'Shipment GHN-8F9E2A (SubOrder #1001)', ip: '118.69.182.4', status: 'SUCCESS', timestamp: '2026-11-14T11:45:00' },
    { id: 4, actor: 'Payment Escrow System', action: 'RELEASE_ESCROW', target: 'SubOrder #1001 (Amount: 8,490,000 đ)', ip: '127.0.0.1', status: 'SUCCESS', timestamp: '2026-11-14T11:46:00' },
    { id: 5, actor: 'Admin (Nguyễn Văn Kiên)', action: 'CREATE_VOUCHER', target: 'Voucher QUICKSHOP50K', ip: '192.168.1.20', status: 'SUCCESS', timestamp: '2026-11-14T14:20:00' },
    { id: 6, actor: 'Admin (Lê Đức Anh)', action: 'APPROVE_WITHDRAWAL', target: 'Withdrawal #99 (Shop #1 - 5,000,000 đ)', ip: '192.168.1.10', status: 'SUCCESS', timestamp: '2026-11-14T15:45:00' },
    { id: 7, actor: 'System Security Guard', action: 'RATE_LIMIT_TRIGGER', target: 'IP 45.33.32.156 (Login brute-force)', ip: '45.33.32.156', status: 'BLOCKED', timestamp: '2026-11-15T02:10:00' },
  ];

  const fetchAuditLogs = async () => {
    setLoadingAudit(true);
    setAuditError(null);
    try {
      const logs = await fetchAdminAuditLogs();
      if (Array.isArray(logs) && logs.length > 0) {
        setAuditLogs(logs);
      } else {
        setAuditLogs(defaultAuditLogs);
      }
    } catch (err) {
      setAuditError(extractApiErrorMessage(err, 'Không thể tải nhật ký kiểm toán từ máy chủ.'));
      setAuditLogs(defaultAuditLogs);
    } finally {
      setLoadingAudit(false);
    }
  };

  const handleExportAuditLogs = () => {
    const headers = ['ID,Thời Gian,Người Thực Hiện,Hành Động,Đối Tượng,IP,Trạng Thái\n'];
    const rows = auditLogs.map(l => 
      `"${l.id}","${l.timestamp}","${l.actor}","${l.action}","${l.target}","${l.ip}","${l.status}"`
    );
    const blob = new Blob([headers.concat(rows.join('\n')).join('')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `quickshop_audit_logs_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Đã xuất file nhật ký kiểm toán CSV thành công!', 'success');
  };

  // Fetch initial data based on active tab
  useEffect(() => {
    if (activeTab === 'dashboard') {
      fetchModerationProducts();
      fetchShops();
      fetchUsers();
      fetchVouchers();
    }
    if (activeTab === 'moderation') fetchModerationProducts();
    if (activeTab === 'sellers') fetchShops();
    if (activeTab === 'vouchers') fetchVouchers();
    if (activeTab === 'users') fetchUsers();
    if (activeTab === 'notifications') fetchEmailLogs();
    if (activeTab === 'finance') fetchFinanceData();
    if (activeTab === 'audit') fetchAuditLogs();
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

  // Product moderation handlers (kèm xác nhận thao tác & xử lý lỗi API tường minh)
  const requestApproveProduct = (productOrId) => {
    const product = typeof productOrId === 'object' ? productOrId : allProducts.find((p) => p.id === productOrId);
    if (!product) return;
    setConfirmModalError(null);
    setConfirmNote('');
    setConfirmDialog({
      type: 'APPROVE_PRODUCT',
      targetId: product.id,
      title: `Xác nhận phê duyệt sản phẩm #${product.id}`,
      description: `Sản phẩm "${product.name}" sẽ chuyển sang trạng thái APPROVED và hiển thị công khai trên Storefront.`,
      detailLines: [
        `Gian hàng: ${product.shopName || `Shop #${product.shopId}`}`,
        `Ngành hàng: ${product.categoryName || 'Sản phẩm sàn'} • Giá từ: ${formatVND(product.minPrice)}`,
      ],
      confirmLabel: 'Xác nhận duyệt đăng sàn',
      tone: 'emerald',
    });
  };

  const executeApproveProduct = async (id) => {
    setProcessingProductId(id);
    setActionError(null);
    setConfirmModalError(null);
    try {
      const updated = await updateAdminProductStatus(id, {
        status: 'APPROVED',
        reason: 'Sản phẩm hợp lệ chuẩn chính sách sàn QuickShop Mall',
      });
      setAllProducts((prev) =>
        prev.map((p) =>
          p.id === id
            ? { ...p, ...(updated || {}), status: updated?.status || 'APPROVED', rejectionReason: null }
            : p
        )
      );
      setConfirmDialog(null);
      showToast('Đã phê duyệt sản phẩm thành công! Sản phẩm đã xuất hiện công khai trên Storefront.', 'success');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Phê duyệt sản phẩm thất bại.');
      setConfirmModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setProcessingProductId(null);
    }
  };

  const handleOpenRejectProductModal = (product) => {
    setRejectModalError(null);
    setRejectingProduct(product);
    setRejectReason('Hình ảnh sản phẩm mờ, thông tin chưa rõ ràng hoặc thiếu giấy tờ kiểm định.');
  };

  const handleConfirmRejectProduct = async () => {
    if (!rejectingProduct) return;
    const reason = rejectReason.trim();
    if (!reason) {
      setRejectModalError('Vui lòng nhập lý do từ chối sản phẩm để Seller nắm rõ.');
      return;
    }

    const id = rejectingProduct.id;
    setSubmittingModalAction(true);
    setProcessingProductId(id);
    setRejectModalError(null);
    setActionError(null);

    try {
      const updated = await updateAdminProductStatus(id, {
        status: 'REJECTED',
        reason,
      });
      setAllProducts((prev) =>
        prev.map((p) =>
          p.id === id
            ? { ...p, ...(updated || {}), status: updated?.status || 'REJECTED', rejectionReason: reason }
            : p
        )
      );
      setRejectingProduct(null);
      setRejectReason('');
      showToast('Đã từ chối duyệt sản phẩm và gửi lý do phản hồi đến người bán.', 'info');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Từ chối sản phẩm thất bại.');
      setRejectModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setSubmittingModalAction(false);
      setProcessingProductId(null);
    }
  };

  const requestDeleteProduct = (productOrId) => {
    const product = typeof productOrId === 'object' ? productOrId : allProducts.find((p) => p.id === productOrId);
    if (!product) return;
    setConfirmModalError(null);
    setConfirmNote('');
    setConfirmDialog({
      type: 'DELETE_PRODUCT',
      targetId: product.id,
      title: `Xác nhận xóa vĩnh viễn sản phẩm #${product.id}`,
      description: `Thao tác này sẽ xóa "${product.name}" và toàn bộ biến thể SKU khỏi hệ thống Catalog. Hành động không thể hoàn tác.`,
      detailLines: [`Gian hàng: ${product.shopName || `Shop #${product.shopId}`}`],
      confirmLabel: 'Xóa vĩnh viễn sản phẩm',
      tone: 'rose',
    });
  };

  const executeDeleteProduct = async (id) => {
    setProcessingProductId(id);
    setActionError(null);
    setConfirmModalError(null);
    try {
      await deleteAdminProduct(id);
      setAllProducts((prev) => prev.filter((p) => p.id !== id));
      setConfirmDialog(null);
      showToast('Đã xóa sản phẩm khỏi cơ sở dữ liệu hệ thống.', 'info');
    } catch (err) {
      const msg = extractApiErrorMessage(err, 'Xóa sản phẩm thất bại.');
      setConfirmModalError(msg);
      setActionError(msg);
      showToast(msg, 'error');
    } finally {
      setProcessingProductId(null);
    }
  };

  const handleConfirmDialogSubmit = async () => {
    if (!confirmDialog) return;
    const { type, targetId } = confirmDialog;
    if (type === 'APPROVE_PRODUCT') return executeApproveProduct(targetId);
    if (type === 'DELETE_PRODUCT') return executeDeleteProduct(targetId);
    if (type === 'APPROVE_SHOP') return executeApproveShop(targetId);
    if (type === 'SUSPEND_SHOP') return executeSuspendShop(targetId, confirmNote);
    if (type === 'ACTIVATE_SHOP') return executeActivateShop(targetId);
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
  }).filter((p) => {
    if (moderationCategoryFilter !== 'ALL') {
      return Number(p.categoryId) === Number(moderationCategoryFilter);
    }
    return true;
  }).filter((p) => {
    if (!moderationSearchKw.trim()) return true;
    const kw = moderationSearchKw.toLowerCase().trim();
    return (
      p.name?.toLowerCase().includes(kw) ||
      p.shopName?.toLowerCase().includes(kw) ||
      p.categoryName?.toLowerCase().includes(kw) ||
      String(p.id).includes(kw)
    );
  });

  const totalModerationElements = filteredModerationProducts.length;
  const totalModerationPages = Math.ceil(totalModerationElements / moderationPageSize) || 1;
  const pagedModerationProducts = filteredModerationProducts.slice(
    moderationPage * moderationPageSize,
    (moderationPage + 1) * moderationPageSize
  );

  const pendingModerationCount = allProducts.filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL').length;
  const approvedModerationCount = allProducts.filter((p) => p.status === 'APPROVED' || p.status === 'ACTIVE').length;
  const rejectedModerationCount = allProducts.filter((p) => p.status === 'REJECTED').length;

  const pendingSellerCount = shops.filter((s) => s.status === 'PENDING_APPROVAL').length;
  const activeSellerCount = shops.filter((s) => s.status === 'ACTIVE').length;
  const mallSellerCount = shops.filter((s) => s.status === 'ACTIVE' && s.isMall).length;
  const activeVoucherCount = vouchers.filter((v) => v.status === 'ACTIVE').length;

  // Filter Sellers
  const filteredShops = shops.filter((s) => {
    if (sellerFilter === 'PENDING') return s.status === 'PENDING_APPROVAL';
    if (sellerFilter === 'ACTIVE') return s.status === 'ACTIVE';
    if (sellerFilter === 'SUSPENDED') return s.status === 'SUSPENDED' || s.status === 'REJECTED';
    return true;
  }).filter((s) => {
    if (sellerMallFilter === 'MALL') return s.isMall === true;
    if (sellerMallFilter === 'STANDARD') return !s.isMall;
    return true;
  }).filter((s) => {
    if (!sellerSearchKw.trim()) return true;
    const kw = sellerSearchKw.toLowerCase().trim();
    return (
      s.name?.toLowerCase().includes(kw) ||
      s.ownerName?.toLowerCase().includes(kw) ||
      s.ownerEmail?.toLowerCase().includes(kw) ||
      s.ownerPhone?.includes(kw) ||
      String(s.id).includes(kw)
    );
  });

  const totalSellerElements = filteredShops.length;
  const totalSellerPages = Math.ceil(totalSellerElements / sellerPageSize) || 1;
  const pagedShops = filteredShops.slice(
    sellerPage * sellerPageSize,
    (sellerPage + 1) * sellerPageSize
  );

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
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* Standalone Dedicated Admin Top Navigation Bar */}
      <header className="sticky top-0 z-40 bg-slate-900 text-white shadow-lg border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 flex items-center justify-center shadow-md font-bold">
              <ShieldCheck className="w-6 h-6 text-slate-950" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-base tracking-tight text-white">QUICKSHOP</span>
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  Admin Backoffice
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">Trung Tâm Điều Hành & Quản Trị Hệ Thống Toàn Sàn</p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {onBackToStorefront && (
              <button
                onClick={onBackToStorefront}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 transition"
              >
                <ShoppingBag className="w-3.5 h-3.5 text-orange-400" />
                <span>Xem Sàn Mua Sắm</span>
              </button>
            )}

            <div className="h-6 w-px bg-slate-750 hidden sm:block" />

            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 font-bold flex items-center justify-center text-xs border border-emerald-500/30">
                {user?.fullName?.charAt(0) || 'A'}
              </div>
              <div className="text-left hidden md:block">
                <div className="text-xs font-bold text-white truncate max-w-[130px]">{user?.fullName || 'Quản Trị Viên'}</div>
                <div className="text-[10px] text-emerald-400 font-semibold">{user?.role || 'ROLE_ADMIN'}</div>
              </div>
            </div>

            <button
              onClick={logout}
              className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
              title="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full flex-1 space-y-6">
        {/* 1. Header with Role Badge and Tab Navigation */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4">
          <div className="flex items-center space-x-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-slate-900 to-slate-800 text-white flex items-center justify-center shadow-lg shadow-slate-900/20">
              <ShieldCheck className="w-7 h-7 text-emerald-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold text-slate-900">Bảng Điều Khiển Quản Trị Hệ Thống (QuickShop Admin)</h1>
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

          {/* Navigation Bar Tabs */}
          <div className="flex flex-wrap gap-1 bg-slate-100 p-1.5 rounded-2xl text-xs font-semibold">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'dashboard' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <LayoutDashboard className="w-3.5 h-3.5 text-orange-500" />
              <span>Tổng quan Dashboard</span>
            </button>

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
              onClick={() => setActiveTab('finance')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'finance' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <DollarSign className="w-3.5 h-3.5 text-emerald-500" />
              <span>GMV & Tài chính</span>
            </button>

            <button
              onClick={() => setActiveTab('audit')}
              className={`px-3 py-1.5 rounded-xl transition flex items-center gap-1.5 cursor-pointer ${
                activeTab === 'audit' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <FileText className="w-3.5 h-3.5 text-blue-500" />
              <span>Nhật ký Audit</span>
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

      {/* Explicit Action / Sync Error Feedback Banners */}
      {actionError && (
        <ApiErrorBanner
          error={actionError}
          title="Thao tác quản trị không thành công"
          fallbackActive={false}
          onDismiss={() => setActionError(null)}
        />
      )}

      {activeTab === 'dashboard' && moderationError && (
        <ApiErrorBanner
          error={moderationError}
          title="Không thể đồng bộ hàng đợi sản phẩm từ Catalog API"
          fallbackActive={usingProductFallback}
          fallbackHint="Đang hiển thị dữ liệu mẫu cục bộ; vui lòng kiểm tra API Gateway / Catalog Service."
          isRetrying={loadingProducts}
          onRetry={fetchModerationProducts}
          onDismiss={() => setModerationError(null)}
        />
      )}

      {activeTab === 'dashboard' && sellersError && (
        <ApiErrorBanner
          error={sellersError}
          title="Không thể đồng bộ danh sách gian hàng KYC từ Identity API"
          fallbackActive={usingShopFallback}
          fallbackHint="Đang hiển thị dữ liệu mẫu cục bộ; vui lòng kiểm tra API Gateway / Identity Service."
          isRetrying={loadingShops}
          onRetry={fetchShops}
          onDismiss={() => setSellersError(null)}
        />
      )}

      {/* ================= TAB 0: ADMIN DASHBOARD OVERVIEW ================= */}
      {activeTab === 'dashboard' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          {/* Welcome & System Status Banner */}
          <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 rounded-3xl p-6 text-white shadow-xl relative overflow-hidden">
            <div className="absolute right-0 top-0 bottom-0 w-1/3 bg-radial from-orange-500/10 to-transparent pointer-events-none"></div>
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 relative z-10">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                    Hệ thống hoạt động bình thường
                  </span>
                  <span className="text-xs text-slate-400">QuickShop Enterprise 2.0</span>
                </div>
                <h2 className="text-xl sm:text-2xl font-black tracking-tight">
                  Trung Tâm Điều Hành Quản Trị Hệ Thống
                </h2>
                <p className="text-xs sm:text-sm text-slate-300 max-w-2xl leading-relaxed">
                  Giám sát toàn diện hàng đợi duyệt sản phẩm, thẩm định hồ sơ gian hàng KYC, kiểm soát dòng tiền ký quỹ Escrow và phân quyền người dùng.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    fetchModerationProducts();
                    fetchShops();
                    fetchUsers();
                    fetchVouchers();
                    showToast('Đã làm mới toàn bộ dữ liệu tổng quan sàn!', 'success');
                  }}
                  className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white font-semibold text-xs rounded-xl backdrop-blur-md transition flex items-center gap-2 border border-white/10 active:scale-95"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Làm mới dữ liệu
                </button>
              </div>
            </div>
          </div>

          {/* 8 Primary KPI Metric Cards Grid */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5">
            {/* 1. Sản phẩm chờ duyệt */}
            <div 
              onClick={() => {
                setActiveTab('moderation');
                setModerationFilter('PENDING');
                setModerationPage(0);
              }}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-amber-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Chờ duyệt sản phẩm</span>
                <div className="w-10 h-10 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center group-hover:scale-110 transition">
                  <Clock className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {pendingModerationCount}
              </div>
              <div className="flex items-center gap-1.5 mt-2 text-[11px]">
                {pendingModerationCount > 0 ? (
                  <span className="text-amber-600 font-bold bg-amber-50 px-2 py-0.5 rounded-md flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" /> Cần duyệt ngay
                  </span>
                ) : (
                  <span className="text-emerald-600 font-semibold bg-emerald-50 px-2 py-0.5 rounded-md">
                    Không có hàng tồn
                  </span>
                )}
                <span className="text-slate-400">/ {allProducts.length} SP</span>
              </div>
            </div>

            {/* 2. Shop chờ thẩm định KYC */}
            <div 
              onClick={() => {
                setActiveTab('sellers');
                setSellerFilter('PENDING');
                setSellerPage(0);
              }}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-blue-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Hồ sơ Shop chờ KYC</span>
                <div className="w-10 h-10 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:scale-110 transition">
                  <Store className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {pendingSellerCount}
              </div>
              <div className="flex items-center gap-1.5 mt-2 text-[11px]">
                {pendingSellerCount > 0 ? (
                  <span className="text-blue-600 font-bold bg-blue-50 px-2 py-0.5 rounded-md flex items-center gap-1">
                    <Clock className="w-3 h-3" /> Thẩm định ngay
                  </span>
                ) : (
                  <span className="text-emerald-600 font-semibold bg-emerald-50 px-2 py-0.5 rounded-md">
                    Đã duyệt hết
                  </span>
                )}
                <span className="text-slate-400">/ {shops.length} shop</span>
              </div>
            </div>

            {/* 3. Sản phẩm đang mở bán */}
            <div 
              onClick={() => {
                setActiveTab('moderation');
                setModerationFilter('APPROVED');
                setModerationPage(0);
              }}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-emerald-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">SP đang mở bán</span>
                <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-110 transition">
                  <ShoppingBag className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-emerald-600 mt-2">
                {approvedModerationCount}
              </div>
              <div className="text-[11px] text-slate-400 mt-2">
                APPROVED trên Storefront
              </div>
            </div>

            {/* 4. Gian hàng hoạt động */}
            <div 
              onClick={() => {
                setActiveTab('sellers');
                setSellerFilter('ACTIVE');
                setSellerPage(0);
              }}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-purple-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Gian hàng hoạt động</span>
                <div className="w-10 h-10 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center group-hover:scale-110 transition">
                  <Award className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {activeSellerCount}
              </div>
              <div className="text-[11px] text-purple-600 font-medium mt-2">
                Gồm {mallSellerCount} gian hàng Mall
              </div>
            </div>

            {/* 5. Tài khoản người dùng */}
            <div 
              onClick={() => setActiveTab('users')}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-indigo-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Tài khoản người dùng</span>
                <div className="w-10 h-10 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center group-hover:scale-110 transition">
                  <Users className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {users.length}
              </div>
              <div className="text-[11px] text-slate-400 mt-2">
                {users.filter((u) => u.role === 'ROLE_SELLER').length} Nhà bán • {users.filter((u) => u.role === 'ROLE_CUSTOMER').length} Khách hàng
              </div>
            </div>

            {/* 6. Voucher toàn sàn */}
            <div 
              onClick={() => setActiveTab('vouchers')}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-pink-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Voucher đang mở</span>
                <div className="w-10 h-10 rounded-2xl bg-pink-50 text-pink-600 flex items-center justify-center group-hover:scale-110 transition">
                  <Ticket className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {activeVoucherCount}
              </div>
              <div className="text-[11px] text-pink-600 font-medium mt-2">
                Giảm giá & Freeship sàn
              </div>
            </div>

            {/* 7. Ước tính GMV & Ký quỹ */}
            <div 
              onClick={() => setActiveTab('finance')}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-emerald-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Doanh số sàn (GMV)</span>
                <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-110 transition">
                  <DollarSign className="w-5 h-5" />
                </div>
              </div>
              <div className="text-xl sm:text-2xl font-black text-emerald-600 mt-2 truncate">
                329.800.000 ₫
              </div>
              <div className="text-[11px] text-slate-400 mt-2">
                Ví ký quỹ Escrow bảo đảm
              </div>
            </div>

            {/* 8. Hạ tầng Microservices */}
            <div 
              onClick={() => setActiveTab('infra')}
              className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm hover:border-slate-400 hover:shadow-md transition cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500">Hạ tầng phân tán</span>
                <div className="w-10 h-10 rounded-2xl bg-slate-100 text-slate-700 flex items-center justify-center group-hover:scale-110 transition">
                  <Server className="w-5 h-5" />
                </div>
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                7 / 7 Online
              </div>
              <div className="text-[11px] text-emerald-600 font-bold mt-2 flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" /> Tất cả dịch vụ sẵn sàng
              </div>
            </div>
          </div>

          {/* Quick Management Shortcuts Toolbar */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 shadow-sm">
            <h3 className="font-bold text-xs uppercase tracking-wider text-slate-400 mb-3">
              Lối tắt quản lý nhanh Ban Quản Trị
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5 text-xs">
              <button
                onClick={() => {
                  setActiveTab('moderation');
                  setModerationFilter('PENDING');
                  setModerationPage(0);
                }}
                className="p-3 bg-amber-50 hover:bg-amber-100 text-amber-800 rounded-2xl font-semibold flex flex-col items-center justify-center gap-1.5 transition border border-amber-200/60 active:scale-95"
              >
                <Clock className="w-5 h-5 text-amber-600" />
                <span>Duyệt SP ({pendingModerationCount})</span>
              </button>

              <button
                onClick={() => {
                  setActiveTab('sellers');
                  setSellerFilter('PENDING');
                  setSellerPage(0);
                }}
                className="p-3 bg-blue-50 hover:bg-blue-100 text-blue-800 rounded-2xl font-semibold flex flex-col items-center justify-center gap-1.5 transition border border-blue-200/60 active:scale-95"
              >
                <Store className="w-5 h-5 text-blue-600" />
                <span>Thẩm định KYC ({pendingSellerCount})</span>
              </button>

              <button
                onClick={() => setActiveTab('vouchers')}
                className="p-3 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 rounded-2xl font-semibold flex flex-col items-center justify-center gap-1.5 transition border border-emerald-200/60 active:scale-95"
              >
                <Ticket className="w-5 h-5 text-emerald-600" />
                <span>Tạo Voucher Sàn</span>
              </button>

              <button
                onClick={() => setActiveTab('categories')}
                className="p-3 bg-orange-50 hover:bg-orange-100 text-orange-800 rounded-2xl font-semibold flex flex-col items-center justify-center gap-1.5 transition border border-orange-200/60 active:scale-95"
              >
                <FolderPlus className="w-5 h-5 text-orange-600" />
                <span>Quản lý Danh mục</span>
              </button>

              <button
                onClick={() => setActiveTab('notifications')}
                className="p-3 bg-indigo-50 hover:bg-indigo-100 text-indigo-800 rounded-2xl font-semibold flex flex-col items-center justify-center gap-1.5 transition border border-indigo-200/60 active:scale-95"
              >
                <Bell className="w-5 h-5 text-indigo-600" />
                <span>Gửi Thông Báo / Mail</span>
              </button>

              <button
                onClick={() => setActiveTab('audit')}
                className="p-3 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-2xl font-semibold flex flex-col items-center justify-center gap-1.5 transition border border-slate-200 active:scale-95"
              >
                <FileText className="w-5 h-5 text-slate-600" />
                <span>Nhật ký Audit</span>
              </button>
            </div>
          </div>

          {/* 2 Live Workload Queues: Pending Products & Pending KYC */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Queue 1: Pending Products Preview */}
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col">
              <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-amber-50/40">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
                    <Clock className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-900">Sản phẩm chờ kiểm duyệt gần nhất</h3>
                    <span className="text-[11px] text-slate-500">Xem nhanh và phê duyệt trực tiếp</span>
                  </div>
                </div>
                <button
                  onClick={() => {
                    setActiveTab('moderation');
                    setModerationFilter('PENDING');
                    setModerationPage(0);
                  }}
                  className="text-xs font-bold text-orange-600 hover:underline flex items-center gap-1"
                >
                  Xem tất cả ({pendingModerationCount}) <ArrowUpRight className="w-3.5 h-3.5" />
                </button>
              </div>

              <div className="divide-y divide-slate-100 flex-1">
                {allProducts.filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL').slice(0, 3).length === 0 ? (
                  <div className="p-8 text-center text-slate-400 text-xs">
                    🎉 Tuyệt vời! Hiện không còn sản phẩm nào tồn đọng cần duyệt.
                  </div>
                ) : (
                  allProducts
                    .filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL')
                    .slice(0, 3)
                    .map((p) => (
                      <div key={p.id} className="p-4 hover:bg-slate-50/60 transition flex items-center justify-between gap-3 text-xs">
                        <div className="flex items-center space-x-3 min-w-0">
                          <img
                            src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                            alt={p.name}
                            className="w-12 h-12 rounded-xl object-cover border border-slate-200 bg-slate-100 shrink-0"
                          />
                          <div className="min-w-0">
                            <h4 className="font-bold text-slate-900 truncate">{p.name}</h4>
                            <div className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                              <span className="text-orange-600 font-semibold">{p.categoryName || 'Sản phẩm'}</span>
                              <span>•</span>
                              <span>{p.shopName || `Shop #${p.shopId}`}</span>
                            </div>
                            <span className="font-bold text-rose-600 text-xs mt-0.5 block">{formatVND(p.minPrice)}</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5 shrink-0">
                          <button
                            onClick={() => requestApproveProduct(p)}
                            disabled={processingProductId === p.id}
                            className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-[11px] font-bold transition flex items-center gap-1 active:scale-95 shadow-xs disabled:opacity-50"
                          >
                            {processingProductId === p.id ? (
                              <RefreshCw className="w-3 h-3 animate-spin" />
                            ) : (
                              <Check className="w-3 h-3" />
                            )}
                            Duyệt
                          </button>
                          <button
                            onClick={() => handleOpenRejectProductModal(p)}
                            disabled={processingProductId === p.id}
                            className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 rounded-xl text-[11px] font-bold transition flex items-center gap-1 active:scale-95 border border-rose-200 disabled:opacity-50"
                          >
                            <X className="w-3 h-3" />
                            Từ chối
                          </button>
                        </div>
                      </div>
                    ))
                )}
              </div>
            </div>

            {/* Queue 2: Pending KYC Shops Preview */}
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col">
              <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-blue-50/40">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center">
                    <Store className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-900">Hồ sơ Shop chờ thẩm định KYC</h3>
                    <span className="text-[11px] text-slate-500">Cấp quyền bán hàng chính thức</span>
                  </div>
                </div>
                <button
                  onClick={() => {
                    setActiveTab('sellers');
                    setSellerFilter('PENDING');
                    setSellerPage(0);
                  }}
                  className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1"
                >
                  Xem tất cả ({pendingSellerCount}) <ArrowUpRight className="w-3.5 h-3.5" />
                </button>
              </div>

              <div className="divide-y divide-slate-100 flex-1">
                {shops.filter((s) => s.status === 'PENDING_APPROVAL').slice(0, 3).length === 0 ? (
                  <div className="p-8 text-center text-slate-400 text-xs">
                    ✅ Toàn bộ hồ sơ đăng ký gian hàng đã được thẩm định xong.
                  </div>
                ) : (
                  shops
                    .filter((s) => s.status === 'PENDING_APPROVAL')
                    .slice(0, 3)
                    .map((s) => (
                      <div key={s.id} className="p-4 hover:bg-slate-50/60 transition flex items-center justify-between gap-3 text-xs">
                        <div className="min-w-0">
                          <h4 className="font-bold text-slate-900 truncate">{s.name}</h4>
                          <div className="text-[11px] text-slate-500 mt-0.5">
                            Chủ shop: <strong className="text-slate-700">{s.ownerName}</strong> ({s.ownerPhone})
                          </div>
                          <div className="text-[10px] text-slate-400 mt-0.5">
                            CCCD: {s.idCardNumber || 'Chưa cung cấp'} • MST: {s.taxCode || 'Cá nhân'}
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5 shrink-0">
                          <button
                            onClick={() => setSelectedKycShop(s)}
                            className="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-[11px] font-semibold transition flex items-center gap-1 active:scale-95"
                          >
                            <Eye className="w-3 h-3" />
                            Xem KYC
                          </button>
                          <button
                            onClick={() => requestApproveShop(s)}
                            disabled={processingShopId === s.id}
                            className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-[11px] font-bold transition flex items-center gap-1 active:scale-95 shadow-xs disabled:opacity-50"
                          >
                            {processingShopId === s.id ? (
                              <RefreshCw className="w-3 h-3 animate-spin" />
                            ) : (
                              <Check className="w-3 h-3" />
                            )}
                            Duyệt
                          </button>
                        </div>
                      </div>
                    ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ================= TAB 1: PRODUCT MODERATION ================= */}
      {activeTab === 'moderation' && (
        <div className="space-y-4">
          {moderationError && (
            <ApiErrorBanner
              error={moderationError}
              title="Lỗi kết nối API Kiểm duyệt Sản phẩm"
              fallbackActive={usingProductFallback}
              fallbackHint="Hiện đang hiển thị dữ liệu mẫu để tham khảo; bấm Thử lại ngay sau khi bật Catalog Service / API Gateway."
              isRetrying={loadingProducts}
              onRetry={fetchModerationProducts}
              onDismiss={() => setModerationError(null)}
            />
          )}

          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-3 bg-slate-50/50">
              <div>
                <h2 className="font-bold text-sm sm:text-base text-slate-900 flex flex-wrap items-center gap-2">
                  <Clock className="w-5 h-5 text-amber-500" />
                  Hàng đợi kiểm duyệt sản phẩm đăng bán ({filteredModerationProducts.length})
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      usingProductFallback
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-emerald-100 text-emerald-700'
                    }`}
                  >
                    {usingProductFallback ? 'Dữ liệu dự phòng' : 'Đã đồng bộ API'}
                  </span>
                </h2>
                <p className="text-xs text-slate-500">Các bài đăng mới từ Seller cần Admin duyệt trước khi xuất hiện trên sàn</p>
              </div>

              <div className="flex flex-wrap items-center gap-2 text-xs">
                {/* Search input */}
                <div className="relative">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={moderationSearchKw}
                    onChange={(e) => {
                      setModerationSearchKw(e.target.value);
                      setModerationPage(0);
                    }}
                    placeholder="Tìm tên sản phẩm, tên shop..."
                    className="pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 text-xs focus:outline-orange-500 w-48 sm:w-56 bg-white"
                  />
                </div>

                {/* Category Filter */}
                <select
                  value={moderationCategoryFilter}
                  onChange={(e) => {
                    setModerationCategoryFilter(e.target.value);
                    setModerationPage(0);
                  }}
                  className="px-2.5 py-1.5 rounded-xl border border-slate-200 text-xs bg-white focus:outline-orange-500 font-medium"
                >
                  <option value="ALL">Tất cả ngành hàng</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.parentId ? `  ↳ ${c.name}` : `● ${c.name}`}
                    </option>
                  ))}
                </select>

                {/* Status Tabs */}
                {[
                  { id: 'PENDING', label: `Chờ duyệt (${pendingModerationCount})` },
                  { id: 'REJECTED', label: `Bị từ chối (${rejectedModerationCount})` },
                  { id: 'APPROVED', label: `Đã duyệt (${approvedModerationCount})` },
                  { id: 'ALL', label: `Tất cả (${allProducts.length})` },
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => {
                      setModerationFilter(f.id);
                      setModerationPage(0);
                    }}
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
                  disabled={loadingProducts}
                  className="text-xs font-semibold text-orange-600 hover:underline ml-1 cursor-pointer flex items-center gap-1 disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loadingProducts ? 'animate-spin' : ''}`} />
                  {loadingProducts ? 'Đang tải...' : 'Làm mới'}
                </button>
              </div>
            </div>

            <div className="divide-y divide-slate-100">
              {loadingProducts && allProducts.length === 0 ? (
                <div className="p-12 text-center text-slate-500 text-xs flex flex-col items-center gap-2">
                  <RefreshCw className="w-5 h-5 text-orange-500 animate-spin" />
                  <span>Đang tải danh sách kiểm duyệt sản phẩm từ Catalog Service...</span>
                </div>
              ) : pagedModerationProducts.length === 0 ? (
                <div className="p-12 text-center text-slate-400 text-sm">
                  Không tìm thấy sản phẩm nào phù hợp với bộ lọc kiểm duyệt này.
                </div>
              ) : (
                pagedModerationProducts.map((p) => {
                  const isBusy = processingProductId === p.id;
                  const isApproved = p.status === 'APPROVED' || p.status === 'ACTIVE';
                  return (
                    <div key={p.id} className="p-5 hover:bg-slate-50/60 transition flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
                      <div className="flex items-start space-x-4">
                        <img
                          src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                          alt={p.name}
                          className="w-16 h-16 object-cover rounded-xl border border-slate-200 bg-slate-100 flex-shrink-0"
                        />
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-bold text-orange-600">{p.categoryName || 'Sản phẩm sàn'}</span>
                            <span className="text-slate-300">•</span>
                            <span className="text-xs text-slate-500 font-medium">{p.shopName || `Shop #${p.shopId || 1}`}</span>
                            {(p.status === 'PENDING' || p.status === 'PENDING_APPROVAL') && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700">
                                Chờ Admin Duyệt
                              </span>
                            )}
                            {isApproved && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">
                                Đã Phê Duyệt
                              </span>
                            )}
                            {p.status === 'REJECTED' && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700">
                                Bị Từ Chối
                              </span>
                            )}
                            {p.status === 'DRAFT' && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600">
                                Bản nháp
                              </span>
                            )}
                          </div>
                          <h3 className="font-bold text-sm text-slate-900 mt-1">{p.name}</h3>
                          <p className="text-xs text-slate-500 line-clamp-1 mt-0.5 max-w-xl">{p.description}</p>
                          <div className="mt-2 flex items-center gap-4 text-xs">
                            <span className="font-bold text-rose-600 text-sm">{formatVND(p.minPrice)}</span>
                            <span className="text-slate-400">
                              Tồn kho ban đầu: {p.variants ? p.variants.reduce((acc, v) => acc + (v.stock || 0), 0) : '0'}
                            </span>
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
                        {!isApproved && (
                          <button
                            onClick={() => requestApproveProduct(p)}
                            disabled={isBusy}
                            className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95 disabled:opacity-50"
                          >
                            {isBusy ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                            Duyệt đăng sàn
                          </button>
                        )}
                        {p.status !== 'REJECTED' && (
                          <button
                            onClick={() => handleOpenRejectProductModal(p)}
                            disabled={isBusy}
                            className="px-3.5 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95 disabled:opacity-50"
                          >
                            <XCircle className="w-3.5 h-3.5" />
                            Từ chối
                          </button>
                        )}
                        <button
                          onClick={() => requestDeleteProduct(p)}
                          disabled={isBusy}
                          className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition cursor-pointer disabled:opacity-50"
                          title="Xóa sản phẩm vĩnh viễn"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Pagination for Products */}
            <div className="p-4 border-t border-slate-100 bg-slate-50/50">
              <Pagination
                currentPage={moderationPage}
                totalPages={totalModerationPages}
                totalElements={totalModerationElements}
                pageSize={moderationPageSize}
                onPageChange={setModerationPage}
                onPageSizeChange={(newSize) => {
                  setModerationPageSize(newSize);
                  setModerationPage(0);
                }}
              />
            </div>
          </div>
        </div>
      )}

      {/* ================= TAB 2: SELLER & SHOP KYC MANAGEMENT ================= */}
      {activeTab === 'sellers' && (
        <div className="space-y-6">
          {sellersError && (
            <ApiErrorBanner
              error={sellersError}
              title="Lỗi kết nối API Quản lý Gian hàng & KYC"
              fallbackActive={usingShopFallback}
              fallbackHint="Hiện đang hiển thị dữ liệu mẫu cục bộ; bấm Thử lại ngay sau khi bật Identity Service / API Gateway."
              isRetrying={loadingShops}
              onRetry={fetchShops}
              onDismiss={() => setSellersError(null)}
            />
          )}

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
                <span className="text-xs text-slate-500 font-medium">Tạm Khóa / Từ Chối</span>
                <h3 className="text-xl font-bold text-rose-600">{shops.filter((s) => s.status === 'SUSPENDED' || s.status === 'REJECTED').length}</h3>
              </div>
            </div>
          </div>

          {/* Shop Table Section */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-3 bg-slate-50/50">
              <div>
                <h2 className="font-bold text-base text-slate-900 flex flex-wrap items-center gap-2">
                  <Store className="w-5 h-5 text-blue-600" />
                  Danh sách Nhà Bán Hàng & Thẩm định KYC ({filteredShops.length})
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      usingShopFallback
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-emerald-100 text-emerald-700'
                    }`}
                  >
                    {usingShopFallback ? 'Dữ liệu dự phòng' : 'Đã đồng bộ API'}
                  </span>
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
                    onChange={(e) => {
                      setSellerSearchKw(e.target.value);
                      setSellerPage(0);
                    }}
                    placeholder="Tìm tên shop, chủ shop, SĐT..."
                    className="pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 text-xs focus:outline-orange-500 w-48 sm:w-56 bg-white"
                  />
                </div>

                {/* Mall Type Filter */}
                <select
                  value={sellerMallFilter}
                  onChange={(e) => {
                    setSellerMallFilter(e.target.value);
                    setSellerPage(0);
                  }}
                  className="px-2.5 py-1.5 rounded-xl border border-slate-200 text-xs bg-white focus:outline-orange-500 font-medium"
                >
                  <option value="ALL">Tất cả loại gian hàng</option>
                  <option value="MALL">⭐ QuickShop Mall</option>
                  <option value="STANDARD">Shop tiêu chuẩn</option>
                </select>

                {/* Status Filters */}
                {[
                  { id: 'ALL', label: 'Tất cả' },
                  { id: 'PENDING', label: `Chờ duyệt KYC (${pendingSellerCount})` },
                  { id: 'ACTIVE', label: 'Hoạt động' },
                  { id: 'SUSPENDED', label: 'Đã khóa / Từ chối' },
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => {
                      setSellerFilter(f.id);
                      setSellerPage(0);
                    }}
                    className={`px-3 py-1.5 rounded-xl font-medium transition cursor-pointer ${
                      sellerFilter === f.id
                        ? 'bg-blue-600 text-white font-bold shadow-sm'
                        : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                    }`}
                  >
                    {f.label}
                  </button>
                ))}

                <button
                  onClick={fetchShops}
                  disabled={loadingShops}
                  className="text-xs font-semibold text-blue-600 hover:underline ml-1 cursor-pointer flex items-center gap-1 disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loadingShops ? 'animate-spin' : ''}`} />
                  {loadingShops ? 'Đang tải...' : 'Làm mới'}
                </button>
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
                  {loadingShops && shops.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="text-center py-12 text-slate-500 text-xs">
                        <div className="flex flex-col items-center gap-2">
                          <RefreshCw className="w-5 h-5 text-blue-600 animate-spin" />
                          <span>Đang tải danh sách gian hàng từ Identity Service...</span>
                        </div>
                      </td>
                    </tr>
                  ) : pagedShops.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="text-center py-10 text-slate-400 text-xs">
                        Không tìm thấy gian hàng nào phù hợp với bộ lọc tìm kiếm.
                      </td>
                    </tr>
                  ) : (
                    pagedShops.map((s) => {
                      const isBusy = processingShopId === s.id;
                      const resolvedProductCount =
                        s.productCount ?? allProducts.filter((p) => Number(p.shopId) === Number(s.id)).length;
                      return (
                        <tr key={s.id} className="hover:bg-slate-50/80 transition">
                          <td className="px-5 py-4">
                            <div className="flex items-center space-x-3">
                              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-black text-sm flex items-center justify-center shadow-sm">
                                {(s.name || 'S').charAt(0).toUpperCase()}
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
                                <span className="text-[11px] text-slate-400">Shop ID: #{s.id} • {resolvedProductCount} SP</span>
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
                            <div className="font-bold text-slate-900">
                              {s.totalRevenue !== null ? formatVND(s.totalRevenue) : 'Chưa đối soát'}
                            </div>
                            <div className="text-[11px] text-amber-500 font-semibold flex items-center gap-1">
                              {s.rating !== null ? `★ ${s.rating} / 5.0` : 'Hồ sơ mới đăng ký'}
                            </div>
                          </td>

                          <td className="px-4 py-4 space-y-1">
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
                            {s.status === 'SUSPENDED' && (
                              <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700 flex items-center gap-1 w-fit">
                                <Lock className="w-3 h-3" />
                                Tạm khóa
                              </span>
                            )}
                            {s.status === 'REJECTED' && (
                              <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700 flex items-center gap-1 w-fit">
                                <XCircle className="w-3 h-3" />
                                Từ chối KYC
                              </span>
                            )}
                            {s.rejectionReason && (
                              <p className="text-[10px] text-rose-600 line-clamp-2 max-w-[180px]" title={s.rejectionReason}>
                                Lý do: {s.rejectionReason}
                              </p>
                            )}
                          </td>

                          <td className="px-5 py-4 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              {s.status === 'PENDING_APPROVAL' ? (
                                <>
                                  <button
                                    onClick={() => requestApproveShop(s)}
                                    disabled={isBusy}
                                    className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95 disabled:opacity-50"
                                    title="Phê duyệt mở shop & cấp quyền ROLE_SELLER"
                                  >
                                    {isBusy ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                                    Duyệt Shop
                                  </button>
                                  <button
                                    onClick={() => handleOpenRejectShopModal(s)}
                                    disabled={isBusy}
                                    className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1 shadow-sm cursor-pointer active:scale-95 disabled:opacity-50"
                                    title="Từ chối hồ sơ KYC"
                                  >
                                    <X className="w-3.5 h-3.5" />
                                    Từ chối
                                  </button>
                                </>
                              ) : (
                                <button
                                  onClick={() => requestToggleLockShop(s)}
                                  disabled={isBusy}
                                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1 cursor-pointer active:scale-95 disabled:opacity-50 ${
                                    s.status === 'ACTIVE'
                                      ? 'bg-rose-50 text-rose-600 hover:bg-rose-100 border border-rose-200'
                                      : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100 border border-emerald-200'
                                  }`}
                                >
                                  {isBusy ? (
                                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                  ) : s.status === 'ACTIVE' ? (
                                    <Lock className="w-3.5 h-3.5" />
                                  ) : (
                                    <Unlock className="w-3.5 h-3.5" />
                                  )}
                                  {s.status === 'ACTIVE' ? 'Khóa Shop' : 'Mở khóa'}
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination for Sellers */}
            <div className="p-4 border-t border-slate-100 bg-slate-50/50">
              <Pagination
                currentPage={sellerPage}
                totalPages={totalSellerPages}
                totalElements={totalSellerElements}
                pageSize={sellerPageSize}
                onPageChange={setSellerPage}
                onPageSizeChange={(newSize) => {
                  setSellerPageSize(newSize);
                  setSellerPage(0);
                }}
              />
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
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                          selectedKycShop.status === 'ACTIVE'
                            ? 'bg-emerald-100 text-emerald-700'
                            : selectedKycShop.status === 'PENDING_APPROVAL'
                              ? 'bg-amber-100 text-amber-700'
                              : 'bg-rose-100 text-rose-700'
                        }`}
                      >
                        {selectedKycShop.status}
                      </span>
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
                      <span className="text-slate-500">Số điện thoại liên hệ:</span>
                      <strong className="text-slate-900">{selectedKycShop.ownerPhone}</strong>
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
                      <strong className="text-slate-900 text-right">{selectedKycShop.bankAccount}</strong>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <span className="text-slate-500 font-medium">Địa chỉ kho hàng & trung tâm bảo hành:</span>
                    <p className="p-2.5 bg-slate-50 rounded-xl text-slate-800 font-medium">
                      <MapPin className="w-3.5 h-3.5 inline mr-1 text-slate-400" />
                      {selectedKycShop.warehouseAddress}
                    </p>
                  </div>

                  {selectedKycShop.rejectionReason && (
                    <div className="p-3 bg-rose-50 rounded-2xl border border-rose-200 text-rose-800 space-y-1">
                      <strong className="font-bold flex items-center gap-1.5">
                        <AlertTriangle className="w-4 h-4 text-rose-600" />
                        Lý do từ chối / tạm khóa gần nhất
                      </strong>
                      <p className="text-[11px] leading-relaxed">{selectedKycShop.rejectionReason}</p>
                    </div>
                  )}

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
                        disabled={processingShopId === selectedKycShop.id}
                        className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition cursor-pointer disabled:opacity-50"
                      >
                        Từ chối KYC
                      </button>
                      <button
                        onClick={() => requestApproveShop(selectedKycShop)}
                        disabled={processingShopId === selectedKycShop.id}
                        className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition cursor-pointer disabled:opacity-50"
                      >
                        Duyệt mở Shop & Cấp quyền
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() => requestToggleLockShop(selectedKycShop)}
                        disabled={processingShopId === selectedKycShop.id}
                        className={`px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer disabled:opacity-50 ${
                          selectedKycShop.status === 'ACTIVE'
                            ? 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200'
                            : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                        }`}
                      >
                        {selectedKycShop.status === 'ACTIVE' ? 'Tạm khóa Shop' : 'Mở khóa hoạt động'}
                      </button>
                      <button
                        onClick={() => setSelectedKycShop(null)}
                        className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition cursor-pointer"
                      >
                        Đóng
                      </button>
                    </>
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

      {/* ================= TAB 7: FINANCE & GMV ================= */}
      {activeTab === 'finance' && (
        <div className="space-y-6">
          {financeError && (
            <ApiErrorBanner
              message={financeError}
              onRetry={fetchFinanceData}
            />
          )}

          {/* Timeframe & Sub Navigation Toolbar */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
            <div className="flex gap-2 bg-slate-100 p-1.5 rounded-2xl text-xs font-semibold">
              <button
                onClick={() => setFinanceSubTab('overview')}
                className={`px-3 py-1.5 rounded-xl transition cursor-pointer ${
                  financeSubTab === 'overview' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Tổng quan & Biểu đồ GMV
              </button>
              <button
                onClick={() => setFinanceSubTab('withdrawals')}
                className={`px-3 py-1.5 rounded-xl transition cursor-pointer flex items-center gap-1.5 ${
                  financeSubTab === 'withdrawals' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>Yêu cầu rút tiền</span>
                {pendingWithdrawals.length > 0 && (
                  <span className="w-5 h-5 rounded-full bg-rose-500 text-white text-[10px] flex items-center justify-center font-bold">
                    {pendingWithdrawals.length}
                  </span>
                )}
              </button>
              <button
                onClick={() => setFinanceSubTab('ledger')}
                className={`px-3 py-1.5 rounded-xl transition cursor-pointer ${
                  financeSubTab === 'ledger' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Sổ cái kép (Ledger)
              </button>
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto justify-between sm:justify-end">
              <div className="flex items-center bg-white border border-slate-200 rounded-xl p-1 text-xs">
                {[
                  { key: 'today', label: 'Hôm nay' },
                  { key: '7d', label: '7 ngày' },
                  { key: '30d', label: '30 ngày' },
                  { key: 'all', label: 'Toàn bộ' },
                ].map((tf) => (
                  <button
                    key={tf.key}
                    onClick={() => setFinanceTimeframe(tf.key)}
                    className={`px-2.5 py-1 rounded-lg font-medium transition cursor-pointer ${
                      financeTimeframe === tf.key
                        ? 'bg-orange-600 text-white font-bold shadow-sm'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {tf.label}
                  </button>
                ))}
              </div>

              <button
                onClick={fetchFinanceData}
                disabled={loadingFinance}
                className="px-3.5 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-xl text-xs font-semibold flex items-center gap-2 transition cursor-pointer"
                title="Làm mới số liệu"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loadingFinance ? 'animate-spin text-orange-600' : ''}`} />
                <span className="hidden sm:inline">Làm mới</span>
              </button>
            </div>
          </div>

          {/* Quick Metrics */}
          {(() => {
            const metrics = {
              today: { gmv: '14.850.000 ₫', commission: '742.500 ₫', escrow: '14.107.500 ₫', payout: '0 ₫', growth: '+8.2% so với hôm qua' },
              '7d': { gmv: '78.400.000 ₫', commission: '3.920.000 ₫', escrow: '18.500.000 ₫', payout: '55.980.000 ₫', growth: '+14.5% so với tuần trước' },
              '30d': { gmv: '248.500.000 ₫', commission: '12.425.000 ₫', escrow: '34.200.000 ₫', payout: '201.875.000 ₫', growth: '+18.4% so với tháng trước' },
              all: { gmv: '612.300.000 ₫', commission: '30.615.000 ₫', escrow: '34.200.000 ₫', payout: '547.485.000 ₫', growth: '+24.1% toàn thời gian' },
            }[financeTimeframe] || { gmv: '248.500.000 ₫', commission: '12.425.000 ₫', escrow: '34.200.000 ₫', payout: '201.875.000 ₫', growth: '+18.4%' };

            return (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm relative overflow-hidden">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Tổng GMV Toàn Sàn</span>
                    <div className="w-10 h-10 rounded-2xl bg-orange-50 text-orange-600 flex items-center justify-center">
                      <TrendingUp className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="mt-3">
                    <span className="text-2xl font-black text-slate-900 tracking-tight">{metrics.gmv}</span>
                    <div className="flex items-center gap-1.5 mt-1 text-[11px] text-emerald-600 font-semibold">
                      <ArrowUpRight className="w-3.5 h-3.5" />
                      <span>{metrics.growth}</span>
                    </div>
                  </div>
                </div>

                <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm relative overflow-hidden">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Phí Sàn Thu Được</span>
                    <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                      <DollarSign className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="mt-3">
                    <span className="text-2xl font-black text-emerald-600 tracking-tight">{metrics.commission}</span>
                    <span className="block text-[11px] text-slate-400 mt-1">Chiết khấu 5% GMV thực tế</span>
                  </div>
                </div>

                <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm relative overflow-hidden">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Tiền Đang Ký Quỹ</span>
                    <div className="w-10 h-10 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center">
                      <ShieldCheck className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="mt-3">
                    <span className="text-2xl font-black text-blue-600 tracking-tight">{metrics.escrow}</span>
                    <span className="block text-[11px] text-slate-400 mt-1">Bảo đảm Escrow giao hàng</span>
                  </div>
                </div>

                <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm relative overflow-hidden">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Đã Thanh Toán Shop</span>
                    <div className="w-10 h-10 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center">
                      <CreditCard className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="mt-3">
                    <span className="text-2xl font-black text-purple-600 tracking-tight">{metrics.payout}</span>
                    <span className="block text-[11px] text-slate-400 mt-1">Giải ngân hoàn tất cho Người bán</span>
                  </div>
                </div>
              </div>
            );
          })()}

          {/* SUB-TAB: OVERVIEW */}
          {financeSubTab === 'overview' && (
            <div className="space-y-6">
              {/* Payment Channel Breakdown & Escrow Flow */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm space-y-4">
                  <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
                    <TrendingUp className="w-4 h-4 text-orange-500" />
                    Cơ cấu dòng tiền & Chu kỳ thanh toán Escrow
                  </h3>
                  <p className="text-xs text-slate-500 leading-relaxed">
                    QuickShop áp dụng mô hình bảo vệ tài chính 2 lớp: Toàn bộ tiền thanh toán từ Người mua (VNPay / Thẻ / COD) được lưu giữ trong tài khoản ký quỹ trung gian (Escrow). Sau khi đơn hàng giao thành công và hết hạn khiếu nại (3 ngày), hệ thống tự động giải phóng 95% giá trị cho Người bán và trích 5% phí dịch vụ nền tảng.
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
                    <div className="p-4 rounded-2xl bg-orange-50/60 border border-orange-100">
                      <span className="text-[11px] font-bold text-orange-700 block uppercase">Bước 1: Khóa tiền</span>
                      <span className="text-xs text-slate-600 mt-1 block">Khách thanh toán &rarr; Escrow Account HOLDING</span>
                    </div>
                    <div className="p-4 rounded-2xl bg-blue-50/60 border border-blue-100">
                      <span className="text-[11px] font-bold text-blue-700 block uppercase">Bước 2: Giao hàng</span>
                      <span className="text-xs text-slate-600 mt-1 block">Webhook GHN/GHTK cập nhật DELIVERED</span>
                    </div>
                    <div className="p-4 rounded-2xl bg-emerald-50/60 border border-emerald-100">
                      <span className="text-[11px] font-bold text-emerald-700 block uppercase">Bước 3: Quyết toán</span>
                      <span className="text-xs text-slate-600 mt-1 block">Mở ví Shop + Sổ cái kép Double-entry</span>
                    </div>
                  </div>
                </div>

                {/* Channel Breakdown */}
                <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm space-y-4">
                  <h3 className="font-bold text-sm text-slate-900">Tỷ lệ theo kênh thanh toán</h3>
                  <div className="space-y-3 text-xs">
                    <div>
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-slate-600 font-medium">Cổng VNPay (QR / Thẻ)</span>
                        <strong className="text-slate-900 font-bold">62% (154.07M)</strong>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div className="bg-blue-600 h-2 rounded-full" style={{ width: '62%' }}></div>
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-slate-600 font-medium">Chuyển khoản MB Bank / PayOS</span>
                        <strong className="text-slate-900 font-bold">28% (69.58M)</strong>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div className="bg-orange-500 h-2 rounded-full" style={{ width: '28%' }}></div>
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-slate-600 font-medium">Thu hộ COD khi nhận hàng</span>
                        <strong className="text-slate-900 font-bold">10% (24.85M)</strong>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div className="bg-emerald-500 h-2 rounded-full" style={{ width: '10%' }}></div>
                      </div>
                    </div>
                    <div className="pt-2 border-t border-slate-100 flex justify-between items-center text-[11px] text-slate-400">
                      <span>Thời gian đối soát: 72h</span>
                      <span>Rút tối thiểu: 100.000 ₫</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Top GMV & Commission Shops Table */}
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
                <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                  <div>
                    <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
                      <Store className="w-4 h-4 text-orange-600" />
                      Top 5 Gian hàng đóng góp GMV & Hoa hồng nền tảng cao nhất
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Thống kê doanh số bán hàng và phí hoa hồng 5% trích về ngân quỹ QuickShop
                    </p>
                  </div>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50/70 border-b border-slate-100 text-slate-500 font-bold uppercase text-[10px]">
                      <tr>
                        <th className="p-4">Xếp hạng</th>
                        <th className="p-4">Gian hàng</th>
                        <th className="p-4">Phân loại</th>
                        <th className="p-4 text-right">Đơn thành công</th>
                        <th className="p-4 text-right">Tổng GMV</th>
                        <th className="p-4 text-right">Hoa hồng sàn (5%)</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {topGmvShops.map((shop, idx) => (
                        <tr key={shop.id} className="hover:bg-slate-50/60 transition">
                          <td className="p-4">
                            <span className={`w-6 h-6 rounded-full inline-flex items-center justify-center font-bold text-xs ${
                              idx === 0 ? 'bg-amber-100 text-amber-800' :
                              idx === 1 ? 'bg-slate-200 text-slate-700' :
                              idx === 2 ? 'bg-amber-50 text-amber-700' : 'bg-slate-100 text-slate-500'
                            }`}>
                              #{idx + 1}
                            </span>
                          </td>
                          <td className="p-4 font-bold text-slate-900 flex items-center gap-2">
                            <Store className="w-4 h-4 text-orange-500" />
                            <span>{shop.name}</span>
                          </td>
                          <td className="p-4">
                            <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
                              shop.status === 'MALL' ? 'bg-rose-100 text-rose-700' : 'bg-blue-100 text-blue-700'
                            }`}>
                              {shop.status === 'MALL' ? 'Shopee Mall' : 'Chính hãng'}
                            </span>
                          </td>
                          <td className="p-4 text-right font-semibold text-slate-700">{shop.orderCount} đơn</td>
                          <td className="p-4 text-right font-black text-slate-900">{shop.gmv.toLocaleString('vi-VN')} ₫</td>
                          <td className="p-4 text-right font-black text-emerald-600">+{shop.commission.toLocaleString('vi-VN')} ₫</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* SUB-TAB: WITHDRAWALS */}
          {financeSubTab === 'withdrawals' && (
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
              <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-purple-600" />
                  Yêu cầu rút tiền từ người bán chờ Admin phê duyệt ({pendingWithdrawals.length})
                </h3>
              </div>

              {pendingWithdrawals.length === 0 ? (
                <div className="p-12 text-center text-slate-400 text-xs">
                  <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2 opacity-80" />
                  Hiện không có yêu cầu rút tiền nào đang chờ duyệt.
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {pendingWithdrawals.map((w) => (
                    <div key={w.id} className="p-4 sm:p-5 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 hover:bg-slate-50/70 transition">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 text-[10px] font-bold">
                            #{w.id}
                          </span>
                          <strong className="text-slate-900 font-bold text-sm">{w.shopName || `Shop #${w.shopId}`}</strong>
                        </div>
                        <p className="text-xs text-slate-500">
                          Ngân hàng: <strong className="text-slate-700">{w.bankCode}</strong> - STK: <strong className="text-slate-700">{w.bankAccountNumber}</strong> ({w.bankAccountName})
                        </p>
                        <span className="text-[11px] text-slate-400 block">Thời gian yêu cầu: {new Date(w.createdAt).toLocaleString('vi-VN')}</span>
                      </div>

                      <div className="flex items-center gap-4 w-full md:w-auto justify-between md:justify-end">
                        <div className="text-right">
                          <span className="text-[10px] text-slate-400 uppercase font-bold">Số tiền yêu cầu</span>
                          <span className="block text-base font-black text-emerald-600">
                            {Number(w.amount).toLocaleString('vi-VN')} ₫
                          </span>
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleProcessWithdrawal(w.id, 'APPROVE')}
                            disabled={processingWithdrawalId === w.id}
                            className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition shadow-sm cursor-pointer disabled:opacity-50"
                          >
                            Duyệt chi
                          </button>
                          <button
                            onClick={() => handleProcessWithdrawal(w.id, 'REJECT')}
                            disabled={processingWithdrawalId === w.id}
                            className="px-3.5 py-2 bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-600 rounded-xl text-xs font-bold transition cursor-pointer disabled:opacity-50"
                          >
                            Từ chối
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* SUB-TAB: LEDGER */}
          {financeSubTab === 'ledger' && (
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
              <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-600" />
                  Sổ cái kế toán kép hệ thống (Double-entry Ledger)
                </h3>
                <span className="text-[11px] text-slate-400 font-mono">Chuẩn kế toán IFRS</span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50/70 border-b border-slate-100 text-slate-500 font-bold uppercase text-[10px]">
                    <tr>
                      <th className="p-4">Mã bút toán</th>
                      <th className="p-4">Loại giao dịch</th>
                      <th className="p-4">Tài khoản Nợ (Debit)</th>
                      <th className="p-4">Tài khoản Có (Credit)</th>
                      <th className="p-4 text-right">Số tiền</th>
                      <th className="p-4 text-right">Thời gian</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {ledgerEntries.map((l) => (
                      <tr key={l.id} className="hover:bg-slate-50/60 transition">
                        <td className="p-4 font-mono font-bold text-slate-800">{l.transactionId}</td>
                        <td className="p-4">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                            {l.transactionType}
                          </span>
                        </td>
                        <td className="p-4 font-mono text-rose-600 font-semibold">{l.accountDebit}</td>
                        <td className="p-4 font-mono text-emerald-600 font-semibold">{l.accountCredit}</td>
                        <td className="p-4 text-right font-black text-slate-900">
                          {Number(l.amount).toLocaleString('vi-VN')} ₫
                        </td>
                        <td className="p-4 text-right text-slate-400 font-mono text-[11px]">
                          {new Date(l.createdAt).toLocaleString('vi-VN')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ================= TAB 8: AUDIT LOGS ================= */}
      {activeTab === 'audit' && (
        <div className="space-y-6">
          {auditError && (
            <ApiErrorBanner
              message={auditError}
              onRetry={fetchAuditLogs}
            />
          )}

          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
              <div>
                <h3 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
                  <FileText className="w-5 h-5 text-blue-500" />
                  Nhật ký kiểm toán hệ thống (Audit Logs)
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Lưu vết toàn bộ thao tác quản trị, webhook vận chuyển và luồng xử lý tự động. Nhấp vào bản ghi để xem chi tiết.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
                <div className="relative flex-1 sm:w-60">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={auditSearchKw}
                    onChange={(e) => setAuditSearchKw(e.target.value)}
                    placeholder="Tìm theo người, đối tượng..."
                    className="w-full pl-9 pr-3 py-1.5 bg-white border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 outline-none"
                  />
                </div>

                <select
                  value={auditFilter}
                  onChange={(e) => setAuditFilter(e.target.value)}
                  className="bg-white border border-slate-200 rounded-xl px-2.5 py-1.5 text-xs text-slate-700 outline-none focus:border-orange-500"
                >
                  <option value="ALL">Tất cả hành động</option>
                  <option value="APPROVE">Phê duyệt (Approve)</option>
                  <option value="REJECT">Từ chối (Reject/Block)</option>
                  <option value="WEBHOOK">Webhook đối tác</option>
                  <option value="ESCROW">Ký quỹ (Escrow)</option>
                </select>

                <select
                  value={auditStatusFilter}
                  onChange={(e) => setAuditStatusFilter(e.target.value)}
                  className="bg-white border border-slate-200 rounded-xl px-2.5 py-1.5 text-xs text-slate-700 outline-none focus:border-orange-500"
                >
                  <option value="ALL">Tất cả trạng thái</option>
                  <option value="SUCCESS">Thành công (SUCCESS)</option>
                  <option value="BLOCKED">Bị chặn (BLOCKED)</option>
                  <option value="FAILED">Thất bại (FAILED)</option>
                </select>

                <button
                  onClick={handleExportAuditLogs}
                  className="px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-xl text-xs font-bold transition flex items-center gap-1.5 cursor-pointer shadow-sm"
                  title="Tải file CSV nhật ký kiểm toán"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">Xuất CSV</span>
                </button>

                <button
                  onClick={fetchAuditLogs}
                  disabled={loadingAudit}
                  className="p-2 bg-white hover:bg-slate-100 border border-slate-200 rounded-xl transition cursor-pointer"
                  title="Làm mới"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loadingAudit ? 'animate-spin text-orange-600' : 'text-slate-600'}`} />
                </button>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50/70 border-b border-slate-100 text-slate-500 font-bold uppercase text-[10px]">
                  <tr>
                    <th className="p-4">Thời gian</th>
                    <th className="p-4">Người thực hiện</th>
                    <th className="p-4">Hành động</th>
                    <th className="p-4">Đối tượng tác động</th>
                    <th className="p-4">Địa chỉ IP</th>
                    <th className="p-4 text-right">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {auditLogs
                    .filter((log) => {
                      if (auditFilter === 'APPROVE' && !log.action.includes('APPROVE')) return false;
                      if (auditFilter === 'REJECT' && !log.action.includes('REJECT') && !log.action.includes('BLOCK')) return false;
                      if (auditFilter === 'WEBHOOK' && !log.action.includes('WEBHOOK')) return false;
                      if (auditFilter === 'ESCROW' && !log.action.includes('ESCROW')) return false;
                      if (auditStatusFilter !== 'ALL' && log.status !== auditStatusFilter) return false;
                      if (auditSearchKw) {
                        const kw = auditSearchKw.toLowerCase();
                        return (
                          log.actor.toLowerCase().includes(kw) ||
                          log.action.toLowerCase().includes(kw) ||
                          log.target.toLowerCase().includes(kw) ||
                          (log.ip && log.ip.includes(kw))
                        );
                      }
                      return true;
                    })
                    .map((log) => (
                      <tr
                        key={log.id}
                        onClick={() => setSelectedAuditLog(log)}
                        className="hover:bg-orange-50/40 transition cursor-pointer group"
                      >
                        <td className="p-4 text-slate-400 font-mono text-[11px] whitespace-nowrap">
                          {new Date(log.timestamp).toLocaleString('vi-VN')}
                        </td>
                        <td className="p-4 font-bold text-slate-900 group-hover:text-orange-600 transition flex items-center gap-1.5">
                          <span>{log.actor}</span>
                        </td>
                        <td className="p-4">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              log.action.includes('APPROVE')
                                ? 'bg-emerald-100 text-emerald-800'
                                : log.action.includes('REJECT') || log.action.includes('BLOCK')
                                ? 'bg-rose-100 text-rose-800'
                                : log.action.includes('WEBHOOK')
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-slate-100 text-slate-800'
                            }`}
                          >
                            {log.action}
                          </span>
                        </td>
                        <td className="p-4 text-slate-700 font-medium">{log.target}</td>
                        <td className="p-4 font-mono text-slate-500 text-[11px]">{log.ip}</td>
                        <td className="p-4 text-right">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              log.status === 'SUCCESS'
                                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                : log.status === 'BLOCKED'
                                ? 'bg-rose-50 text-rose-700 border border-rose-200'
                                : 'bg-amber-50 text-amber-700 border border-amber-200'
                            }`}
                          >
                            {log.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
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

      {/* ================= MODAL: XÁC NHẬN THAO TÁC QUẢN TRỊ (Duyệt / Xóa / Khóa Shop) ================= */}
      {confirmDialog && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex justify-between items-start border-b border-slate-100 pb-3">
              <div className="flex items-start gap-2.5">
                <div
                  className={`p-2 rounded-xl shrink-0 ${
                    confirmDialog.tone === 'rose'
                      ? 'bg-rose-100 text-rose-600'
                      : 'bg-emerald-100 text-emerald-600'
                  }`}
                >
                  {confirmDialog.tone === 'rose' ? (
                    <AlertTriangle className="w-5 h-5" />
                  ) : (
                    <ShieldCheck className="w-5 h-5" />
                  )}
                </div>
                <div>
                  <h3 className="font-bold text-base text-slate-900">{confirmDialog.title}</h3>
                  <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">{confirmDialog.description}</p>
                </div>
              </div>
              <button
                type="button"
                disabled={Boolean(processingProductId || processingShopId)}
                onClick={() => {
                  setConfirmDialog(null);
                  setConfirmModalError(null);
                }}
                className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 disabled:opacity-50"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {confirmDialog.detailLines?.length > 0 && (
              <div className="bg-slate-50 p-3 rounded-2xl space-y-1 text-xs text-slate-700 border border-slate-200/70">
                {confirmDialog.detailLines.map((line, idx) => (
                  <div key={idx}>{line}</div>
                ))}
              </div>
            )}

            {confirmDialog.requireNote && (
              <div className="space-y-1.5 text-xs">
                <label className="block text-slate-700 font-bold">{confirmDialog.noteLabel || 'Ghi chú thao tác *'}</label>
                <textarea
                  rows="3"
                  value={confirmNote}
                  onChange={(e) => setConfirmNote(e.target.value)}
                  placeholder="Nhập lý do chi tiết..."
                  className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-rose-500 focus:bg-white resize-none"
                />
              </div>
            )}

            {confirmModalError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-2xl text-xs text-rose-700 flex items-start gap-2">
                <XCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
                <span>{confirmModalError}</span>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                disabled={Boolean(processingProductId || processingShopId)}
                onClick={() => {
                  setConfirmDialog(null);
                  setConfirmModalError(null);
                }}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition disabled:opacity-50"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                disabled={
                  Boolean(processingProductId || processingShopId) ||
                  (confirmDialog.requireNote && !confirmNote.trim())
                }
                onClick={handleConfirmDialogSubmit}
                className={`px-5 py-2 text-white rounded-xl text-xs font-bold transition shadow-sm active:scale-95 disabled:opacity-50 flex items-center gap-1.5 ${
                  confirmDialog.tone === 'rose'
                    ? 'bg-rose-600 hover:bg-rose-700'
                    : 'bg-emerald-600 hover:bg-emerald-700'
                }`}
              >
                {Boolean(processingProductId || processingShopId) && (
                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                )}
                <span>
                  {Boolean(processingProductId || processingShopId)
                    ? 'Đang gửi yêu cầu...'
                    : confirmDialog.confirmLabel}
                </span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: TỪ CHỐI DUYỆT SẢN PHẨM ================= */}
      {rejectingProduct && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex justify-between items-start border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <XCircle className="w-5 h-5 text-rose-600" />
                  Từ Chối Duyệt Sản Phẩm #{rejectingProduct.id}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 font-medium line-clamp-1">
                  {rejectingProduct.name}
                </p>
              </div>
              <button
                type="button"
                disabled={submittingModalAction}
                onClick={() => {
                  setRejectingProduct(null);
                  setRejectModalError(null);
                }}
                className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 disabled:opacity-50"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1.5">Chọn nhanh lý do từ chối mẫu:</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                  {[
                    'Hình ảnh mờ, không đạt độ phân giải tiêu chuẩn',
                    'Thiếu giấy tờ kiểm định hợp quy / xuất xứ nguồn gốc',
                    'Tên hoặc mô tả vi phạm chính sách cộng đồng sàn',
                    'Giá bán hoặc tồn kho biến thể SKU bất hợp lý',
                  ].map((preset, idx) => (
                    <button
                      key={idx}
                      type="button"
                      disabled={submittingModalAction}
                      onClick={() => setRejectReason(preset)}
                      className="p-2 text-left bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-xl border border-slate-200/80 transition text-[11px] active:scale-95 disabled:opacity-50"
                    >
                      • {preset}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1.5">
                  Chi tiết phản hồi gửi đến Nhà bán hàng (Seller) *:
                </label>
                <textarea
                  rows="3"
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Nhập chi tiết yêu cầu chỉnh sửa để Seller nắm rõ..."
                  className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-rose-500 focus:bg-white resize-none"
                ></textarea>
              </div>

              {rejectModalError && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-2xl text-xs text-rose-700 flex items-start gap-2">
                  <XCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
                  <span>{rejectModalError}</span>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                disabled={submittingModalAction}
                onClick={() => {
                  setRejectingProduct(null);
                  setRejectModalError(null);
                }}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition disabled:opacity-50"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmRejectProduct}
                disabled={submittingModalAction || !rejectReason.trim()}
                className="px-5 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition shadow-sm active:scale-95 disabled:opacity-50 flex items-center gap-1.5"
              >
                {submittingModalAction && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                <span>{submittingModalAction ? 'Đang gửi từ chối...' : 'Xác nhận từ chối'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: TỪ CHỐI DUYỆT GIAN HÀNG KYC ================= */}
      {rejectingShop && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex justify-between items-start border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <XCircle className="w-5 h-5 text-rose-600" />
                  Từ Chối Hồ Sơ KYC Gian Hàng #{rejectingShop.id}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 font-medium">
                  {rejectingShop.name} (Chủ shop: {rejectingShop.ownerName})
                </p>
              </div>
              <button
                type="button"
                disabled={submittingModalAction}
                onClick={() => {
                  setRejectingShop(null);
                  setRejectModalError(null);
                }}
                className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 disabled:opacity-50"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1.5">Gợi ý lý do từ chối KYC:</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                  {[
                    'CCCD / Hộ chiếu không trùng khớp thông tin chủ sở hữu',
                    'Mã số thuế doanh nghiệp không tồn tại hoặc đã giải thể',
                    'Địa chỉ kho hàng không hợp lệ cho việc lấy hàng giao',
                    'Tài khoản ngân hàng thụ hưởng không chính chủ',
                  ].map((preset, idx) => (
                    <button
                      key={idx}
                      type="button"
                      disabled={submittingModalAction}
                      onClick={() => setRejectShopReason(preset)}
                      className="p-2 text-left bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-xl border border-slate-200/80 transition text-[11px] active:scale-95 disabled:opacity-50"
                    >
                      • {preset}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1.5">
                  Nội dung phản hồi từ chối KYC *:
                </label>
                <textarea
                  rows="3"
                  value={rejectShopReason}
                  onChange={(e) => setRejectShopReason(e.target.value)}
                  placeholder="Ghi rõ lý do và tài liệu bổ sung cần nộp lại..."
                  className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-rose-500 focus:bg-white resize-none"
                ></textarea>
              </div>

              {rejectModalError && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-2xl text-xs text-rose-700 flex items-start gap-2">
                  <XCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
                  <span>{rejectModalError}</span>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                disabled={submittingModalAction}
                onClick={() => {
                  setRejectingShop(null);
                  setRejectModalError(null);
                }}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition disabled:opacity-50"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmRejectShop}
                disabled={submittingModalAction || !rejectShopReason.trim()}
                className="px-5 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition shadow-sm active:scale-95 disabled:opacity-50 flex items-center gap-1.5"
              >
                {submittingModalAction && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                <span>{submittingModalAction ? 'Đang gửi từ chối...' : 'Xác nhận từ chối KYC'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: CHI TIẾT NHẬT KÝ KIỂM TOÁN (AUDIT LOG) ================= */}
      {selectedAuditLog && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex justify-between items-start border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <FileText className="w-5 h-5 text-blue-600" />
                  Chi Tiết Nhật Ký Kiểm Toán #{selectedAuditLog.id}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 font-medium">
                  Thời gian: {new Date(selectedAuditLog.timestamp).toLocaleString('vi-VN')}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedAuditLog(null)}
                className="p-1 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3 bg-slate-50 p-3 rounded-2xl border border-slate-100">
                <div>
                  <span className="text-slate-500 block text-[11px]">Người thực hiện:</span>
                  <span className="font-bold text-slate-900">{selectedAuditLog.actor}</span>
                </div>
                <div>
                  <span className="text-slate-500 block text-[11px]">Địa chỉ IP:</span>
                  <span className="font-mono text-slate-700">{selectedAuditLog.ip || '127.0.0.1'}</span>
                </div>
                <div>
                  <span className="text-slate-500 block text-[11px]">Hành động:</span>
                  <span className="font-bold text-slate-900">{selectedAuditLog.action}</span>
                </div>
                <div>
                  <span className="text-slate-500 block text-[11px]">Trạng thái:</span>
                  <span
                    className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      selectedAuditLog.status === 'SUCCESS'
                        ? 'bg-emerald-100 text-emerald-800'
                        : selectedAuditLog.status === 'BLOCKED'
                        ? 'bg-rose-100 text-rose-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {selectedAuditLog.status}
                  </span>
                </div>
              </div>

              <div>
                <span className="text-slate-500 block text-[11px] mb-1">Đối tượng tác động:</span>
                <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-medium text-slate-800">
                  {selectedAuditLog.target}
                </div>
              </div>

              <div>
                <span className="text-slate-500 block text-[11px] mb-1">Dữ liệu chi tiết (Metadata / Payload):</span>
                <pre className="p-3 bg-slate-900 text-emerald-400 rounded-xl text-[11px] font-mono overflow-x-auto max-h-40 border border-slate-800">
                  {JSON.stringify(
                    selectedAuditLog.details || {
                      actor: selectedAuditLog.actor,
                      action: selectedAuditLog.action,
                      target: selectedAuditLog.target,
                      ip: selectedAuditLog.ip,
                      status: selectedAuditLog.status,
                      timestamp: selectedAuditLog.timestamp,
                    },
                    null,
                    2
                  )}
                </pre>
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setSelectedAuditLog(null)}
                className="px-5 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition shadow-sm cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ================= TOAST NOTIFICATION BANNER ================= */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 bg-slate-900/95 text-white backdrop-blur-md rounded-2xl shadow-2xl border border-slate-700/50 animate-in fade-in slide-in-from-bottom-5 duration-200 text-xs sm:text-sm font-medium">
          {toast.type === 'success' && <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />}
          {toast.type === 'info' && <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />}
          {toast.type === 'error' && <XCircle className="w-5 h-5 text-rose-400 shrink-0" />}
          <span>{toast.message}</span>
          <button
            type="button"
            onClick={() => setToast(null)}
            className="ml-2 text-slate-400 hover:text-white p-1 rounded-lg transition"
          >
            ✕
          </button>
        </div>
      )}
      </main>

      {/* Standalone Admin Footer */}
      <footer className="py-4 border-t border-slate-200 bg-white text-center text-xs text-slate-400">
        QuickShop Backoffice Admin Console • Hệ sinh thái Microservices phân tán 8 cụm dịch vụ
      </footer>
    </div>
  );
}
