import React, { useState, useEffect } from 'react';
import { 
  Store, Plus, Upload, Trash2, Edit, Sparkles, CheckCircle2, 
  Clock, XCircle, FileText, Send, ArrowLeft, Filter, Layers, 
  Image as ImageIcon, Tag, DollarSign, Package, AlertTriangle,
  LogOut, ShoppingBag, Truck, RotateCcw, MessageSquare, Wallet, 
  User, Printer, ChevronRight, Eye, Star, Check, Phone, MapPin, 
  Building, CreditCard, ExternalLink, HelpCircle, ShieldCheck,
  Menu, X, Search, RefreshCw, Zap, CheckCircle
} from 'lucide-react';
import { catalogApi } from '../api/catalogApi';
import { useAuthStore } from '../stores/useAuthStore';
import { defaultProducts } from '../data/sampleProducts';
import {
  getShopOrders,
  updateSubOrderFulfillmentStatus,
  getStoredRefunds,
  handleSellerRefundDecision,
  getStoredReviews,
  replyToShopReviewInStore,
  getSellerWalletData,
  createSellerWithdrawal,
} from '../data/sampleOrders';

export default function SellerPortal({ categories = [], onProductCreated, onBackToStorefront }) {
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    if (onBackToStorefront) {
      onBackToStorefront();
    }
  };

  // Sidebar navigation state: 'products' | 'orders' | 'refunds' | 'reviews' | 'wallet' | 'profile'
  const [activeTab, setActiveTab] = useState('products');
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Toast notification
  const [toast, setToast] = useState(null);
  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast((prev) => (prev?.message === message ? null : prev));
    }, 4000);
  };

  // ==========================================
  // 1. PRODUCTS MANAGEMENT STATES
  // ==========================================
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, APPROVED, PENDING, DRAFT, REJECTED

  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);

  // Form states for creating product
  const [name, setName] = useState('');
  const [categoryId, setCategoryId] = useState(categories[0]?.id || 1);
  const [brand, setBrand] = useState('Chính Hãng Phân Phối');
  const [description, setDescription] = useState('');
  const [mainImage, setMainImage] = useState('');
  const [descriptionImages, setDescriptionImages] = useState([]);
  const [newDescImageUrl, setNewDescImageUrl] = useState('');
  const [highlights, setHighlights] = useState(['Bảo hành chính hãng 12 tháng', 'Giao hàng hỏa tốc trong 2 giờ']);
  const [newHighlight, setNewHighlight] = useState('');
  const [specifications, setSpecifications] = useState([
    { label: 'Thương hiệu', value: 'Chính Hãng Phân Phối' },
    { label: 'Xuất xứ', value: 'Chính hãng VN/A' },
    { label: 'Bảo hành', value: '12 tháng toàn quốc' },
  ]);
  const [isDraft, setIsDraft] = useState(false);
  const [variants, setVariants] = useState([
    { sku: 'SKU-01', color: 'Đen', size: 'Tiêu chuẩn', price: 299000, stock: 50 },
  ]);

  // Form states for editing product
  const [editingId, setEditingId] = useState(null);
  const [editingProduct, setEditingProduct] = useState(null);
  const [editName, setEditName] = useState('');
  const [editCategoryId, setEditCategoryId] = useState(1);
  const [editBrand, setEditBrand] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editMainImage, setEditMainImage] = useState('');
  const [editDescriptionImages, setEditDescriptionImages] = useState([]);
  const [editNewDescImageUrl, setEditNewDescImageUrl] = useState('');
  const [editHighlights, setEditHighlights] = useState([]);
  const [editNewHighlight, setEditNewHighlight] = useState('');
  const [editSpecifications, setEditSpecifications] = useState([]);
  const [editVariants, setEditVariants] = useState([]);

  // AI SEO Generator state
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiSuccessMsg, setAiSuccessMsg] = useState('');

  // Danh sách sản phẩm mẫu đầy đủ chi tiết cho gian hàng (Shop ID #1)
  const defaultSellerProducts = defaultProducts.filter(
    (p) => Number(p.shopId) === Number(user?.shopId || 1) || !p.shopId
  );

  // Fetch products
  const fetchMyProducts = async () => {
    setLoading(true);
    try {
      const res = await catalogApi.getMySellerProducts({ shopId: user?.shopId || 1 });
      const serverItems = Array.isArray(res?.data)
        ? res.data
        : (Array.isArray(res?.data?.content) ? res.data.content : []);

      if (serverItems.length > 0) {
        // Bổ sung thêm highlights, specifications và descriptionImages nếu backend trả về thiếu
        const enriched = serverItems.map((item) => {
          const match = defaultProducts.find((p) => String(p.id) === String(item.id));
          return match
            ? {
                ...match,
                ...item,
                highlights: item.highlights || match.highlights || [],
                specifications: item.specifications || match.specifications || [],
                descriptionImages: item.descriptionImages || match.descriptionImages || [],
              }
            : item;
        });

        // Kết hợp với các sản phẩm mẫu của shop để người bán luôn có danh mục đa dạng
        const serverIds = new Set(enriched.map((p) => String(p.id)));
        const additional = defaultSellerProducts.filter((p) => !serverIds.has(String(p.id)));
        const finalSellerList = [...enriched, ...additional];

        setProducts(finalSellerList);
        localStorage.setItem('quickshop_seller_products', JSON.stringify(finalSellerList));
      } else {
        const local = localStorage.getItem('quickshop_seller_products');
        if (local) {
          try {
            const parsed = JSON.parse(local);
            setProducts(Array.isArray(parsed) && parsed.length > 0 ? parsed : defaultSellerProducts);
          } catch {
            setProducts(defaultSellerProducts);
          }
        } else {
          setProducts(defaultSellerProducts);
          localStorage.setItem('quickshop_seller_products', JSON.stringify(defaultSellerProducts));
        }
      }
    } catch {
      const local = localStorage.getItem('quickshop_seller_products');
      if (local) {
        try {
          const parsed = JSON.parse(local);
          setProducts(Array.isArray(parsed) && parsed.length > 0 ? parsed : defaultSellerProducts);
        } catch {
          setProducts(defaultSellerProducts);
        }
      } else {
        setProducts(defaultSellerProducts);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyProducts();
  }, []);

  // ==========================================
  // 2. ORDER FULFILLMENT STATES
  // ==========================================
  const [shopOrders, setShopOrders] = useState([]);
  const [orderFilter, setOrderFilter] = useState('ALL'); // ALL, PENDING, PACKING, SHIPPING, DELIVERED, CANCELLED
  const [selectedOrderForSlip, setSelectedOrderForSlip] = useState(null); // Packing slip modal

  const loadShopOrders = () => {
    const orders = getShopOrders(user?.shopId || 1);
    setShopOrders(orders);
  };

  useEffect(() => {
    if (activeTab === 'orders') {
      loadShopOrders();
    }
  }, [activeTab]);

  const handlePackOrder = (subOrderId) => {
    const updated = updateSubOrderFulfillmentStatus(subOrderId, 'PACKING');
    setShopOrders(updated);
    const target = updated.find((o) => o.id === subOrderId);
    if (target) {
      setSelectedOrderForSlip(target);
    }
    showToast('Đã chuyển đơn hàng sang trạng thái Chuẩn bị / Đóng gói thành công!');
  };

  const handleHandoverShipper = (subOrderId) => {
    const updated = updateSubOrderFulfillmentStatus(subOrderId, 'SHIPPING');
    setShopOrders(updated);
    if (selectedOrderForSlip && selectedOrderForSlip.id === subOrderId) {
      setSelectedOrderForSlip(null);
    }
    showToast('Đã bàn giao bưu kiện thành công cho Đơn vị vận chuyển GHN!');
  };

  // ==========================================
  // 3. RETURNS & REFUNDS STATES
  // ==========================================
  const [refunds, setRefunds] = useState([]);
  const [rejectingRefundId, setRejectingRefundId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  const loadRefunds = () => {
    const all = getStoredRefunds();
    setRefunds(all);
  };

  useEffect(() => {
    if (activeTab === 'refunds') {
      loadRefunds();
    }
  }, [activeTab]);

  const handleApproveRefund = (refundId) => {
    const updated = handleSellerRefundDecision(refundId, 'APPROVE', 'Shop đã kiểm tra và đồng ý hoàn tiền qua Escrow');
    setRefunds(updated);
    showToast('Đã duyệt yêu cầu hoàn tiền thành công! Tiền sẽ được hoàn trả cho người mua.');
  };

  const handleRejectRefund = (refundId) => {
    if (!rejectReason) {
      showToast('Vui lòng nhập lý do từ chối hoàn tiền', 'error');
      return;
    }
    const updated = handleSellerRefundDecision(refundId, 'REJECT', rejectReason);
    setRefunds(updated);
    setRejectingRefundId(null);
    setRejectReason('');
    showToast('Đã từ chối yêu cầu hoàn tiền.');
  };

  // ==========================================
  // 4. CUSTOMER REVIEWS & FEEDBACK STATES
  // ==========================================
  const [reviews, setReviews] = useState([]);
  const [replyingReviewId, setReplyingReviewId] = useState(null);
  const [replyText, setReplyText] = useState('');

  const loadReviews = () => {
    const all = getStoredReviews();
    setReviews(all);
  };

  useEffect(() => {
    if (activeTab === 'reviews') {
      loadReviews();
    }
  }, [activeTab]);

  const handleSendReviewReply = (reviewId) => {
    if (!replyText.trim()) {
      showToast('Vui lòng nhập nội dung phản hồi cho khách hàng', 'error');
      return;
    }
    const updated = replyToShopReviewInStore(reviewId, replyText);
    setReviews(updated);
    setReplyingReviewId(null);
    setReplyText('');
    showToast('Đã gửi câu trả lời phản hồi cho khách hàng thành công!');
  };

  // ==========================================
  // 5. SELLER WALLET & FINANCE STATES
  // ==========================================
  const [wallet, setWallet] = useState({ availableBalance: 125450000, escrowBalance: 32600000, totalRevenue: 489200000, withdrawals: [] });
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [withdrawAmount, setWithdrawAmount] = useState('10000000');
  const [bankName, setBankName] = useState('MB Bank - Ngân hàng Quân Đội');
  const [accountNumber, setAccountNumber] = useState('098812345678');
  const [accountHolder, setAccountHolder] = useState('LE DUC ANH');

  const loadWallet = () => {
    const data = getSellerWalletData(user?.shopId || 1);
    setWallet(data);
  };

  useEffect(() => {
    if (activeTab === 'wallet') {
      loadWallet();
    }
  }, [activeTab]);

  const handleRequestWithdraw = (e) => {
    e.preventDefault();
    const amt = parseInt(withdrawAmount, 10);
    if (!amt || amt <= 0 || amt > wallet.availableBalance) {
      showToast('Số tiền rút không hợp lệ hoặc vượt quá số dư khả dụng!', 'error');
      return;
    }
    const updated = createSellerWithdrawal(user?.shopId || 1, {
      amount: amt,
      bankName,
      accountNumber,
      accountHolder,
    });
    setWallet(updated);
    setShowWithdrawModal(false);
    showToast(`Đã tạo lệnh rút ${(amt).toLocaleString()}₫ thành công! Đang chờ Admin giải ngân.`);
  };

  // ==========================================
  // AI GEMINI DESCRIPTION GENERATOR
  // ==========================================
  const handleGenerateAiDescription = (isEditMode = false) => {
    const targetName = isEditMode ? editName : name;
    if (!targetName.trim()) {
      showToast('Vui lòng nhập tên sản phẩm trước khi sinh mô tả bằng AI', 'error');
      return;
    }

    setAiGenerating(true);
    setAiSuccessMsg('');

    setTimeout(() => {
      const generatedDesc = `Sản phẩm ${targetName} chính hãng thuộc phân khúc cao cấp tại gian hàng. Được sản xuất trên dây chuyền công nghệ hiện đại, đáp ứng các tiêu chuẩn kiểm định nghiêm ngặt về độ bền, hiệu năng và an toàn cho người sử dụng.

Thiết kế tinh tế sang trọng với từng đường nét được gia công chuẩn xác. Tích hợp các công nghệ thông minh mới nhất giúp tối ưu trải nghiệm và tiết kiệm điện năng. Đầy đủ hóa đơn chứng từ, tem niêm phong và bảo hành chính hãng 12 tháng tại các trung tâm trên toàn quốc.`;

      const generatedHighlights = [
        `Công nghệ tiên phong đáp ứng tối đa hiệu năng cho ${targetName}`,
        'Chất liệu cao cấp chống trầy xước, bền bỉ theo thời gian',
        'Tiết kiệm điện năng và tối ưu hóa trải nghiệm người dùng',
        'Bảo hành chính hãng 12 tháng, 1 đổi 1 trong 7 ngày đầu nếu lỗi',
      ];

      const sampleDescImages = [
        'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=1000&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=1000&auto=format&fit=crop&q=80',
      ];

      if (isEditMode) {
        setEditDescription(generatedDesc);
        setEditHighlights(generatedHighlights);
        setEditDescriptionImages(sampleDescImages);
      } else {
        setDescription(generatedDesc);
        setHighlights(generatedHighlights);
        setDescriptionImages(sampleDescImages);
      }

      setAiGenerating(false);
      setAiSuccessMsg('✨ AI Gemini đã sinh mô tả chi tiết, đặc điểm nổi bật và gợi ý hình ảnh minh họa thành công!');
      setTimeout(() => setAiSuccessMsg(''), 5000);
    }, 1200);
  };

  // Format currency helper
  const formatVND = (num) => `${(num || 0).toLocaleString('vi-VN')}₫`;

  // Filtered products list
  const filteredProducts = products.filter((p) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'APPROVED') return p.status === 'APPROVED' || p.status === 'ACTIVE';
    if (statusFilter === 'PENDING') return p.status === 'PENDING' || p.status === 'PENDING_APPROVAL';
    if (statusFilter === 'DRAFT') return p.status === 'DRAFT';
    if (statusFilter === 'REJECTED') return p.status === 'REJECTED';
    return true;
  });

  // Filtered orders list
  const filteredOrders = shopOrders.filter((o) => {
    if (orderFilter === 'ALL') return true;
    if (orderFilter === 'PENDING') return o.status === 'PENDING' || o.status === 'CONFIRMED';
    if (orderFilter === 'PACKING') return o.status === 'PACKING';
    if (orderFilter === 'SHIPPING') return o.status === 'SHIPPING' || o.status === 'DELIVERING';
    if (orderFilter === 'DELIVERED') return o.status === 'DELIVERED' || o.status === 'COMPLETED';
    if (orderFilter === 'CANCELLED') return o.status === 'CANCELLED';
    return true;
  });

  return (
    <div className="min-h-screen bg-slate-50 flex font-sans">
      {/* Toast Alert */}
      {toast && (
        <div
          className={`fixed top-5 right-5 z-50 px-4 py-3 rounded-2xl shadow-xl flex items-center space-x-2 text-xs font-semibold animate-in fade-in slide-in-from-top-2 border ${
            toast.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-800'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          {toast.type === 'error' ? (
            <XCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
          ) : (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
          )}
          <span>{toast.message}</span>
        </div>
      )}

      {/* ========================================================== */}
      {/* PROFESSIONAL SIDEBAR NAVIGATION (THANH BÊN ĐIỀU HƯỚNG) */}
      {/* ========================================================== */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-slate-900 text-slate-300 flex flex-col transition-transform duration-300 ease-in-out md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } shadow-2xl md:shadow-none`}
      >
        {/* Sidebar Header / Brand */}
        <div className="h-16 flex items-center justify-between px-5 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-orange-500 to-rose-600 text-white flex items-center justify-center shadow-lg shadow-orange-500/20 font-bold">
              <Store className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="text-sm font-black tracking-tight text-white flex items-center gap-1.5">
                <span>QUICKSHOP</span>
                <span className="text-[9px] font-bold px-1.5 py-0.2 rounded bg-orange-500/20 text-orange-400 border border-orange-500/30">
                  SELLER
                </span>
              </div>
              <p className="text-[10px] text-slate-400">Kênh Quản Lý Người Bán</p>
            </div>
          </div>
          <button
            onClick={() => setSidebarOpen(false)}
            className="md:hidden p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Shop Profile Banner in Sidebar */}
        <div className="p-4 mx-3 my-3 rounded-2xl bg-slate-800/60 border border-slate-700/60 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-orange-500/20 text-orange-400 font-bold flex items-center justify-center text-sm border border-orange-500/30 flex-shrink-0">
            {user?.fullName?.charAt(0) || 'T'}
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-xs font-bold text-white truncate">TechWorld Official Store</div>
            <div className="text-[10px] text-emerald-400 font-medium flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
              Đang hoạt động (Mall)
            </div>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 py-2 space-y-1 overflow-y-auto">
          <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500">
            Quản trị Bán hàng
          </div>

          <button
            onClick={() => { setActiveTab('products'); setSidebarOpen(false); }}
            className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
              activeTab === 'products'
                ? 'bg-orange-600 text-white shadow-lg shadow-orange-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <div className="flex items-center gap-3">
              <Package className="w-4 h-4" />
              <span>Quản lý Sản phẩm</span>
            </div>
            <span className={`text-[10px] px-1.5 py-0.5 rounded-full font-bold ${
              activeTab === 'products' ? 'bg-white/20 text-white' : 'bg-slate-800 text-slate-400'
            }`}>
              {products.length}
            </span>
          </button>

          <button
            onClick={() => { setActiveTab('orders'); setSidebarOpen(false); }}
            className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
              activeTab === 'orders'
                ? 'bg-orange-600 text-white shadow-lg shadow-orange-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <div className="flex items-center gap-3">
              <FileText className="w-4 h-4" />
              <span>Quản lý Đơn hàng</span>
            </div>
            {shopOrders.filter((o) => o.status === 'PENDING' || o.status === 'CONFIRMED' || o.status === 'PACKING').length > 0 && (
              <span className="text-[10px] px-1.5 py-0.5 rounded-full font-bold bg-amber-500 text-slate-950">
                {shopOrders.filter((o) => o.status === 'PENDING' || o.status === 'CONFIRMED' || o.status === 'PACKING').length}
              </span>
            )}
          </button>

          <button
            onClick={() => { setActiveTab('refunds'); setSidebarOpen(false); }}
            className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
              activeTab === 'refunds'
                ? 'bg-orange-600 text-white shadow-lg shadow-orange-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <div className="flex items-center gap-3">
              <RotateCcw className="w-4 h-4" />
              <span>Trả hàng / Hoàn tiền</span>
            </div>
            {refunds.filter((r) => r.status === 'PENDING_SELLER').length > 0 && (
              <span className="text-[10px] px-1.5 py-0.5 rounded-full font-bold bg-rose-500 text-white animate-pulse">
                {refunds.filter((r) => r.status === 'PENDING_SELLER').length}
              </span>
            )}
          </button>

          <button
            onClick={() => { setActiveTab('reviews'); setSidebarOpen(false); }}
            className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
              activeTab === 'reviews'
                ? 'bg-orange-600 text-white shadow-lg shadow-orange-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <div className="flex items-center gap-3">
              <MessageSquare className="w-4 h-4" />
              <span>Phản ánh & Đánh giá</span>
            </div>
            <span className="text-[10px] px-1.5 py-0.5 rounded-full font-bold bg-slate-800 text-slate-400">
              {reviews.length}
            </span>
          </button>

          <div className="pt-4 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500">
            Tài chính & Cài đặt
          </div>

          <button
            onClick={() => { setActiveTab('wallet'); setSidebarOpen(false); }}
            className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
              activeTab === 'wallet'
                ? 'bg-orange-600 text-white shadow-lg shadow-orange-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <div className="flex items-center gap-3">
              <Wallet className="w-4 h-4" />
              <span>Ví tiền & Doanh thu</span>
            </div>
            <span className="text-[10px] text-emerald-400 font-mono font-bold">
              {(wallet.availableBalance / 1000000).toFixed(1)}M
            </span>
          </button>

          <button
            onClick={() => { setActiveTab('profile'); setSidebarOpen(false); }}
            className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
              activeTab === 'profile'
                ? 'bg-orange-600 text-white shadow-lg shadow-orange-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <Building className="w-4 h-4" />
            <span>Hồ sơ Gian hàng (KYC)</span>
          </button>
        </nav>

        {/* Sidebar Footer */}
        <div className="p-3 border-t border-slate-800 bg-slate-950/40 space-y-1">
          {onBackToStorefront && (
            <button
              onClick={onBackToStorefront}
              className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition"
            >
              <ShoppingBag className="w-4 h-4 text-orange-500" />
              <span>Xem Sàn Mua Sắm</span>
            </button>
          )}

          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium text-rose-400 hover:bg-rose-500/10 transition"
          >
            <LogOut className="w-4 h-4" />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Backdrop for mobile sidebar */}
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 z-40 bg-slate-900/60 backdrop-blur-sm md:hidden"
        ></div>
      )}

      {/* ========================================================== */}
      {/* MAIN CONTENT WRAPPER */}
      {/* ========================================================== */}
      <div className="flex-1 md:ml-64 flex flex-col min-w-0">
        {/* Top Navbar */}
        <header className="sticky top-0 z-30 h-16 bg-white border-b border-slate-200 px-4 sm:px-8 flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="md:hidden p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50"
            >
              <Menu className="w-5 h-5" />
            </button>

            <div>
              <h2 className="text-sm sm:text-base font-bold text-slate-900">
                {activeTab === 'products' && 'Quản Lý Sản Phẩm & Tồn Kho SKU'}
                {activeTab === 'orders' && 'Quản Lý Đơn Hàng & Đóng Gói (Fulfillment)'}
                {activeTab === 'refunds' && 'Xử Lý Khiếu Nại Trả Hàng / Hoàn Tiền'}
                {activeTab === 'reviews' && 'Phản Ánh & Đánh Giá Từ Khách Hàng'}
                {activeTab === 'wallet' && 'Ví Người Bán & Quản Lý Dòng Tiền (Escrow)'}
                {activeTab === 'profile' && 'Cài Đặt Hồ Sơ & Địa Chỉ Kho Hàng'}
              </h2>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                TechWorld Official Store • Kênh bán hàng chuẩn Shopee Marketplace
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {onBackToStorefront && (
              <button
                onClick={onBackToStorefront}
                className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold transition"
              >
                <ShoppingBag className="w-3.5 h-3.5 text-orange-600" />
                <span>Xem Sàn Mua Sắm</span>
              </button>
            )}

            <div className="h-6 w-px bg-slate-200 hidden sm:block" />

            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-lg bg-orange-100 text-orange-700 font-bold flex items-center justify-center text-xs">
                {user?.fullName?.charAt(0) || 'S'}
              </div>
              <div className="text-left hidden sm:block">
                <div className="text-xs font-bold text-slate-900 truncate max-w-[130px]">{user?.fullName || 'Chủ Gian Hàng'}</div>
                <div className="text-[10px] text-emerald-600 font-semibold">{user?.role || 'ROLE_SELLER'}</div>
              </div>
            </div>

            <button
              onClick={handleLogout}
              className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
              title="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </header>

        {/* Tab Body Container */}
        <main className="p-4 sm:p-6 lg:p-8 space-y-6 flex-1">

          {/* ========================================================== */}
          {/* TAB 1: QUẢN LÝ SẢN PHẨM (PRODUCTS) */}
          {/* ========================================================== */}
          {activeTab === 'products' && (
            <div className="space-y-6">
              {/* Header Banner */}
              <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center shadow-lg shadow-orange-500/20">
                    <Package className="w-7 h-7" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h1 className="text-lg sm:text-xl font-bold text-slate-900">Danh Mục Sản Phẩm Đăng Bán</h1>
                      <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-full">
                        {products.length} Sản phẩm
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Đăng tải mô tả chi tiết kèm hình ảnh minh họa, thông số kỹ thuật và biến thể SKU
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => setShowCreateModal(true)}
                  className="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-semibold text-xs sm:text-sm rounded-xl transition shadow-lg shadow-orange-500/30 flex items-center gap-2 active:scale-95"
                >
                  <Plus className="w-4 h-4 stroke-[3]" />
                  <span>Đăng bán sản phẩm mới</span>
                </button>
              </div>

              {/* Statistics */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
                  <div className="text-[11px] font-medium text-slate-400">Đã Phê Duyệt / Bán</div>
                  <div className="text-xl font-black text-emerald-600 mt-1">
                    {products.filter((p) => p.status === 'APPROVED' || p.status === 'ACTIVE').length}
                  </div>
                </div>
                <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
                  <div className="text-[11px] font-medium text-slate-400">Chờ Kiểm Duyệt</div>
                  <div className="text-xl font-black text-amber-500 mt-1">
                    {products.filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL').length}
                  </div>
                </div>
                <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
                  <div className="text-[11px] font-medium text-slate-400">Bản Nháp (Draft)</div>
                  <div className="text-xl font-black text-slate-600 mt-1">
                    {products.filter((p) => p.status === 'DRAFT').length}
                  </div>
                </div>
                <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
                  <div className="text-[11px] font-medium text-slate-400">Bị Từ Chối</div>
                  <div className="text-xl font-black text-rose-500 mt-1">
                    {products.filter((p) => p.status === 'REJECTED').length}
                  </div>
                </div>
              </div>

              {/* Filters */}
              <div className="flex flex-wrap items-center gap-2">
                {['ALL', 'APPROVED', 'PENDING', 'DRAFT', 'REJECTED'].map((st) => (
                  <button
                    key={st}
                    onClick={() => setStatusFilter(st)}
                    className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                      statusFilter === st
                        ? 'bg-slate-900 text-white shadow-sm'
                        : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {st === 'ALL' && 'Tất cả'}
                    {st === 'APPROVED' && 'Đang bán (Approved)'}
                    {st === 'PENDING' && 'Chờ duyệt (Pending)'}
                    {st === 'DRAFT' && 'Bản nháp (Draft)'}
                    {st === 'REJECTED' && 'Bị từ chối (Rejected)'}
                  </button>
                ))}
              </div>

              {/* Products Table */}
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50/80 border-b border-slate-200/80 text-[11px] uppercase tracking-wider text-slate-500 font-bold">
                        <th className="py-3.5 px-4">Sản phẩm</th>
                        <th className="py-3.5 px-4">Danh mục</th>
                        <th className="py-3.5 px-4">Kho & Biến thể</th>
                        <th className="py-3.5 px-4">Giá bán</th>
                        <th className="py-3.5 px-4">Trạng thái</th>
                        <th className="py-3.5 px-4 text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {loading ? (
                        <tr>
                          <td colSpan="6" className="py-12 text-center text-slate-400">
                            <div className="w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                            <span>Đang nạp danh sách sản phẩm từ gian hàng...</span>
                          </td>
                        </tr>
                      ) : filteredProducts.length === 0 ? (
                        <tr>
                          <td colSpan="6" className="py-12 text-center text-slate-400">
                            <Package className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                            <p className="font-semibold text-slate-600">Không có sản phẩm nào trong trạng thái này</p>
                            <button
                              onClick={() => setStatusFilter('ALL')}
                              className="mt-2 text-xs font-bold text-orange-600 hover:underline"
                            >
                              Xem tất cả sản phẩm
                            </button>
                          </td>
                        </tr>
                      ) : (
                        filteredProducts.map((p) => (
                        <tr key={p.id} className="hover:bg-slate-50/50 transition">
                          <td className="py-3.5 px-4">
                            <div className="flex items-center space-x-3 max-w-sm">
                              <img
                                src={p.mainImage || 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=100&auto=format&fit=crop&q=80'}
                                alt=""
                                className="w-12 h-12 rounded-xl object-cover border border-slate-200 flex-shrink-0"
                              />
                              <div className="min-w-0">
                                <div className="font-bold text-slate-900 line-clamp-1">{p.name}</div>
                                <div className="text-[11px] text-slate-400 line-clamp-1">{p.description}</div>
                              </div>
                            </div>
                          </td>
                          <td className="py-3.5 px-4 text-slate-600 font-medium">{p.categoryName}</td>
                          <td className="py-3.5 px-4">
                            <div className="font-semibold text-slate-800">
                              {p.variants?.reduce((sum, v) => sum + (v.stock || 0), 0) || 50} cái
                            </div>
                            <div className="text-[10px] text-slate-400">{p.variants?.length || 1} biến thể SKU</div>
                          </td>
                          <td className="py-3.5 px-4 font-black text-orange-600">
                            {formatVND(p.minPrice || p.variants?.[0]?.price || 299000)}
                          </td>
                          <td className="py-3.5 px-4">
                            {p.status === 'APPROVED' || p.status === 'ACTIVE' ? (
                              <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-lg font-bold text-[10px]">
                                Đang hiển thị
                              </span>
                            ) : p.status === 'PENDING' ? (
                              <span className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded-lg font-bold text-[10px]">
                                Chờ duyệt
                              </span>
                            ) : p.status === 'DRAFT' ? (
                              <span className="px-2.5 py-1 bg-slate-100 text-slate-600 rounded-lg font-bold text-[10px]">
                                Bản nháp
                              </span>
                            ) : (
                              <span className="px-2.5 py-1 bg-rose-100 text-rose-800 rounded-lg font-bold text-[10px]">
                                Bị từ chối
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <div className="inline-flex items-center gap-1.5">
                              <button
                                onClick={() => {
                                  setEditingId(p.id);
                                  setEditingProduct(p);
                                  setEditName(p.name);
                                  setEditCategoryId(p.categoryId || 1);
                                  setEditBrand(p.brand || 'Chính Hãng');
                                  setEditDescription(p.description || '');
                                  setEditMainImage(p.mainImage || '');
                                  setEditDescriptionImages(p.descriptionImages || []);
                                  setEditHighlights(p.highlights || []);
                                  setEditSpecifications(p.specifications || []);
                                  setEditVariants(p.variants || []);
                                  setShowEditModal(true);
                                }}
                                className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-600 transition"
                                title="Chỉnh sửa sản phẩm"
                              >
                                <Edit className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      )))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================== */}
          {/* TAB 2: QUẢN LÝ ĐƠN HÀNG SHOP (ORDER FULFILLMENT & PACKING) */}
          {/* ========================================================== */}
          {activeTab === 'orders' && (
            <div className="space-y-6">
              {/* Banner */}
              <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-blue-500 to-indigo-600 text-white flex items-center justify-center shadow-lg shadow-blue-500/20">
                    <FileText className="w-7 h-7" />
                  </div>
                  <div>
                    <h1 className="text-lg sm:text-xl font-bold text-slate-900">Quản Lý Đơn Hàng & Đóng Gói</h1>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Xác nhận chuẩn bị hàng, in phiếu xuất kho GHN/GHTK và bàn giao cho Shipper
                    </p>
                  </div>
                </div>

                <button
                  onClick={loadShopOrders}
                  className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition flex items-center gap-1.5"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Làm mới đơn hàng</span>
                </button>
              </div>

              {/* Status Filters */}
              <div className="flex flex-wrap items-center gap-2">
                {[
                  { id: 'ALL', label: 'Tất cả' },
                  { id: 'PENDING', label: 'Chờ chuẩn bị' },
                  { id: 'PACKING', label: 'Đang đóng gói' },
                  { id: 'SHIPPING', label: 'Đã giao bưu tá' },
                  { id: 'DELIVERED', label: 'Giao thành công' },
                  { id: 'CANCELLED', label: 'Đã hủy' },
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => setOrderFilter(f.id)}
                    className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                      orderFilter === f.id
                        ? 'bg-slate-900 text-white shadow-sm'
                        : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>

              {/* Orders List */}
              <div className="space-y-4">
                {filteredOrders.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 space-y-3">
                    <Package className="w-10 h-10 text-slate-300 mx-auto" />
                    <p className="text-xs text-slate-500">Chưa có đơn hàng nào ở trạng thái này</p>
                  </div>
                ) : (
                  filteredOrders.map((sub) => (
                    <div
                      key={sub.id}
                      className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 space-y-4 hover:shadow-md transition"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100 text-xs">
                        <div className="flex items-center gap-3">
                          <span className="font-mono font-black text-slate-900">#{sub.orderCode}</span>
                          <span className="text-slate-300">|</span>
                          <span className="text-slate-500">
                            {new Date(sub.createdAt).toLocaleString('vi-VN')}
                          </span>
                          <span className="text-slate-300">|</span>
                          <span className="text-slate-700 font-semibold">
                            PTTT: {sub.paymentMethod} ({sub.paymentStatus})
                          </span>
                        </div>

                        <div>
                          {sub.status === 'PENDING' || sub.status === 'CONFIRMED' ? (
                            <span className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded-lg font-bold text-[11px]">
                              Chờ người bán đóng hàng
                            </span>
                          ) : sub.status === 'PACKING' ? (
                            <span className="px-2.5 py-1 bg-blue-100 text-blue-800 rounded-lg font-bold text-[11px]">
                              Đang đóng gói & Chờ shipper
                            </span>
                          ) : sub.status === 'SHIPPING' ? (
                            <span className="px-2.5 py-1 bg-indigo-100 text-indigo-800 rounded-lg font-bold text-[11px]">
                              Đã bàn giao bưu tá GHN
                            </span>
                          ) : sub.status === 'DELIVERED' || sub.status === 'COMPLETED' ? (
                            <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-lg font-bold text-[11px]">
                              Giao thành công
                            </span>
                          ) : (
                            <span className="px-2.5 py-1 bg-slate-100 text-slate-600 rounded-lg font-bold text-[11px]">
                              {sub.status}
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Items */}
                      <div className="space-y-3">
                        {(sub.items || []).map((it) => (
                          <div key={it.id} className="flex items-center gap-3">
                            <img
                              src={it.imageUrl || 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=100&auto=format&fit=crop&q=80'}
                              alt=""
                              className="w-14 h-14 rounded-xl object-cover border border-slate-200"
                            />
                            <div className="flex-1 min-w-0">
                              <h4 className="text-xs font-bold text-slate-900 line-clamp-1">{it.productName}</h4>
                              <div className="text-[11px] text-slate-500">Phân loại: {it.variantName}</div>
                              <div className="text-[11px] text-slate-400">Số lượng: x{it.quantity}</div>
                            </div>
                            <div className="text-right">
                              <span className="text-xs font-black text-orange-600">{formatVND(it.price * it.quantity)}</span>
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Customer Address & Action Bar */}
                      <div className="pt-3 border-t border-slate-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
                        <div className="text-slate-600">
                          <div>Người nhận: <strong>{sub.recipientName}</strong> ({sub.recipientPhone})</div>
                          <div className="text-[11px] text-slate-400 truncate max-w-md">{sub.deliveryAddress}</div>
                        </div>

                        <div className="flex items-center gap-2">
                          {(sub.status === 'PENDING' || sub.status === 'CONFIRMED') && (
                            <button
                              onClick={() => handlePackOrder(sub.id)}
                              className="px-4 py-2 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white font-bold rounded-xl shadow-md transition flex items-center gap-1.5"
                            >
                              <Package className="w-3.5 h-3.5 stroke-[2.5]" />
                              <span>Đóng Hàng / In Phiếu</span>
                            </button>
                          )}

                          {sub.status === 'PACKING' && (
                            <>
                              <button
                                onClick={() => setSelectedOrderForSlip(sub)}
                                className="px-3 py-2 border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold rounded-xl transition flex items-center gap-1.5"
                              >
                                <Printer className="w-3.5 h-3.5" />
                                <span>In Phiếu Giao</span>
                              </button>
                              <button
                                onClick={() => handleHandoverShipper(sub.id)}
                                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl shadow-md transition flex items-center gap-1.5"
                              >
                                <Truck className="w-3.5 h-3.5" />
                                <span>Giao Cho Bưu Tá GHN</span>
                              </button>
                            </>
                          )}

                          {sub.trackingCode && (
                            <span className="font-mono text-[11px] font-bold text-blue-600 px-2 py-1 bg-blue-50 rounded-lg">
                              Mã vận đơn: {sub.trackingCode}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* ========================================================== */}
          {/* TAB 3: TRẢ HÀNG & HOÀN TIỀN (RETURNS & REFUNDS) */}
          {/* ========================================================== */}
          {activeTab === 'refunds' && (
            <div className="space-y-6">
              <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex items-center space-x-4">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-rose-500 to-amber-500 text-white flex items-center justify-center shadow-lg shadow-rose-500/20">
                  <RotateCcw className="w-7 h-7" />
                </div>
                <div>
                  <h1 className="text-lg sm:text-xl font-bold text-slate-900">Quản Lý Trả Hàng / Hoàn Tiền</h1>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Tiếp nhận khiếu nại, xem ảnh chứng từ của người mua và giải phóng hoàn tiền từ Escrow
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                {refunds.map((ref) => (
                  <div
                    key={ref.id}
                    className="bg-white rounded-3xl border border-slate-200/90 p-5 shadow-sm space-y-4"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100 text-xs">
                      <div>
                        <span className="font-mono font-bold text-slate-900">Mã yêu cầu: #{ref.id}</span>
                        <span className="text-slate-300 mx-2">|</span>
                        <span className="text-slate-500">Đơn hàng: #{ref.orderId}</span>
                      </div>
                      <div>
                        {ref.status === 'PENDING_SELLER' ? (
                          <span className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded-lg font-bold text-[11px]">
                            Chờ shop duyệt
                          </span>
                        ) : ref.status === 'APPROVED' ? (
                          <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-lg font-bold text-[11px]">
                            Đã duyệt hoàn tiền
                          </span>
                        ) : (
                          <span className="px-2.5 py-1 bg-rose-100 text-rose-800 rounded-lg font-bold text-[11px]">
                            Đã từ chối
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                      <div className="space-y-1.5">
                        <div className="text-slate-500">Khách hàng: <strong>{ref.customerName}</strong> ({ref.customerPhone})</div>
                        <div className="text-slate-500">Sản phẩm khiếu nại: <strong>{ref.productName}</strong></div>
                        <div className="text-slate-500">
                          Số tiền yêu cầu hoàn: <strong className="text-orange-600 text-sm font-black">{formatVND(ref.amount)}</strong>
                        </div>
                        <div className="p-3 bg-rose-50/70 border border-rose-100 rounded-xl text-rose-900 mt-2">
                          <strong>Lý do của khách:</strong> {ref.reason}
                        </div>
                      </div>

                      <div>
                        <div className="text-[11px] font-bold text-slate-700 mb-1.5">Ảnh bằng chứng khách gửi:</div>
                        {ref.proofImages && ref.proofImages.length > 0 ? (
                          <div className="flex gap-2">
                            {ref.proofImages.map((img, i) => (
                              <img
                                key={i}
                                src={img}
                                alt="proof"
                                className="w-24 h-24 object-cover rounded-xl border border-slate-200"
                              />
                            ))}
                          </div>
                        ) : (
                          <div className="text-[11px] text-slate-400 italic">Không đính kèm ảnh</div>
                        )}

                        {ref.sellerResponse && (
                          <div className="mt-2 text-[11px] text-slate-600 bg-slate-50 p-2 rounded-lg">
                            <strong>Phản hồi của shop:</strong> {ref.sellerResponse}
                          </div>
                        )}
                      </div>
                    </div>

                    {ref.status === 'PENDING_SELLER' && (
                      <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                        <button
                          onClick={() => setRejectingRefundId(ref.id)}
                          className="px-4 py-2 border border-slate-300 text-slate-700 hover:bg-slate-50 rounded-xl text-xs font-bold transition"
                        >
                          Từ chối hoàn tiền
                        </button>
                        <button
                          onClick={() => handleApproveRefund(ref.id)}
                          className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-md transition"
                        >
                          Chấp nhận hoàn tiền qua Escrow
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ========================================================== */}
          {/* TAB 4: ĐÁNH GIÁ & PHẢN HỒI KHÁCH HÀNG (REVIEWS & FEEDBACK) */}
          {/* ========================================================== */}
          {activeTab === 'reviews' && (
            <div className="space-y-6">
              <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex items-center space-x-4">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-500 text-white flex items-center justify-center shadow-lg shadow-amber-500/20">
                  <MessageSquare className="w-7 h-7" />
                </div>
                <div>
                  <h1 className="text-lg sm:text-xl font-bold text-slate-900">Phản Ánh & Đánh Giá Khách Hàng</h1>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Lắng nghe phản hồi từ khách hàng và gửi câu trả lời chăm sóc khách hàng từ gian hàng
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                {reviews.map((rev) => (
                  <div
                    key={rev.id}
                    className="bg-white rounded-3xl border border-slate-200/90 p-5 shadow-sm space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <img
                          src={rev.customerAvatar}
                          alt=""
                          className="w-10 h-10 rounded-full object-cover border border-slate-200"
                        />
                        <div>
                          <div className="text-xs font-bold text-slate-900">{rev.customerName}</div>
                          <div className="flex text-amber-400 mt-0.5">
                            {[1, 2, 3, 4, 5].map((s) => (
                              <Star
                                key={s}
                                className={`w-3.5 h-3.5 ${s <= rev.rating ? 'fill-amber-400 text-amber-400' : 'text-slate-200'}`}
                              />
                            ))}
                          </div>
                        </div>
                      </div>
                      <span className="text-[11px] text-slate-400">
                        {new Date(rev.createdAt).toLocaleString('vi-VN')}
                      </span>
                    </div>

                    <div className="text-xs text-slate-500">
                      Sản phẩm: <strong className="text-slate-800">{rev.productName}</strong>
                    </div>

                    <p className="text-xs text-slate-700 leading-relaxed bg-slate-50 p-3 rounded-xl border border-slate-100">
                      "{rev.comment}"
                    </p>

                    {/* Shop reply display */}
                    {rev.sellerReply ? (
                      <div className="p-3 bg-orange-50/70 border border-orange-100 rounded-xl text-xs space-y-1">
                        <div className="font-bold text-orange-900 flex items-center gap-1.5">
                          <Store className="w-3.5 h-3.5 text-orange-600" />
                          <span>Phản hồi từ người bán (TechWorld):</span>
                        </div>
                        <p className="text-slate-700 pl-5">{rev.sellerReply}</p>
                      </div>
                    ) : (
                      <div className="pt-2">
                        {replyingReviewId === rev.id ? (
                          <div className="space-y-2">
                            <textarea
                              rows={2}
                              value={replyText}
                              onChange={(e) => setReplyText(e.target.value)}
                              placeholder="Nhập câu trả lời phản hồi khách hàng..."
                              className="w-full px-3 py-2 border border-slate-300 rounded-xl text-xs outline-none focus:ring-2 focus:ring-orange-500"
                            />
                            <div className="flex justify-end gap-2">
                              <button
                                onClick={() => setReplyingReviewId(null)}
                                className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-lg"
                              >
                                Hủy
                              </button>
                              <button
                                onClick={() => handleSendReviewReply(rev.id)}
                                className="px-4 py-1.5 bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold rounded-lg shadow-sm"
                              >
                                Gửi phản hồi
                              </button>
                            </div>
                          </div>
                        ) : (
                          <button
                            onClick={() => {
                              setReplyingReviewId(rev.id);
                              setReplyText('');
                            }}
                            className="text-xs font-bold text-orange-600 hover:underline flex items-center gap-1"
                          >
                            <Send className="w-3 h-3" />
                            <span>Trả lời đánh giá</span>
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ========================================================== */}
          {/* TAB 5: VÍ TIỀN & DOANH THU (WALLET & FINANCE) */}
          {/* ========================================================== */}
          {activeTab === 'wallet' && (
            <div className="space-y-6">
              <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-600 text-white flex items-center justify-center shadow-lg shadow-emerald-500/20">
                    <Wallet className="w-7 h-7" />
                  </div>
                  <div>
                    <h1 className="text-lg sm:text-xl font-bold text-slate-900">Ví Tiền & Doanh Thu Gian Hàng</h1>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Đối soát tài chính, bảo vệ ký quỹ Escrow và rút tiền về tài khoản ngân hàng
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => setShowWithdrawModal(true)}
                  className="px-5 py-2.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white font-bold text-xs sm:text-sm rounded-xl transition shadow-lg shadow-emerald-600/30 flex items-center gap-2 active:scale-95"
                >
                  <DollarSign className="w-4 h-4" />
                  <span>Rút tiền về Ngân hàng</span>
                </button>
              </div>

              {/* Stat Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="bg-gradient-to-br from-emerald-500 to-teal-700 text-white p-6 rounded-3xl shadow-lg shadow-emerald-600/20 space-y-1">
                  <div className="text-xs font-semibold text-emerald-100">Số Dư Khả Dụng (Rút ngay)</div>
                  <div className="text-2xl sm:text-3xl font-black">{formatVND(wallet.availableBalance)}</div>
                  <div className="text-[11px] text-emerald-100 pt-2 flex items-center gap-1">
                    <CheckCircle className="w-3.5 h-3.5" />
                    Đã hoàn thành thời gian ký quỹ Escrow
                  </div>
                </div>

                <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-1">
                  <div className="text-xs font-semibold text-slate-400">Tiền Ký Quỹ Tạm Giữ (Escrow Hold)</div>
                  <div className="text-2xl sm:text-3xl font-black text-amber-500">{formatVND(wallet.escrowBalance)}</div>
                  <div className="text-[11px] text-slate-400 pt-2 flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    Sẽ giải phóng sau khi khách nhận hàng 72h
                  </div>
                </div>

                <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-1">
                  <div className="text-xs font-semibold text-slate-400">Tổng Doanh Thu Tích Lũy</div>
                  <div className="text-2xl sm:text-3xl font-black text-slate-900">{formatVND(wallet.totalRevenue)}</div>
                  <div className="text-[11px] text-slate-400 pt-2">
                    Phí dịch vụ nền tảng đã trích (5%): {formatVND(wallet.commissionPaid || 24460000)}
                  </div>
                </div>
              </div>

              {/* Withdrawals History */}
              <div className="bg-white rounded-3xl border border-slate-200/90 p-6 shadow-sm space-y-4">
                <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-emerald-600" />
                  Lịch Sử Các Lệnh Rút Tiền
                </h3>

                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-200 text-[11px] uppercase tracking-wider text-slate-500 font-bold">
                        <th className="py-3 px-4">Mã lệnh</th>
                        <th className="py-3 px-4">Số tiền</th>
                        <th className="py-3 px-4">Tài khoản thụ hưởng</th>
                        <th className="py-3 px-4">Thời gian tạo</th>
                        <th className="py-3 px-4">Trạng thái</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {(wallet.withdrawals || []).map((w) => (
                        <tr key={w.id} className="hover:bg-slate-50/50 transition">
                          <td className="py-3 px-4 font-mono font-bold text-slate-900">{w.id}</td>
                          <td className="py-3 px-4 font-black text-emerald-600">{formatVND(w.amount)}</td>
                          <td className="py-3 px-4 text-slate-700">
                            <div>{w.bankName}</div>
                            <div className="font-mono text-[11px] text-slate-500">{w.accountNumber} - {w.accountHolder}</div>
                          </td>
                          <td className="py-3 px-4 text-slate-500">{new Date(w.createdAt).toLocaleString('vi-VN')}</td>
                          <td className="py-3 px-4">
                            {w.status === 'COMPLETED' ? (
                              <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-lg font-bold text-[10px]">
                                Đã chuyển khoản
                              </span>
                            ) : (
                              <span className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded-lg font-bold text-[10px]">
                                Đang xử lý
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================== */}
          {/* TAB 6: HỒ SƠ GIAN HÀNG (SHOP PROFILE) */}
          {/* ========================================================== */}
          {activeTab === 'profile' && (
            <div className="bg-white rounded-3xl border border-slate-200/90 p-6 sm:p-8 shadow-sm space-y-6">
              <div className="flex items-center space-x-4 pb-4 border-b border-slate-100">
                <div className="w-16 h-16 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-xl border border-orange-200">
                  TW
                </div>
                <div>
                  <h2 className="text-lg font-bold text-slate-900">TechWorld Official Store</h2>
                  <p className="text-xs text-slate-500">Gian hàng chính hãng QuickShop Mall • Định danh doanh nghiệp KYC chuẩn</p>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-slate-400 font-medium">Chủ sở hữu:</span>
                  <div className="font-bold text-slate-900">{user?.fullName || 'Lê Đức Anh'}</div>
                </div>
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-slate-400 font-medium">Hotline hỗ trợ:</span>
                  <div className="font-bold text-slate-900">0988 123 456</div>
                </div>
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-slate-400 font-medium">Địa chỉ kho gửi hàng:</span>
                  <div className="font-bold text-slate-900">Tầng 3 Tòa Dolphin Plaza, 28 Trần Bình, Nam Từ Liêm, Hà Nội</div>
                </div>
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-slate-400 font-medium">Đơn vị vận chuyển liên kết:</span>
                  <div className="font-bold text-blue-600">Giao Hàng Nhanh (GHN) Express</div>
                </div>
              </div>
            </div>
          )}

        </main>
      </div>

      {/* ========================================================== */}
      {/* MODAL: TẠO SẢN PHẨM MỚI (CÓ THÊM ẢNH MÔ TẢ & AI GEMINI) */}
      {/* ========================================================== */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 my-8 space-y-5 animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
                  <Plus className="w-4 h-4 stroke-[3]" />
                </div>
                <h3 className="font-bold text-base text-slate-900">Đăng Bán Sản Phẩm Mới</h3>
              </div>
              <button onClick={() => setShowCreateModal(false)} className="p-1 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
              <div>
                <label className="text-xs font-bold text-slate-700">Tên sản phẩm (*):</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Ví dụ: Điện thoại thông minh QuickPhone Pro Max 256GB..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none focus:ring-2 focus:ring-orange-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700">Danh mục:</label>
                  <select
                    value={categoryId}
                    onChange={(e) => setCategoryId(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700">Thương hiệu:</label>
                  <input
                    type="text"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700">Ảnh đại diện chính (URL):</label>
                <input
                  type="text"
                  value={mainImage}
                  onChange={(e) => setMainImage(e.target.value)}
                  placeholder="https://images.unsplash.com/photo-..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none"
                />
              </div>

              {/* Nút AI Gemini Sinh mô tả chi tiết & ảnh */}
              <div className="p-3 bg-gradient-to-r from-orange-50 to-amber-50 border border-orange-200/80 rounded-2xl flex items-center justify-between">
                <div>
                  <div className="text-xs font-bold text-orange-950 flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-orange-600" />
                    <span>Trợ Lý AI Gemini Marketing & SEO</span>
                  </div>
                  <p className="text-[11px] text-slate-500">Tự động sinh mô tả chi tiết chuẩn TMĐT, thông số và ảnh minh họa</p>
                </div>
                <button
                  type="button"
                  onClick={() => handleGenerateAiDescription(false)}
                  disabled={aiGenerating}
                  className="px-3 py-1.5 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white text-xs font-bold rounded-xl shadow-sm transition flex items-center gap-1"
                >
                  {aiGenerating ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Sparkles className="w-3.5 h-3.5" />}
                  <span>{aiGenerating ? 'Đang viết...' : 'Sinh Bằng AI'}</span>
                </button>
              </div>
              {aiSuccessMsg && <p className="text-xs text-emerald-600 font-semibold">{aiSuccessMsg}</p>}

              {/* Mô tả chi tiết */}
              <div>
                <label className="text-xs font-bold text-slate-700">Mô tả chi tiết sản phẩm:</label>
                <textarea
                  rows={4}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Giới thiệu chi tiết về công năng, vật liệu, chế độ bảo hành..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none focus:ring-2 focus:ring-orange-500"
                />
              </div>

              {/* Thêm hình ảnh minh họa trong mô tả (User Requirement) */}
              <div className="space-y-2 p-3 bg-slate-50 rounded-2xl border border-slate-200">
                <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                  <ImageIcon className="w-3.5 h-3.5 text-orange-600" />
                  <span>Hình ảnh minh họa chèn trong mô tả (Rich Visual Showcase):</span>
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newDescImageUrl}
                    onChange={(e) => setNewDescImageUrl(e.target.value)}
                    placeholder="Dán link ảnh minh họa (https://...)"
                    className="flex-1 px-3 py-2 border border-slate-300 rounded-xl text-xs outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      if (newDescImageUrl.trim()) {
                        setDescriptionImages([...descriptionImages, newDescImageUrl.trim()]);
                        setNewDescImageUrl('');
                      }
                    }}
                    className="px-3 py-2 bg-slate-900 text-white rounded-xl text-xs font-bold hover:bg-slate-800"
                  >
                    Thêm ảnh
                  </button>
                </div>

                {descriptionImages.length > 0 && (
                  <div className="flex flex-wrap gap-2 pt-2">
                    {descriptionImages.map((img, i) => (
                      <div key={i} className="relative group w-20 h-20 rounded-xl overflow-hidden border border-slate-300">
                        <img src={img} alt="" className="w-full h-full object-cover" />
                        <button
                          type="button"
                          onClick={() => setDescriptionImages(descriptionImages.filter((_, idx) => idx !== i))}
                          className="absolute inset-0 bg-rose-900/60 text-white opacity-0 group-hover:opacity-100 flex items-center justify-center transition"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Biến thể SKU */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700">Biến thể phân loại SKU & Giá:</label>
                {variants.map((v, idx) => (
                  <div key={idx} className="grid grid-cols-4 gap-2 items-center bg-slate-50 p-2 rounded-xl">
                    <input
                      type="text"
                      placeholder="Màu sắc"
                      value={v.color}
                      onChange={(e) => {
                        const copy = [...variants];
                        copy[idx].color = e.target.value;
                        setVariants(copy);
                      }}
                      className="px-2 py-1.5 border border-slate-300 rounded-lg text-xs"
                    />
                    <input
                      type="text"
                      placeholder="Size/Dung lượng"
                      value={v.size}
                      onChange={(e) => {
                        const copy = [...variants];
                        copy[idx].size = e.target.value;
                        setVariants(copy);
                      }}
                      className="px-2 py-1.5 border border-slate-300 rounded-lg text-xs"
                    />
                    <input
                      type="number"
                      placeholder="Giá (VNĐ)"
                      value={v.price}
                      onChange={(e) => {
                        const copy = [...variants];
                        copy[idx].price = Number(e.target.value);
                        setVariants(copy);
                      }}
                      className="px-2 py-1.5 border border-slate-300 rounded-lg text-xs font-bold text-orange-600"
                    />
                    <input
                      type="number"
                      placeholder="Tồn kho"
                      value={v.stock}
                      onChange={(e) => {
                        const copy = [...variants];
                        copy[idx].stock = Number(e.target.value);
                        setVariants(copy);
                      }}
                      className="px-2 py-1.5 border border-slate-300 rounded-lg text-xs"
                    />
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={() => {
                  if (!name) {
                    showToast('Vui lòng nhập tên sản phẩm', 'error');
                    return;
                  }
                  const newProduct = {
                    id: Date.now(),
                    shopId: user?.shopId || 1,
                    shopName: 'TechWorld Official Store',
                    categoryId,
                    categoryName: categories.find((c) => c.id === categoryId)?.name || 'Điện tử',
                    brand,
                    name,
                    description,
                    highlights,
                    specifications,
                    descriptionImages,
                    mainImage: mainImage || 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
                    status: 'PENDING',
                    minPrice: variants[0]?.price || 299000,
                    variants,
                  };
                  const updated = [newProduct, ...products];
                  setProducts(updated);
                  localStorage.setItem('quickshop_seller_products', JSON.stringify(updated));
                  setShowCreateModal(false);
                  showToast('Đã gửi sản phẩm lên kiểm duyệt thành công!');
                  if (onProductCreated) onProductCreated();
                }}
                className="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white text-xs font-bold rounded-xl shadow-md"
              >
                Gửi duyệt sản phẩm
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================== */}
      {/* MODAL: CHỈNH SỬA SẢN PHẨM */}
      {/* ========================================================== */}
      {showEditModal && editingProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 my-8 space-y-5 animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-bold text-base text-slate-900">Chỉnh Sửa Thông Tin Sản Phẩm</h3>
              <button onClick={() => setShowEditModal(false)} className="p-1 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1 text-xs">
              <div>
                <label className="font-bold text-slate-700">Tên sản phẩm:</label>
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-slate-900 outline-none"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Mô tả chi tiết:</label>
                <textarea
                  rows={4}
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-slate-900 outline-none"
                />
              </div>

              {/* Ảnh minh họa trong mô tả */}
              <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200 space-y-2">
                <label className="font-bold text-slate-800">Ảnh minh họa chi tiết:</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={editNewDescImageUrl}
                    onChange={(e) => setEditNewDescImageUrl(e.target.value)}
                    placeholder="Thêm link ảnh minh họa..."
                    className="flex-1 px-3 py-2 border border-slate-300 rounded-xl"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      if (editNewDescImageUrl.trim()) {
                        setEditDescriptionImages([...editDescriptionImages, editNewDescImageUrl.trim()]);
                        setEditNewDescImageUrl('');
                      }
                    }}
                    className="px-3 py-2 bg-slate-900 text-white rounded-xl font-bold"
                  >
                    Thêm
                  </button>
                </div>
                <div className="flex flex-wrap gap-2 pt-2">
                  {editDescriptionImages.map((img, i) => (
                    <div key={i} className="relative group w-16 h-16 rounded-xl overflow-hidden border border-slate-300">
                      <img src={img} alt="" className="w-full h-full object-cover" />
                      <button
                        type="button"
                        onClick={() => setEditDescriptionImages(editDescriptionImages.filter((_, idx) => idx !== i))}
                        className="absolute inset-0 bg-rose-900/60 text-white opacity-0 group-hover:opacity-100 flex items-center justify-center"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowEditModal(false)}
                className="px-4 py-2.5 bg-slate-100 text-slate-700 font-bold rounded-xl"
              >
                Đóng
              </button>
              <button
                type="button"
                onClick={() => {
                  const updated = products.map((p) =>
                    p.id === editingId
                      ? {
                          ...p,
                          name: editName,
                          description: editDescription,
                          descriptionImages: editDescriptionImages,
                        }
                      : p
                  );
                  setProducts(updated);
                  localStorage.setItem('quickshop_seller_products', JSON.stringify(updated));
                  setShowEditModal(false);
                  showToast('Đã lưu thay đổi thông tin sản phẩm thành công!');
                }}
                className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl shadow-md"
              >
                Lưu cập nhật
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================== */}
      {/* MODAL: IN PHIẾU ĐÓNG GÓI & GIAO HÀNG (PACKING SLIP) */}
      {/* ========================================================== */}
      {selectedOrderForSlip && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-9 h-9 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center">
                  <Printer className="w-5 h-5 stroke-[2.5]" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-slate-900">Phiếu Giao Hàng & Đóng Gói</h3>
                  <p className="text-[11px] text-blue-600 font-mono font-bold">
                    {selectedOrderForSlip.trackingCode || 'GHN-VNM-READY'}
                  </p>
                </div>
              </div>
              <button onClick={() => setSelectedOrderForSlip(null)} className="p-1 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Printable Area */}
            <div className="p-4 rounded-2xl border-2 border-dashed border-slate-300 space-y-4 text-xs">
              <div className="flex justify-between items-center border-b border-slate-200 pb-3">
                <div>
                  <div className="font-black text-sm text-slate-900">QUICKSHOP LOGISTICS</div>
                  <div className="text-[10px] text-slate-500">Đơn vị: Giao Hàng Nhanh (GHN Express)</div>
                </div>
                <div className="text-right">
                  <div className="font-mono font-black text-xs text-slate-900">{selectedOrderForSlip.orderCode}</div>
                  <div className="text-[10px] text-slate-400">{new Date(selectedOrderForSlip.createdAt).toLocaleDateString('vi-VN')}</div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 pb-3 border-b border-slate-200">
                <div>
                  <span className="text-[10px] font-bold uppercase text-slate-400">Người gửi:</span>
                  <div className="font-bold text-slate-900">TechWorld Official Store</div>
                  <div className="text-[10px] text-slate-500">28 Trần Bình, Cầu Giấy, Hà Nội</div>
                </div>
                <div>
                  <span className="text-[10px] font-bold uppercase text-slate-400">Người nhận:</span>
                  <div className="font-bold text-slate-900">{selectedOrderForSlip.recipientName}</div>
                  <div className="text-[10px] text-slate-500">{selectedOrderForSlip.recipientPhone}</div>
                  <div className="text-[10px] text-slate-500">{selectedOrderForSlip.deliveryAddress}</div>
                </div>
              </div>

              <div className="space-y-1.5">
                <span className="text-[10px] font-bold uppercase text-slate-400">Nội dung hàng hóa:</span>
                {(selectedOrderForSlip.items || []).map((it, idx) => (
                  <div key={idx} className="flex justify-between text-slate-800">
                    <span>{it.productName} ({it.variantName})</span>
                    <span className="font-bold">x{it.quantity}</span>
                  </div>
                ))}
              </div>

              <div className="p-3 bg-slate-50 rounded-xl flex justify-between items-center font-bold">
                <span>Tiền Thu Hộ (COD):</span>
                <span className="text-orange-600 text-sm">
                  {selectedOrderForSlip.paymentStatus === 'PAID' ? '0₫ (Đã thanh toán trước)' : formatVND(selectedOrderForSlip.subTotal)}
                </span>
              </div>
            </div>

            <div className="pt-2 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => window.print()}
                className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl flex items-center gap-1.5"
              >
                <Printer className="w-3.5 h-3.5" />
                <span>In Phiếu Ngay</span>
              </button>
              <button
                type="button"
                onClick={() => handleHandoverShipper(selectedOrderForSlip.id)}
                className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl shadow-md flex items-center gap-1.5"
              >
                <Truck className="w-3.5 h-3.5" />
                <span>Bàn Giao Shipper GHN</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================== */}
      {/* MODAL: RÚT TIỀN VỀ NGÂN HÀNG */}
      {/* ========================================================== */}
      {showWithdrawModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center">
                  <DollarSign className="w-4 h-4 stroke-[3]" />
                </div>
                <h3 className="font-bold text-sm text-slate-900">Rút Tiền Về Ngân Hàng</h3>
              </div>
              <button onClick={() => setShowWithdrawModal(false)} className="p-1 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3 bg-emerald-50 rounded-2xl border border-emerald-100 text-xs text-emerald-900">
              Số dư khả dụng hiện tại: <strong className="text-sm">{formatVND(wallet.availableBalance)}</strong>
            </div>

            <form onSubmit={handleRequestWithdraw} className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-slate-700">Số tiền muốn rút (VNĐ):</label>
                <input
                  type="number"
                  value={withdrawAmount}
                  onChange={(e) => setWithdrawAmount(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl font-bold text-orange-600 text-sm outline-none"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Ngân hàng thụ hưởng:</label>
                <select
                  value={bankName}
                  onChange={(e) => setBankName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl outline-none"
                >
                  <option value="MB Bank - Ngân hàng Quân Đội">MB Bank - Ngân hàng Quân Đội</option>
                  <option value="Vietcombank - Ngoại Thương Việt Nam">Vietcombank - Ngoại Thương Việt Nam</option>
                  <option value="Techcombank - Kỹ Thương Việt Nam">Techcombank - Kỹ Thương Việt Nam</option>
                  <option value="BIDV - Đầu tư & Phát triển">BIDV - Đầu tư & Phát triển</option>
                  <option value="VPBank - Việt Nam Thịnh Vượng">VPBank - Việt Nam Thịnh Vượng</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-slate-700">Số tài khoản ngân hàng:</label>
                <input
                  type="text"
                  value={accountNumber}
                  onChange={(e) => setAccountNumber(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl outline-none font-mono"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Tên chủ tài khoản (Viết hoa không dấu):</label>
                <input
                  type="text"
                  value={accountHolder}
                  onChange={(e) => setAccountHolder(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl outline-none font-bold"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowWithdrawModal(false)}
                  className="px-4 py-2 bg-slate-100 text-slate-700 font-bold rounded-xl"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow-md"
                >
                  Tạo lệnh rút tiền
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
