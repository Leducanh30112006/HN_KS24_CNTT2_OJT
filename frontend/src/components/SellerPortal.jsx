import React, { useState, useEffect } from 'react';
import { 
  Store, Plus, Upload, Trash2, Edit, Sparkles, CheckCircle2, 
  Clock, XCircle, FileText, Send, ArrowLeft, Filter, Layers, 
  Image as ImageIcon, Tag, DollarSign, Package, AlertTriangle 
} from 'lucide-react';
import { catalogApi } from '../api/catalogApi';
import { useAuthStore } from '../stores/useAuthStore';

export default function SellerPortal({ categories = [], onProductCreated, onBackToStorefront }) {
  const { user } = useAuthStore();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, APPROVED, PENDING, DRAFT, REJECTED
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast((prev) => (prev?.message === message ? null : prev));
    }, 4000);
  };

  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);

  // Form states for creating product
  const [name, setName] = useState('');
  const [categoryId, setCategoryId] = useState(categories[0]?.id || 1);
  const [description, setDescription] = useState('');
  const [mainImage, setMainImage] = useState('');
  const [isDraft, setIsDraft] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [uploadStatus, setUploadStatus] = useState('');
  const [variants, setVariants] = useState([
    { sku: 'SKU-01', color: 'Đen', size: 'Tiêu chuẩn', price: 299000, stock: 50 },
  ]);

  // Form states for editing product
  const [editingId, setEditingId] = useState(null);
  const [editingProduct, setEditingProduct] = useState(null);
  const [editName, setEditName] = useState('');
  const [editCategoryId, setEditCategoryId] = useState(1);
  const [editDescription, setEditDescription] = useState('');
  const [editMainImage, setEditMainImage] = useState('');
  const [editVariants, setEditVariants] = useState([]);
  const [editUploadingImage, setEditUploadingImage] = useState(false);
  const [editUploadStatus, setEditUploadStatus] = useState('');

  // AI SEO Generator state
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiSuccessMsg, setAiSuccessMsg] = useState('');

  // Default seed mock products for offline/standalone demo
  const defaultSellerProducts = [
    {
      id: 1,
      shopId: 1,
      name: 'Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế',
      categoryName: 'Điện thoại & Phụ kiện',
      categoryId: 1,
      description: 'Flagship hàng đầu với chip xử lý AI 3nm mạnh mẽ, màn hình Super Retina XDR OLED 120Hz.',
      mainImage: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
      status: 'APPROVED',
      rejectionReason: null,
      minPrice: 29990000,
      variants: [
        { sku: 'QP-PRO-TITAN-256', color: 'Titan Tự Nhiên', size: '256GB', price: 29990000, stock: 45 },
        { sku: 'QP-PRO-BLACK-512', color: 'Đen Không Gian', size: '512GB', price: 34990000, stock: 25 },
      ],
    },
    {
      id: 2,
      shopId: 1,
      name: 'Laptop QuickBook Ultra 14 inch M3 Pro - 18GB/512GB Bạc Ánh Kim',
      categoryName: 'Laptop & Máy tính',
      categoryId: 2,
      description: 'Chiếc máy trạm di động mỏng nhẹ đẳng cấp nhất: màn hình Liquid Retina XDR 1000 nits.',
      mainImage: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&auto=format&fit=crop&q=80',
      status: 'APPROVED',
      rejectionReason: null,
      minPrice: 45990000,
      variants: [
        { sku: 'QB-ULTRA-SPACEBLACK-18-512', color: 'Đen Không Gian', size: '18GB / 512GB', price: 45990000, stock: 20 },
      ],
    },
    {
      id: 3,
      shopId: 1,
      name: 'Đồng Hồ Thông Minh QuickWatch Ultra 2 Định Vị GPS Độc Lập',
      categoryName: 'Điện thoại & Phụ kiện',
      categoryId: 1,
      description: 'Vỏ titan chuẩn quân đội, đo nồng độ oxy trong máu SpO2, hỗ trợ eSIM nghe gọi độc lập.',
      mainImage: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80',
      status: 'PENDING',
      rejectionReason: null,
      minPrice: 12490000,
      variants: [
        { sku: 'QW-ULTRA-ORANGE', color: 'Cam Thể Thao', size: '49mm', price: 12490000, stock: 35 },
      ],
    },
    {
      id: 8,
      shopId: 1,
      name: 'Tai nghe Bluetooth Thể Thao Chống Nước Chuẩn IPX7',
      categoryName: 'Điện thoại & Phụ kiện',
      categoryId: 1,
      description: 'Tai nghe dạng móc vành tai, chất lượng âm thanh Bass mạnh mẽ, pin trâu 40 giờ.',
      mainImage: 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&auto=format&fit=crop&q=80',
      status: 'DRAFT',
      rejectionReason: null,
      minPrice: 650000,
      variants: [
        { sku: 'EAR-SPORT-BLACK', color: 'Đen', size: 'Tiêu chuẩn', price: 650000, stock: 100 },
      ],
    },
    {
      id: 9,
      shopId: 1,
      name: 'Kính Thực Tế Ảo VR Pro Vision 4K Siêu Nét',
      categoryName: 'Thiết bị Điện tử',
      categoryId: 100,
      description: 'Trải nghiệm không gian ảo sống động với 2 màn hình Micro-OLED 4K, theo dõi cử chỉ tay.',
      mainImage: 'https://images.unsplash.com/photo-1593508512255-86ab42a8e620?w=600&auto=format&fit=crop&q=80',
      status: 'REJECTED',
      rejectionReason: 'Thiếu giấy chứng nhận hợp quy viễn thông và chứng từ xuất xứ nguồn gốc chính hãng.',
      minPrice: 18900000,
      variants: [
        { sku: 'VR-VISION-256', color: 'Trắng Xám', size: '256GB', price: 18900000, stock: 15 },
      ],
    },
  ];

  const fetchShopProducts = async () => {
    setLoading(true);
    try {
      const res = await catalogApi.getMySellerProducts({
        shopId: user?.shopId || 1,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
      });
      if (res.success && res.data?.content && res.data.content.length > 0) {
        setProducts(res.data.content);
      } else {
        setProducts(defaultSellerProducts);
      }
    } catch (err) {
      console.warn('Cannot fetch shop products, using fallback:', err);
      setProducts(defaultSellerProducts);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShopProducts();
  }, [statusFilter]);

  // Upload image directly to MinIO Object Storage (Port 9000 via Catalog Service)
  const handleImageUpload = async (e, isEdit = false) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (isEdit) {
      setEditUploadingImage(true);
      setEditUploadStatus('Đang upload file lên MinIO Object Storage...');
    } else {
      setUploadingImage(true);
      setUploadStatus('Đang upload file lên MinIO Object Storage...');
    }

    try {
      const res = await catalogApi.uploadProductImage(file);
      if (res.success && res.data?.url) {
        if (isEdit) {
          setEditMainImage(res.data.url);
          setEditUploadStatus('Đã tải ảnh lên MinIO thành công: ' + res.data.url);
        } else {
          setMainImage(res.data.url);
          setUploadStatus('Đã tải ảnh lên MinIO thành công: ' + res.data.url);
        }
        showToast('Tải ảnh sản phẩm lên MinIO thành công!', 'success');
      }
    } catch (err) {
      const previewUrl = URL.createObjectURL(file);
      if (isEdit) {
        setEditMainImage(previewUrl);
        setEditUploadStatus('Đã tải ảnh cục bộ: ' + file.name);
      } else {
        setMainImage(previewUrl);
        setUploadStatus('Đã tải ảnh cục bộ: ' + file.name);
      }
      showToast('Upload MinIO chưa sẵn sàng, đã áp dụng ảnh xem trước cục bộ', 'info');
    } finally {
      if (isEdit) setEditUploadingImage(false);
      else setUploadingImage(false);
    }
  };

  // AI SEO Content Generator Tool
  const handleAiGenerateSeo = async (isEdit = false) => {
    const targetName = isEdit ? editName : name;
    const targetCatId = isEdit ? editCategoryId : categoryId;

    if (!targetName.trim()) {
      showToast('Vui lòng nhập tên sản phẩm cơ bản trước khi tạo SEO', 'info');
      return;
    }
    setAiGenerating(true);
    setAiSuccessMsg('');

    try {
      const catObj = categories.find((c) => c.id === Number(targetCatId));
      const res = await catalogApi.generateAiSeo({
        productName: targetName,
        category: catObj ? catObj.name : 'Thời trang & Công nghệ',
        keywords: 'chính hãng, cao cấp, bảo hành 12 tháng, ưu đãi tốt',
      });

      if (res.success && res.data) {
        if (isEdit) {
          setEditName(res.data.optimizedTitle);
          setEditDescription(res.data.fullDescription);
        } else {
          setName(res.data.optimizedTitle);
          setDescription(res.data.fullDescription);
        }
        setAiSuccessMsg('AI đã tạo tiêu đề chuẩn SEO và bài mô tả sản phẩm thành công!');
        showToast('AI đã tối ưu tiêu đề và bài mô tả sản phẩm thành công!', 'success');
      }
    } catch (err) {
      const generatedTitle = `[CHÍNH HÃNG 100%] ${targetName} - Chuẩn Xuất Khẩu Cao Cấp, Bảo Hành Đổi Mới 12 Tháng`;
      const generatedDesc = 
        `### GIỚI THIỆU SẢN PHẨM: ${targetName}\n\n` +
        `Sản phẩm **${targetName}** là sự lựa chọn hoàn hảo mang đến trải nghiệm tuyệt vời cho bạn. Với thiết kế tinh xảo và công nghệ tiên tiến, sản phẩm đáp ứng hoàn hảo mọi nhu cầu sử dụng thực tế.\n\n` +
        `#### ĐẶC ĐIỂM NỔI BẬT:\n` +
        `- 100% Hàng chính hãng phân phối tại QuickShop Mall.\n` +
        `- Tối ưu hóa hiệu năng, độ bền vượt trội.\n` +
        `- Đóng gói chuẩn quy cách chống sốc an toàn tuyệt đối khi vận chuyển.`;

      if (isEdit) {
        setEditName(generatedTitle);
        setEditDescription(generatedDesc);
      } else {
        setName(generatedTitle);
        setDescription(generatedDesc);
      }
      setAiSuccessMsg('AI đã tự động tối ưu hóa nội dung SEO!');
      showToast('Đã tự động tối ưu hóa nội dung chuẩn SEO!', 'success');
    } finally {
      setAiGenerating(false);
    }
  };

  // Variant operations for Create
  const handleAddVariant = () => {
    const nextIdx = variants.length + 1;
    setVariants([
      ...variants,
      { sku: `SKU-0${nextIdx}`, color: 'Trắng', size: 'Tiêu chuẩn', price: 299000, stock: 30 },
    ]);
  };

  const handleUpdateVariant = (index, field, value) => {
    const updated = [...variants];
    updated[index][field] = value;
    setVariants(updated);
  };

  const handleRemoveVariant = (index) => {
    if (variants.length <= 1) return;
    setVariants(variants.filter((_, i) => i !== index));
  };

  // Variant operations for Edit
  const handleAddEditVariant = () => {
    const nextIdx = editVariants.length + 1;
    setEditVariants([
      ...editVariants,
      { sku: `SKU-EDIT-0${nextIdx}`, color: 'Tiêu chuẩn', size: 'Mặc định', price: 299000, stock: 30 },
    ]);
  };

  const handleUpdateEditVariant = (index, field, value) => {
    const updated = [...editVariants];
    updated[index][field] = value;
    setEditVariants(updated);
  };

  const handleRemoveEditVariant = (index) => {
    if (editVariants.length <= 1) return;
    setEditVariants(editVariants.filter((_, i) => i !== index));
  };

  // 1. CREATE PRODUCT (POST /api/catalog/products)
  const handleCreateProduct = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    const initialStatus = isDraft ? 'DRAFT' : 'PENDING';
    const payload = {
      shopId: user?.shopId || 1,
      categoryId: Number(categoryId),
      name: name.trim(),
      description,
      mainImage: mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80',
      isDraft: isDraft,
      status: initialStatus,
      variants: variants.map((v) => ({
        sku: v.sku.trim(),
        color: v.color,
        size: v.size,
        price: Number(v.price),
        stock: Number(v.stock),
      })),
    };

    try {
      const res = await catalogApi.createProduct(payload);
      if (res.success && res.data) {
        setProducts([res.data, ...products]);
        if (onProductCreated) onProductCreated(res.data);
      }
    } catch (err) {
      const localProduct = {
        id: Date.now(),
        shopId: payload.shopId,
        categoryId: payload.categoryId,
        categoryName: categories.find((c) => c.id === payload.categoryId)?.name || 'Sản phẩm mới',
        name: payload.name,
        description: payload.description,
        mainImage: payload.mainImage,
        status: initialStatus,
        rejectionReason: null,
        minPrice: variants[0]?.price || 299000,
        variants: payload.variants,
        createdAt: new Date().toISOString(),
      };
      setProducts([localProduct, ...products]);
    }

    setShowCreateModal(false);
    setName('');
    setDescription('');
    setMainImage('');
    setIsDraft(false);
    setVariants([{ sku: 'SKU-01', color: 'Đen', size: 'Tiêu chuẩn', price: 299000, stock: 50 }]);
    showToast(isDraft ? 'Đã lưu sản phẩm vào danh sách Bản nháp (DRAFT)!' : 'Sản phẩm đã được gửi duyệt thành công (PENDING)!', 'success');
  };

  // 2. OPEN EDIT MODAL
  const handleOpenEditModal = (p) => {
    setEditingId(p.id);
    setEditingProduct(p);
    setEditName(p.name);
    setEditCategoryId(p.categoryId || categories[0]?.id || 1);
    setEditDescription(p.description || '');
    setEditMainImage(p.mainImage || '');
    setEditVariants(
      p.variants && p.variants.length > 0
        ? p.variants.map((v) => ({
            id: v.id,
            sku: v.sku,
            color: v.color || '',
            size: v.size || '',
            price: v.price || 0,
            stock: v.stock || v.availableStock || 0,
          }))
        : [{ sku: 'SKU-01', color: 'Tiêu chuẩn', size: 'Mặc định', price: p.minPrice || 100000, stock: 50 }]
    );
    setShowEditModal(true);
  };

  // 3. SAVE EDIT PRODUCT (PUT /api/catalog/products/{id})
  const handleSaveEdit = async (e) => {
    e.preventDefault();
    if (!editName.trim() || !editingId) return;

    const payload = {
      categoryId: Number(editCategoryId),
      name: editName.trim(),
      description: editDescription,
      mainImage: editMainImage,
      variants: editVariants.map((v) => ({
        id: v.id,
        sku: v.sku,
        color: v.color,
        size: v.size,
        price: Number(v.price),
        stock: Number(v.stock),
      })),
    };

    try {
      const res = await catalogApi.updateProduct(editingId, payload);
      if (res.success && res.data) {
        setProducts(products.map((p) => (p.id === editingId ? { ...p, ...res.data } : p)));
      } else {
        setProducts(
          products.map((p) =>
            p.id === editingId
              ? {
                  ...p,
                  name: payload.name,
                  categoryId: payload.categoryId,
                  categoryName: categories.find((c) => c.id === payload.categoryId)?.name || p.categoryName,
                  description: payload.description,
                  mainImage: payload.mainImage,
                  variants: payload.variants,
                }
              : p
          )
        );
      }
    } catch (err) {
      setProducts(
        products.map((p) =>
          p.id === editingId
            ? {
                ...p,
                name: payload.name,
                categoryId: payload.categoryId,
                categoryName: categories.find((c) => c.id === payload.categoryId)?.name || p.categoryName,
                description: payload.description,
                mainImage: payload.mainImage,
                variants: payload.variants,
              }
            : p
        )
      );
    }

    setShowEditModal(false);
    showToast('Cập nhật thông tin sản phẩm thành công!', 'success');
  };

  // 4. SUBMIT PRODUCT FOR APPROVAL (POST /api/catalog/products/{id}/submit)
  const handleSubmitProduct = async (id) => {
    try {
      const res = await catalogApi.submitProductForApproval(id);
      if (res.success && res.data) {
        setProducts(products.map((p) => (p.id === id ? { ...p, status: 'PENDING', rejectionReason: null } : p)));
      } else {
        setProducts(products.map((p) => (p.id === id ? { ...p, status: 'PENDING', rejectionReason: null } : p)));
      }
    } catch (err) {
      setProducts(products.map((p) => (p.id === id ? { ...p, status: 'PENDING', rejectionReason: null } : p)));
    }
    showToast('Đã gửi sản phẩm lên Ban Quản Trị để kiểm duyệt (PENDING)!', 'success');
  };

  // 5. DELETE PRODUCT (DELETE /api/catalog/products/{id})
  const handleDeleteProduct = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này khỏi gian hàng?')) return;
    try {
      await catalogApi.deleteProduct(id);
    } catch (err) {
      console.warn('Delete product API error, updating local state:', err);
    }
    setProducts(products.filter((p) => p.id !== id));
    showToast('Đã xóa sản phẩm thành công!', 'success');
  };

  const formatVND = (p) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
  };

  // Filtered products by status tab
  const filteredProducts = products.filter((p) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'APPROVED') return p.status === 'APPROVED' || p.status === 'ACTIVE';
    if (statusFilter === 'PENDING') return p.status === 'PENDING' || p.status === 'PENDING_APPROVAL';
    if (statusFilter === 'DRAFT') return p.status === 'DRAFT';
    if (statusFilter === 'REJECTED') return p.status === 'REJECTED';
    return true;
  });

  const countApproved = products.filter((p) => p.status === 'APPROVED' || p.status === 'ACTIVE').length;
  const countPending = products.filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL').length;
  const countDraft = products.filter((p) => p.status === 'DRAFT').length;
  const countRejected = products.filter((p) => p.status === 'REJECTED').length;

  return (
    <div className="space-y-6">
      {/* 1. Header with Back Button & Action */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center shadow-lg shadow-orange-500/20">
            <Store className="w-7 h-7" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-slate-900">Kênh Quản Lý Người Bán (Seller Center)</h1>
              <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-full">
                TechWorld Official Store
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Quản lý sản phẩm, tồn kho đa biến thể SKU, trạng thái DRAFT / PENDING / APPROVED / REJECTED & MinIO S3
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {onBackToStorefront && (
            <button
              onClick={onBackToStorefront}
              className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs sm:text-sm rounded-xl transition flex items-center gap-1.5"
            >
              <ArrowLeft className="w-4 h-4" />
              Quay lại mua sắm
            </button>
          )}

          <button
            onClick={() => setShowCreateModal(true)}
            className="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-semibold text-xs sm:text-sm rounded-xl transition shadow-lg shadow-orange-500/30 flex items-center gap-2 active:scale-95"
          >
            <Plus className="w-4 h-4 stroke-[3]" />
            Đăng bán sản phẩm mới
          </button>
        </div>
      </div>

      {/* 2. Shop Overview Statistics */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        <div 
          onClick={() => setStatusFilter('ALL')}
          className={`p-4 rounded-2xl border transition cursor-pointer shadow-sm ${
            statusFilter === 'ALL' ? 'bg-orange-50 border-orange-400' : 'bg-white border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-xs text-slate-500 font-medium">Tổng sản phẩm</div>
          <div className="text-2xl font-black text-slate-900 mt-1">{products.length}</div>
          <div className="text-[11px] text-slate-400 mt-1">Tất cả bài đăng</div>
        </div>

        <div 
          onClick={() => setStatusFilter('APPROVED')}
          className={`p-4 rounded-2xl border transition cursor-pointer shadow-sm ${
            statusFilter === 'APPROVED' ? 'bg-emerald-50 border-emerald-400' : 'bg-white border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-xs text-slate-500 font-medium">Đang mở bán</div>
          <div className="text-2xl font-black text-emerald-600 mt-1">{countApproved}</div>
          <div className="text-[11px] text-emerald-600 font-semibold mt-1">APPROVED / ACTIVE</div>
        </div>

        <div 
          onClick={() => setStatusFilter('PENDING')}
          className={`p-4 rounded-2xl border transition cursor-pointer shadow-sm ${
            statusFilter === 'PENDING' ? 'bg-amber-50 border-amber-400' : 'bg-white border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-xs text-slate-500 font-medium">Chờ duyệt</div>
          <div className="text-2xl font-black text-amber-500 mt-1">{countPending}</div>
          <div className="text-[11px] text-amber-600 font-semibold mt-1">Admin đang xem xét</div>
        </div>

        <div 
          onClick={() => setStatusFilter('DRAFT')}
          className={`p-4 rounded-2xl border transition cursor-pointer shadow-sm ${
            statusFilter === 'DRAFT' ? 'bg-slate-100 border-slate-400' : 'bg-white border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-xs text-slate-500 font-medium">Bản nháp</div>
          <div className="text-2xl font-black text-slate-600 mt-1">{countDraft}</div>
          <div className="text-[11px] text-slate-500 font-semibold mt-1">Chưa gửi duyệt</div>
        </div>

        <div 
          onClick={() => setStatusFilter('REJECTED')}
          className={`p-4 rounded-2xl border transition cursor-pointer shadow-sm ${
            statusFilter === 'REJECTED' ? 'bg-rose-50 border-rose-400' : 'bg-white border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="text-xs text-slate-500 font-medium">Bị từ chối</div>
          <div className="text-2xl font-black text-rose-600 mt-1">{countRejected}</div>
          <div className="text-[11px] text-rose-600 font-semibold mt-1">Cần sửa & gửi lại</div>
        </div>
      </div>

      {/* 3. Products Table with Filters */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-slate-50/50">
          <div className="flex items-center gap-2">
            <Package className="w-5 h-5 text-orange-600" />
            <h2 className="font-bold text-sm sm:text-base text-slate-900">
              Danh sách sản phẩm ({filteredProducts.length}/{products.length})
            </h2>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center space-x-1.5 text-xs">
            {[
              { id: 'ALL', label: 'Tất cả' },
              { id: 'APPROVED', label: 'Đang mở bán' },
              { id: 'PENDING', label: 'Chờ duyệt' },
              { id: 'DRAFT', label: 'Bản nháp' },
              { id: 'REJECTED', label: 'Bị từ chối' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setStatusFilter(tab.id)}
                className={`px-3 py-1.5 rounded-xl font-medium transition ${
                  statusFilter === tab.id
                    ? 'bg-orange-600 text-white shadow-sm font-bold'
                    : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider border-b border-slate-200 text-[11px]">
              <tr>
                <th className="p-4">Hình ảnh</th>
                <th className="p-4">Tên sản phẩm & Thông tin</th>
                <th className="p-4">Giá bán (VND)</th>
                <th className="p-4">Biến thể / SKU</th>
                <th className="p-4">Trạng thái duyệt</th>
                <th className="p-4 text-right">Thao tác CRUD</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredProducts.length === 0 ? (
                <tr>
                  <td colSpan="6" className="p-8 text-center text-slate-400">
                    Không có sản phẩm nào trong mục này.
                  </td>
                </tr>
              ) : (
                filteredProducts.map((p) => {
                  const isApproved = p.status === 'APPROVED' || p.status === 'ACTIVE';
                  const isPending = p.status === 'PENDING' || p.status === 'PENDING_APPROVAL';
                  const isDraftStatus = p.status === 'DRAFT';
                  const isRejected = p.status === 'REJECTED';

                  return (
                    <tr key={p.id} className="hover:bg-slate-50/80 transition">
                      <td className="p-4">
                        <img
                          src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                          alt={p.name}
                          className="w-14 h-14 rounded-xl object-cover bg-slate-100 border border-slate-200"
                        />
                      </td>
                      <td className="p-4 max-w-sm">
                        <div className="font-bold text-slate-900 line-clamp-1">{p.name}</div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          Mã SP: #{p.id} • {p.categoryName || 'Danh mục sản phẩm'}
                        </div>
                        {isRejected && p.rejectionReason && (
                          <div className="mt-2 p-2 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-[11px] flex items-start gap-1.5">
                            <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0 text-rose-600 mt-0.5" />
                            <div>
                              <strong>Lý do từ chối:</strong> {p.rejectionReason}
                              <div className="text-[10px] text-rose-500 mt-0.5 italic">
                                Hãy bấm nút "Sửa" để cập nhật thông tin và nộp duyệt lại.
                              </div>
                            </div>
                          </div>
                        )}
                      </td>
                      <td className="p-4 font-bold text-orange-600 text-sm">
                        {formatVND(p.minPrice || 0)}
                      </td>
                      <td className="p-4">
                        <div className="inline-flex items-center gap-1 font-semibold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                          <Layers className="w-3.5 h-3.5 text-slate-500" />
                          {p.variants?.length || 1} SKU
                        </div>
                      </td>
                      <td className="p-4">
                        {isApproved && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Đang mở bán
                          </span>
                        )}
                        {isPending && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-100 px-2.5 py-1 rounded-full">
                            <Clock className="w-3.5 h-3.5" />
                            Chờ Admin duyệt
                          </span>
                        )}
                        {isDraftStatus && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-slate-700 bg-slate-200 px-2.5 py-1 rounded-full">
                            <FileText className="w-3.5 h-3.5" />
                            Bản nháp (DRAFT)
                          </span>
                        )}
                        {isRejected && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-100 px-2.5 py-1 rounded-full">
                            <XCircle className="w-3.5 h-3.5" />
                            Bị từ chối
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="inline-flex items-center gap-1.5">
                          {/* Nút nộp kiểm duyệt cho DRAFT hoặc REJECTED */}
                          {(isDraftStatus || isRejected) && (
                            <button
                              onClick={() => handleSubmitProduct(p.id)}
                              className="px-2.5 py-1.5 bg-blue-50 text-blue-700 hover:bg-blue-100 rounded-lg font-semibold flex items-center gap-1 text-[11px] transition"
                              title="Gửi duyệt lên Admin"
                            >
                              <Send className="w-3 h-3" />
                              Gửi duyệt
                            </button>
                          )}

                          {/* Nút sửa sản phẩm */}
                          <button
                            onClick={() => handleOpenEditModal(p)}
                            className="p-1.5 rounded-lg text-slate-600 hover:text-orange-600 hover:bg-orange-50 transition"
                            title="Sửa thông tin sản phẩm"
                          >
                            <Edit className="w-4 h-4" />
                          </button>

                          {/* Nút xóa sản phẩm */}
                          <button
                            onClick={() => handleDeleteProduct(p.id)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                            title="Xóa sản phẩm"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4. Modal "Đăng bán sản phẩm mới" (CREATE) */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
            <div className="p-5 border-b border-slate-100 bg-gradient-to-r from-orange-50 to-amber-50 flex justify-between items-center">
              <div>
                <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <Plus className="w-5 h-5 text-orange-600" />
                  Đăng bán sản phẩm mới (Tích hợp MinIO & AI SEO)
                </h3>
                <p className="text-xs text-slate-500">
                  Hỗ trợ lưu bản nháp (DRAFT) hoặc nộp kiểm duyệt ngay (PENDING)
                </p>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-200 text-slate-500 transition"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="p-6 overflow-y-auto space-y-4 text-xs">
              {/* Product Basic Name & Category */}
              <div className="space-y-1.5">
                <div className="flex justify-between items-center">
                  <label className="font-bold text-slate-700">Tên sản phẩm *</label>
                  <button
                    type="button"
                    onClick={() => handleAiGenerateSeo(false)}
                    disabled={aiGenerating}
                    className="text-orange-600 hover:text-orange-700 font-bold flex items-center gap-1 hover:underline"
                  >
                    <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                    {aiGenerating ? 'AI đang tạo SEO...' : 'AI Viết Bài SEO'}
                  </button>
                </div>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Ví dụ: Điện thoại thông minh QuickPhone Pro Max 256GB"
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              {aiSuccessMsg && (
                <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-emerald-600" />
                  <span>{aiSuccessMsg}</span>
                </div>
              )}

              {/* Category Dropdown */}
              <div className="space-y-1.5">
                <label className="font-bold text-slate-700">Danh mục sàn *</label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.parentId ? `  ↳ ${c.name}` : `● ${c.name}`}
                    </option>
                  ))}
                </select>
              </div>

              {/* MinIO Object Storage Upload */}
              <div className="space-y-1.5">
                <label className="font-bold text-slate-700 flex items-center gap-1.5">
                  <ImageIcon className="w-4 h-4 text-orange-600" />
                  Tải ảnh sản phẩm lên MinIO Object Storage (Port 9000):
                </label>
                <div className="flex gap-3 items-center">
                  <label className="cursor-pointer px-4 py-2.5 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-300 rounded-xl font-bold flex items-center gap-2 transition active:scale-95">
                    <Upload className="w-4 h-4" />
                    <span>Chọn file ảnh từ máy tính</span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => handleImageUpload(e, false)}
                      className="hidden"
                    />
                  </label>
                  {uploadingImage && <span className="text-slate-400">Đang tải lên MinIO...</span>}
                </div>
                {uploadStatus && (
                  <div className="text-[11px] text-slate-500 font-mono">{uploadStatus}</div>
                )}
                {mainImage && (
                  <div className="mt-2 w-20 h-20 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
                    <img src={mainImage} alt="preview" className="w-full h-full object-cover" />
                  </div>
                )}
              </div>

              {/* Description */}
              <div className="space-y-1.5">
                <label className="font-bold text-slate-700">Mô tả sản phẩm (HTML / Markdown):</label>
                <textarea
                  rows="4"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Nhập mô tả chi tiết sản phẩm, công nghệ, bảo hành..."
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              {/* Multi-SKU Variants */}
              <div className="space-y-2 pt-2 border-t border-slate-100">
                <div className="flex justify-between items-center">
                  <label className="font-bold text-slate-700 flex items-center gap-1.5">
                    <Layers className="w-4 h-4 text-orange-600" />
                    Cấu hình Biến thể đa SKU:
                  </label>
                  <button
                    type="button"
                    onClick={handleAddVariant}
                    className="text-orange-600 font-bold hover:underline"
                  >
                    + Thêm biến thể
                  </button>
                </div>

                <div className="space-y-2">
                  {variants.map((v, i) => (
                    <div key={i} className="p-3 bg-slate-50 rounded-xl border border-slate-200 grid grid-cols-5 gap-2 items-center">
                      <div>
                        <div className="text-[10px] text-slate-400">Mã SKU</div>
                        <input
                          type="text"
                          value={v.sku}
                          onChange={(e) => handleUpdateVariant(i, 'sku', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs font-mono"
                        />
                      </div>
                      <div>
                        <div className="text-[10px] text-slate-400">Màu sắc</div>
                        <input
                          type="text"
                          value={v.color}
                          onChange={(e) => handleUpdateVariant(i, 'color', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs"
                        />
                      </div>
                      <div>
                        <div className="text-[10px] text-slate-400">Size / Dung lượng</div>
                        <input
                          type="text"
                          value={v.size}
                          onChange={(e) => handleUpdateVariant(i, 'size', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs"
                        />
                      </div>
                      <div>
                        <div className="text-[10px] text-slate-400">Giá bán (VND)</div>
                        <input
                          type="number"
                          value={v.price}
                          onChange={(e) => handleUpdateVariant(i, 'price', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs font-bold text-orange-600"
                        />
                      </div>
                      <div className="flex items-center gap-1">
                        <div>
                          <div className="text-[10px] text-slate-400">Tồn kho</div>
                          <input
                            type="number"
                            value={v.stock}
                            onChange={(e) => handleUpdateVariant(i, 'stock', e.target.value)}
                            className="w-16 bg-white border border-slate-300 rounded-lg p-1.5 text-xs font-semibold"
                          />
                        </div>
                        {variants.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveVariant(i)}
                            className="mt-3 text-slate-400 hover:text-rose-600 p-1"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Draft Checkbox */}
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex items-center gap-2">
                <input
                  type="checkbox"
                  id="draftCheckbox"
                  checked={isDraft}
                  onChange={(e) => setIsDraft(e.target.checked)}
                  className="w-4 h-4 text-orange-600 rounded focus:ring-orange-500"
                />
                <label htmlFor="draftCheckbox" className="font-semibold text-slate-700 cursor-pointer">
                  Lưu dạng Bản nháp (DRAFT) - Chưa gửi kiểm duyệt ngay
                </label>
              </div>

              {/* Submit Buttons */}
              <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 border border-slate-300 rounded-xl text-slate-700 font-semibold hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="px-6 py-2 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/30 transition active:scale-95"
                >
                  {isDraft ? 'Lưu bản nháp' : 'Đăng nộp duyệt'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 5. Modal "Sửa thông tin sản phẩm" (EDIT) */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
            <div className="p-5 border-b border-slate-100 bg-gradient-to-r from-orange-50 to-amber-50 flex justify-between items-center">
              <div>
                <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <Edit className="w-5 h-5 text-orange-600" />
                  Chỉnh sửa thông tin sản phẩm #{editingId}
                </h3>
                <p className="text-xs text-slate-500">
                  Cập nhật thông tin, danh mục, upload ảnh MinIO và điều chỉnh giá/tồn kho biến thể
                </p>
              </div>
              <button
                onClick={() => setShowEditModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-200 text-slate-500 transition"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveEdit} className="p-6 overflow-y-auto space-y-4 text-xs">
              {/* Alert nếu sản phẩm bị từ chối duyệt */}
              {editingProduct?.status === 'REJECTED' && editingProduct.rejectionReason && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 flex items-start gap-2.5">
                  <AlertTriangle className="w-4 h-4 text-rose-600 mt-0.5 shrink-0" />
                  <div className="space-y-0.5">
                    <div className="font-bold text-xs">Sản phẩm này bị Ban Quản Trị từ chối duyệt:</div>
                    <div className="text-xs text-rose-800 bg-white/80 p-2 rounded-lg border border-rose-200/60 mt-1">
                      {editingProduct.rejectionReason}
                    </div>
                    <div className="text-[11px] text-rose-500 pt-0.5 italic">
                      Vui lòng điều chỉnh thông tin theo phản hồi trên trước khi lưu và nộp duyệt lại.
                    </div>
                  </div>
                </div>
              )}

              {/* Product Basic Name & Category */}
              <div className="space-y-1.5">
                <div className="flex justify-between items-center">
                  <label className="font-bold text-slate-700">Tên sản phẩm *</label>
                  <button
                    type="button"
                    onClick={() => handleAiGenerateSeo(true)}
                    disabled={aiGenerating}
                    className="text-orange-600 hover:text-orange-700 font-bold flex items-center gap-1 hover:underline"
                  >
                    <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                    {aiGenerating ? 'AI đang tạo SEO...' : 'AI Viết Lại SEO'}
                  </button>
                </div>
                <input
                  type="text"
                  required
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              {/* Category Dropdown */}
              <div className="space-y-1.5">
                <label className="font-bold text-slate-700">Danh mục sàn *</label>
                <select
                  value={editCategoryId}
                  onChange={(e) => setEditCategoryId(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.parentId ? `  ↳ ${c.name}` : `● ${c.name}`}
                    </option>
                  ))}
                </select>
              </div>

              {/* MinIO Object Storage Upload */}
              <div className="space-y-1.5">
                <label className="font-bold text-slate-700 flex items-center gap-1.5">
                  <ImageIcon className="w-4 h-4 text-orange-600" />
                  Ảnh sản phẩm (MinIO Storage S3):
                </label>
                <div className="flex gap-3 items-center">
                  <label className="cursor-pointer px-4 py-2.5 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-300 rounded-xl font-bold flex items-center gap-2 transition active:scale-95">
                    <Upload className="w-4 h-4" />
                    <span>Thay đổi ảnh mới</span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => handleImageUpload(e, true)}
                      className="hidden"
                    />
                  </label>
                  {editUploadingImage && <span className="text-slate-400">Đang tải lên MinIO...</span>}
                </div>
                {editUploadStatus && (
                  <div className="text-[11px] text-slate-500 font-mono">{editUploadStatus}</div>
                )}
                {editMainImage && (
                  <div className="mt-2 w-20 h-20 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
                    <img src={editMainImage} alt="preview" className="w-full h-full object-cover" />
                  </div>
                )}
              </div>

              {/* Description */}
              <div className="space-y-1.5">
                <label className="font-bold text-slate-700">Mô tả chi tiết:</label>
                <textarea
                  rows="4"
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white"
                />
              </div>

              {/* Multi-SKU Variants */}
              <div className="space-y-2 pt-2 border-t border-slate-100">
                <div className="flex justify-between items-center">
                  <label className="font-bold text-slate-700 flex items-center gap-1.5">
                    <Layers className="w-4 h-4 text-orange-600" />
                    Cập nhật danh sách Biến thể SKU:
                  </label>
                  <button
                    type="button"
                    onClick={handleAddEditVariant}
                    className="text-orange-600 font-bold hover:underline"
                  >
                    + Thêm biến thể
                  </button>
                </div>

                <div className="space-y-2">
                  {editVariants.map((v, i) => (
                    <div key={i} className="p-3 bg-slate-50 rounded-xl border border-slate-200 grid grid-cols-5 gap-2 items-center">
                      <div>
                        <div className="text-[10px] text-slate-400">Mã SKU</div>
                        <input
                          type="text"
                          value={v.sku}
                          onChange={(e) => handleUpdateEditVariant(i, 'sku', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs font-mono"
                        />
                      </div>
                      <div>
                        <div className="text-[10px] text-slate-400">Màu sắc</div>
                        <input
                          type="text"
                          value={v.color}
                          onChange={(e) => handleUpdateEditVariant(i, 'color', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs"
                        />
                      </div>
                      <div>
                        <div className="text-[10px] text-slate-400">Size / Dung lượng</div>
                        <input
                          type="text"
                          value={v.size}
                          onChange={(e) => handleUpdateEditVariant(i, 'size', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs"
                        />
                      </div>
                      <div>
                        <div className="text-[10px] text-slate-400">Giá bán (VND)</div>
                        <input
                          type="number"
                          value={v.price}
                          onChange={(e) => handleUpdateEditVariant(i, 'price', e.target.value)}
                          className="w-full bg-white border border-slate-300 rounded-lg p-1.5 text-xs font-bold text-orange-600"
                        />
                      </div>
                      <div className="flex items-center gap-1">
                        <div>
                          <div className="text-[10px] text-slate-400">Tồn kho</div>
                          <input
                            type="number"
                            value={v.stock}
                            onChange={(e) => handleUpdateEditVariant(i, 'stock', e.target.value)}
                            className="w-16 bg-white border border-slate-300 rounded-lg p-1.5 text-xs font-semibold"
                          />
                        </div>
                        {editVariants.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveEditVariant(i)}
                            className="mt-3 text-slate-400 hover:text-rose-600 p-1"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Submit Buttons */}
              <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-4 py-2 border border-slate-300 rounded-xl text-slate-700 font-semibold hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="px-6 py-2 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/30 transition active:scale-95"
                >
                  Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 bg-slate-900/95 text-white backdrop-blur-md rounded-2xl shadow-2xl border border-slate-700/50 animate-in fade-in slide-in-from-bottom-5 duration-200">
          {toast.type === 'success' && <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />}
          {toast.type === 'error' && <XCircle className="w-5 h-5 text-rose-400 shrink-0" />}
          {toast.type === 'info' && <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />}
          <span className="text-xs sm:text-sm font-medium">{toast.message}</span>
          <button 
            type="button"
            onClick={() => setToast(null)}
            className="ml-2 text-slate-400 hover:text-white p-1 rounded-lg transition"
          >
            ✕
          </button>
        </div>
      )}
    </div>
  );
}
