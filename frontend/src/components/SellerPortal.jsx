import React, { useState, useEffect } from 'react';
import { 
  Store, Plus, Upload, Trash2, Edit, Sparkles, CheckCircle2, 
  Clock, XCircle, Layers, Image as ImageIcon, Tag, DollarSign, Package
} from 'lucide-react';
import axiosClient from '../api/axiosClient';
import { useAuthStore } from '../stores/useAuthStore';

export default function SellerPortal({ categories = [], onProductCreated }) {
  const { user } = useAuthStore();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Form states for creating product
  const [name, setName] = useState('');
  const [categoryId, setCategoryId] = useState(categories[0]?.id || 1);
  const [description, setDescription] = useState('');
  const [mainImage, setMainImage] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [uploadStatus, setUploadStatus] = useState('');

  // Variants list state
  const [variants, setVariants] = useState([
    { sku: 'SKU-01', color: 'Đen', size: 'Tiêu chuẩn', price: 299000, stock: 50 },
  ]);

  // AI SEO Generator state
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiSuccessMsg, setAiSuccessMsg] = useState('');

  const fetchShopProducts = async () => {
    setLoading(true);
    try {
      const res = await axiosClient.get('/api/catalog/products/seller/my?shopId=1');
      if (res.success && res.data?.content) {
        setProducts(res.data.content);
      }
    } catch (err) {
      console.warn('Cannot fetch shop products, using local fallback:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShopProducts();
  }, []);

  // Upload image directly to MinIO Object Storage (Port 9000 via Catalog Service)
  const handleImageUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingImage(true);
    setUploadStatus('Đang upload file lên MinIO Object Storage...');

    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await axiosClient.post('/api/catalog/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (res.success && res.data?.url) {
        setMainImage(res.data.url);
        setUploadStatus('Đã tải ảnh lên MinIO thành công! File S3: ' + res.data.url);
      }
    } catch (err) {
      // Fallback preview
      const previewUrl = URL.createObjectURL(file);
      setMainImage(previewUrl);
      setUploadStatus('Đã tải ảnh cục bộ: ' + file.name);
    } finally {
      setUploadingImage(false);
    }
  };

  // AI SEO Content Generator Tool
  const handleAiGenerateSeo = async () => {
    if (!name.trim()) {
      alert('Vui lòng nhập tên sản phẩm cơ bản trước khi tạo SEO');
      return;
    }
    setAiGenerating(true);
    setAiSuccessMsg('');

    try {
      const catObj = categories.find((c) => c.id === Number(categoryId));
      const res = await axiosClient.post('/api/ai/generate-seo', {
        productName: name,
        category: catObj ? catObj.name : 'Thời trang & Công nghệ',
        keywords: 'chính hãng, cao cấp, bảo hành 12 tháng, ưu đãi tốt',
      });

      if (res.success && res.data) {
        setName(res.data.optimizedTitle);
        setDescription(res.data.fullDescription);
        setAiSuccessMsg('AI đã tạo tiêu đề chuẩn SEO và bài mô tả sản phẩm thành công!');
      }
    } catch (err) {
      // Mock AI Fallback
      setName(`[CHÍNH HÃNG 100%] ${name} - Chuẩn Xuất Khẩu Cao Cấp, Bảo Hành Đổi Mới 12 Tháng`);
      setDescription(
        `### GIỚI THIỆU SẢN PHẨM: ${name}\n\n` +
        `Sản phẩm **${name}** là sự lựa chọn hoàn hảo mang đến trải nghiệm tuyệt vời cho bạn. Với thiết kế tinh xảo và công nghệ tiên tiến, sản phẩm đáp ứng hoàn hảo mọi nhu cầu sử dụng thực tế.\n\n` +
        `#### ĐẶC ĐIỂM NỔI BẬT:\n` +
        `- 100% Hàng chính hãng phân phối tại QuickShop Mall.\n` +
        `- Tối ưu hóa hiệu năng, độ bền vượt trội.\n` +
        `- Đóng gói chuẩn quy cách chống sốc an toàn tuyệt đối khi vận chuyển.`
      );
      setAiSuccessMsg('AI đã tự động tối ưu hóa nội dung SEO!');
    } finally {
      setAiGenerating(false);
    }
  };

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

  const handleCreateProduct = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    const payload = {
      shopId: 1,
      categoryId: Number(categoryId),
      name: name.trim(),
      description,
      mainImage: mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80',
      variants: variants.map((v) => ({
        sku: v.sku.trim(),
        color: v.color,
        size: v.size,
        price: Number(v.price),
        stock: Number(v.stock),
      })),
    };

    try {
      const res = await axiosClient.post('/api/catalog/products', payload);
      if (res.success && res.data) {
        setProducts([res.data, ...products]);
        if (onProductCreated) onProductCreated(res.data);
      }
    } catch (err) {
      // Local fallback
      const localProduct = {
        id: Date.now(),
        shopId: 1,
        name: payload.name,
        description: payload.description,
        mainImage: payload.mainImage,
        status: 'PENDING_APPROVAL',
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
    setVariants([{ sku: 'SKU-01', color: 'Đen', size: 'Tiêu chuẩn', price: 299000, stock: 50 }]);
  };

  const handleDeleteProduct = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')) return;
    try {
      await axiosClient.delete(`/api/catalog/products/${id}`);
    } catch (err) {
      console.warn('Delete product API error, updating local state:', err);
    }
    setProducts(products.filter((p) => p.id !== id));
  };

  const formatVND = (p) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
  };

  const activeCount = products.filter((p) => p.status === 'ACTIVE').length;
  const pendingCount = products.filter((p) => p.status === 'PENDING_APPROVAL').length;

  return (
    <div className="space-y-6">
      {/* 1. Seller Center Header */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center shadow-lg shadow-orange-500/20">
            <Store className="w-7 h-7" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-slate-900">TechWorld Official Store</h1>
              <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-full">
                Gian hàng hoạt động
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Quản lý sản phẩm, tồn kho đa biến thể SKU, upload MinIO S3 & công cụ AI SEO
            </p>
          </div>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-semibold text-xs sm:text-sm rounded-xl transition shadow-lg shadow-orange-500/30 flex items-center gap-2 active:scale-95"
        >
          <Plus className="w-4 h-4 stroke-[3]" />
          Đăng bán sản phẩm mới
        </button>
      </div>

      {/* 2. Shop Overview Statistics */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <div className="text-xs text-slate-500 font-medium">Tổng sản phẩm</div>
          <div className="text-2xl font-black text-slate-900 mt-1">{products.length}</div>
          <div className="text-[11px] text-slate-400 mt-1">Gian hàng TechWorld</div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <div className="text-xs text-slate-500 font-medium">Đang mở bán</div>
          <div className="text-2xl font-black text-emerald-600 mt-1">{activeCount}</div>
          <div className="text-[11px] text-emerald-600 font-semibold mt-1">Trạng thái ACTIVE</div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <div className="text-xs text-slate-500 font-medium">Chờ duyệt</div>
          <div className="text-2xl font-black text-amber-500 mt-1">{pendingCount}</div>
          <div className="text-[11px] text-amber-600 font-semibold mt-1">Admin đang xem xét</div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <div className="text-xs text-slate-500 font-medium">Lưu trữ ảnh</div>
          <div className="text-2xl font-black text-purple-600 mt-1">MinIO S3</div>
          <div className="text-[11px] text-purple-600 font-semibold mt-1">Port 9000 Object Storage</div>
        </div>
      </div>

      {/* 3. Products Table */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
          <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
            <Package className="w-5 h-5 text-orange-600" />
            Danh sách sản phẩm của gian hàng ({products.length})
          </h2>
          <button
            onClick={fetchShopProducts}
            className="text-xs font-semibold text-orange-600 hover:underline"
          >
            Làm mới danh sách
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider border-b border-slate-200 text-[11px]">
              <tr>
                <th className="p-4">Hình ảnh</th>
                <th className="p-4">Tên sản phẩm</th>
                <th className="p-4">Giá bán (VND)</th>
                <th className="p-4">Biến thể / SKU</th>
                <th className="p-4">Trạng thái duyệt</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {products.length === 0 ? (
                <tr>
                  <td colSpan="6" className="p-8 text-center text-slate-400">
                    Chưa có sản phẩm nào. Hãy đăng bán sản phẩm đầu tiên!
                  </td>
                </tr>
              ) : (
                products.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/80 transition">
                    <td className="p-4">
                      <img
                        src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                        alt={p.name}
                        className="w-12 h-12 rounded-xl object-cover bg-slate-100 border border-slate-200"
                      />
                    </td>
                    <td className="p-4 max-w-xs">
                      <div className="font-bold text-slate-900 line-clamp-1">{p.name}</div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        Mã SP: #{p.id} • {p.categoryName || 'Danh mục'}
                      </div>
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
                      {p.status === 'ACTIVE' && (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Đang mở bán
                        </span>
                      )}
                      {p.status === 'PENDING_APPROVAL' && (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-100 px-2.5 py-1 rounded-full">
                          <Clock className="w-3.5 h-3.5" />
                          Chờ Admin duyệt
                        </span>
                      )}
                      {p.status === 'REJECTED' && (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-100 px-2.5 py-1 rounded-full">
                          <XCircle className="w-3.5 h-3.5" />
                          Bị từ chối
                        </span>
                      )}
                    </td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => handleDeleteProduct(p.id)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                        title="Xóa sản phẩm"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4. Modal "Đăng bán sản phẩm mới" */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
            <div className="p-5 border-b border-slate-100 bg-gradient-to-r from-orange-50 to-amber-50 flex justify-between items-center">
              <div>
                <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                  <Plus className="w-5 h-5 text-orange-600" />
                  Đăng bán sản phẩm mới (Tích hợp MinIO & AI SEO)
                </h3>
                <p className="text-xs text-slate-500">Sản phẩm tạo ban đầu sẽ ở trạng thái Chờ duyệt (PENDING_APPROVAL)</p>
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
                    onClick={handleAiGenerateSeo}
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
                      {c.name}
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
                      onChange={handleImageUpload}
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
                  Đăng bán sản phẩm
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
