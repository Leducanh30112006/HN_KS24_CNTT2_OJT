import React, { useState, useEffect, useMemo } from 'react';
import { 
  Package, Search, Truck, CheckCircle2, Clock, XCircle, RotateCcw, 
  ChevronRight, Phone, MapPin, Eye, ArrowLeft, AlertCircle, 
  Sparkles, ExternalLink, RefreshCw, ShoppingCart, ShieldCheck,
  Star, MessageSquare, Image as ImageIcon, Check, Box, Calendar, 
  CreditCard, Info, AlertTriangle, Layers
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { fixMojibakeText } from '../utils/vietnameseEncoding';
import { 
  getStoredOrders, 
  getStoredShipments, 
  cancelOrderById, 
  simulateNextCarrierStepInStore,
  saveStoredOrders,
  createCustomerReviewInStore,
  submitCustomerRefundRequest
} from '../data/sampleOrders';
import axiosClient from '../api/axiosClient';
import { AnimatedPresence, AnimatedNumber } from './motion/MotionPrimitives';

export default function OrderHistoryPage({ onBackToHome, onNavigateToProduct, onBuyAgain }) {
  const { user } = useAuthStore();
  const { addToCart } = useCartStore();

  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL' | 'PENDING' | 'PROCESSING' | 'DELIVERING' | 'COMPLETED' | 'CANCELLED'
  const [searchQuery, setSearchQuery] = useState('');
  const [orders, setOrders] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(false);

  // Thông báo người dùng (User notification toast/banner)
  const [notificationAlert, setNotificationAlert] = useState(null); // { type: 'success' | 'error' | 'info', title, message }

  // Modal chi tiết đơn hàng (Order Details Modal)
  const [selectedOrderDetails, setSelectedOrderDetails] = useState(null);

  // Modal tracking shipper
  const [selectedShipment, setSelectedShipment] = useState(null);
  const [isSimulating, setIsSimulating] = useState(false);
  const [trackingModalOpen, setTrackingModalOpen] = useState(false);

  // Cancel order modal
  const [orderToCancel, setOrderToCancel] = useState(null);
  const [cancelReason, setCancelReason] = useState('Đổi ý muốn mua sản phẩm khác');
  const [isCancelling, setIsCancelling] = useState(false);

  // Review modal
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewTargetItem, setReviewTargetItem] = useState(null);
  const [reviewTargetSubOrder, setReviewTargetSubOrder] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewedItems, setReviewedItems] = useState({});

  // Refund modal
  const [refundModalOpen, setRefundModalOpen] = useState(false);
  const [refundTargetSubOrder, setRefundTargetSubOrder] = useState(null);
  const [refundTargetOrder, setRefundTargetOrder] = useState(null);
  const [refundReason, setRefundReason] = useState('Sản phẩm nhận được bị lỗi kỹ thuật / không đúng mô tả');
  const [refundProofUrl, setRefundProofUrl] = useState('');

  const formatVND = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  };

  /**
   * Load dữ liệu đơn hàng: Tích hợp thông minh và đồng bộ 2 chiều giữa Backend API và LocalStorage
   * Đảm bảo MỌI đơn hàng đã đặt, đang chờ duyệt, đang giao hoặc đã hủy đều hiển thị trọn vẹn
   */
  const loadData = async () => {
    setLoading(true);
    const localOrders = getStoredOrders();
    const localShipments = getStoredShipments();

    let combinedOrders = [...localOrders];

    try {
      const res = await axiosClient.get('/api/orders', {
        params: { userId: user?.userId || 2 },
      });

      // res.data hoặc res là array
      const backendData = res?.data || (Array.isArray(res) ? res : []);
      if (Array.isArray(backendData) && backendData.length > 0) {
        const orderMap = new Map();

        // 1. Thêm local orders trước
        localOrders.forEach((o) => {
          const key = String(o.orderCode || o.id);
          orderMap.set(key, o);
        });

        // 2. Merge và chuẩn hóa với backend orders
        backendData.forEach((bo) => {
          const key = String(bo.orderCode || bo.id);
          const existing = orderMap.get(key) || {};

          // SubOrders với snapshot sản phẩm
          let subOrders = bo.subOrders && bo.subOrders.length > 0 ? bo.subOrders : existing.subOrders;
          if (!subOrders || subOrders.length === 0) {
            subOrders = [
              {
                id: (bo.id || 100) * 10,
                shopId: 1,
                shopName: 'TechWorld Official Store',
                shopAvatar: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
                status: bo.status || 'CONFIRMED',
                subTotal: bo.totalAmount,
                shippingFee: bo.shippingFee || 25000,
                carrierName: 'Giao Hàng Nhanh (GHN)',
                trackingCode: `GHN-VNM-${100000 + (bo.id || 1)}`,
                items: [
                  {
                    id: (bo.id || 100) * 100 + 1,
                    productId: 1,
                    productName: 'Sản phẩm QuickShop Mall chính hãng',
                    variantName: 'Bản Tiêu Chuẩn',
                    price: bo.totalAmount || 1000000,
                    quantity: 1,
                    imageUrl: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=300&auto=format&fit=crop&q=80',
                  },
                ],
              },
            ];
          }

          const merged = {
            ...existing,
            ...bo,
            id: bo.id || existing.id,
            orderCode: bo.orderCode || existing.orderCode || `QS-${100000 + (bo.id || 1)}`,
            userId: bo.userId || existing.userId || 2,
            createdAt: bo.createdAt || existing.createdAt || new Date().toISOString(),
            status: bo.status || existing.status || 'PROCESSING',
            cancelReason: bo.cancelReason || existing.cancelReason || '',
            totalAmount: bo.totalAmount || existing.totalAmount || 0,
            discountAmount: bo.discountAmount || bo.voucherDiscount || existing.discountAmount || 0,
            shippingFee: bo.shippingFee || bo.shippingFeeTotal || existing.shippingFee || 25000,
            finalAmount: bo.finalAmount || bo.totalAmount || existing.finalAmount || 0,
            paymentMethod: bo.paymentMethod || existing.paymentMethod || 'PAYOS',
            paymentStatus: bo.status === 'CANCELLED' ? 'CANCELLED' : (bo.paymentStatus || existing.paymentStatus || 'PAID'),
            recipientName: bo.recipientName || existing.recipientName || user?.fullName || 'Lê Đức Anh',
            recipientPhone: bo.recipientPhone || existing.recipientPhone || '0988 123 456',
            deliveryAddress: bo.deliveryAddress || bo.shippingAddress || existing.deliveryAddress || 'Số 18, Ngõ 45 Cầu Giấy, Hà Nội',
            subOrders,
          };

          orderMap.set(key, merged);
        });

        combinedOrders = Array.from(orderMap.values());
      }
    } catch (err) {
      console.warn('Lỗi load backend orders, fallback hiển thị kho dữ liệu mẫu:', err);
    }

    // Sắp xếp đơn hàng mới nhất lên trên
    combinedOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    setOrders(combinedOrders);
    saveStoredOrders(combinedOrders);
    setShipments(localShipments);
    setLoading(false);
  };

  useEffect(() => {
    void loadData();
  }, [user]);

  /**
   * TÁCH ĐƠN HÀNG THEO TỪNG SHOP ĐỘC LẬP:
   * "ở giỏ hàng là 2 đơn nhưng vào đơn phải tách ra là 2 đơn chứ"
   * Nếu đơn thanh toán gồm nhiều shop (như Gian Hàng 2 và Gian Hàng 5),
   * tách thành các đơn độc lập với mã vận đơn riêng, tiến trình riêng và nút hủy riêng cho từng shop.
   */
  const splitOrders = useMemo(() => {
    const list = [];
    orders.forEach((order) => {
      const subOrders = order.subOrders || [];
      if (subOrders.length === 0) {
        list.push({
          ...order,
          cardKey: String(order.id || order.orderCode),
          displayOrderCode: order.orderCode || `QS-${order.id}`,
          shopName: 'QuickShop Mall',
          shopAvatar: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
          shopCarrierName: 'Giao Hàng Nhanh (GHN)',
          shopTrackingCode: `GHN-VNM-${100000 + (order.id || 1)}`,
          shopStatus: order.status,
          shopItems: order.items || [],
          shopSubTotal: order.totalAmount || order.finalAmount || 0,
          shopShippingFee: order.shippingFee || 0,
          shopTotal: order.finalAmount || order.totalAmount || 0,
          parentOrder: order,
          subOrder: null,
          isSplit: false,
        });
      } else {
        subOrders.forEach((sub, idx) => {
          const shopCode = subOrders.length > 1
            ? `${order.orderCode}-GH${sub.shopId || idx + 1}`
            : (sub.orderCode || order.orderCode || `QS-${order.id}`);

          const sSubTotal = sub.subTotal || (sub.items || []).reduce((sum, it) => sum + (it.price || 0) * (it.quantity || 1), 0);
          const sShipFee = sub.shippingFee ?? 25000;

          list.push({
            ...order,
            cardKey: `${order.id || order.orderCode}_sub_${sub.id || idx}`,
            displayOrderCode: shopCode,
            shopId: sub.shopId,
            shopName: sub.shopName || `Gian Hàng #${sub.shopId || idx + 1}`,
            shopAvatar: sub.shopAvatar || 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
            shopCarrierName: sub.carrierName || 'Giao Hàng Nhanh (GHN)',
            shopTrackingCode: sub.trackingCode || `GHN-VNM-${100000 + (order.id || 1)}`,
            shopStatus: sub.status || order.status,
            shopItems: sub.items || [],
            shopSubTotal: sSubTotal,
            shopShippingFee: sShipFee,
            shopTotal: sSubTotal + sShipFee,
            parentOrder: order,
            subOrder: sub,
            isSplit: subOrders.length > 1,
          });
        });
      }
    });
    return list;
  }, [orders]);

  /**
   * KIỂM TRA ĐIỀU KIỆN ĐƯỢC PHÉP HỦY ĐƠN HÀNG:
   * "Khi shop chưa chuyển trạng thái hàng đã đóng thì vẫn được hủy"
   * Cho phép hủy nếu:
   * - Kiện hàng ở trạng thái: PENDING, PENDING_PAYMENT, PROCESSING, CONFIRMED, CREATED, DRAFT
   * - Chưa chuyển sang PACKING (Đã đóng gói), SHIPPING (Đang vận chuyển), DELIVERED, COMPLETED
   */
  const canCancelOrder = (item) => {
    if (!item) return false;
    const s = (item.shopStatus || item.status || '').toUpperCase();
    if (['CANCELLED', 'COMPLETED', 'DELIVERED', 'REFUNDED', 'PACKING', 'SHIPPING'].includes(s)) return false;
    return ['PENDING', 'PENDING_PAYMENT', 'PROCESSING', 'CONFIRMED', 'CREATED', 'DRAFT'].includes(s);
  };

  /**
   * Thực hiện hủy đơn hàng (hủy kiện hàng của shop cụ thể hoặc toàn bộ đơn)
   */
  const handleConfirmCancelOrder = async () => {
    if (!orderToCancel) return;
    setIsCancelling(true);

    const targetSubOrder = orderToCancel.subOrder;
    const parentOrder = orderToCancel.parentOrder || orderToCancel;
    const orderId = parentOrder.id;
    const displayCode = orderToCancel.displayOrderCode || parentOrder.orderCode;

    try {
      // 1. Gửi yêu cầu hủy lên backend API
      await axiosClient.post(`/api/orders/${orderId}/cancel`, {
        reason: cancelReason,
        subOrderId: targetSubOrder?.id,
      });
    } catch (err) {
      console.warn('Backend cancel endpoint error / offline fallback:', err);
    }

    // 2. Cập nhật localStorage
    cancelOrderById(orderId, cancelReason);

    // 3. Cập nhật state cục bộ
    const updated = orders.map((o) => {
      if (String(o.id) === String(orderId) || o.orderCode === parentOrder.orderCode) {
        if (targetSubOrder) {
          const newSubOrders = (o.subOrders || []).map((sub) =>
            sub.id === targetSubOrder.id ? { ...sub, status: 'CANCELLED', cancelReason } : sub
          );
          const allCancelled = newSubOrders.every((sub) => sub.status === 'CANCELLED');
          return {
            ...o,
            status: allCancelled ? 'CANCELLED' : o.status,
            cancelReason: allCancelled ? cancelReason : o.cancelReason,
            subOrders: newSubOrders,
          };
        } else {
          return {
            ...o,
            status: 'CANCELLED',
            cancelReason,
            paymentStatus: o.paymentStatus === 'PAID' ? 'REFUNDED' : 'CANCELLED',
            subOrders: (o.subOrders || []).map((sub) => ({ ...sub, status: 'CANCELLED' })),
          };
        }
      }
      return o;
    });

    setOrders(updated);
    saveStoredOrders(updated);

    if (selectedOrderDetails && (selectedOrderDetails.displayOrderCode === displayCode || selectedOrderDetails.orderCode === parentOrder.orderCode)) {
      setSelectedOrderDetails({
        ...selectedOrderDetails,
        shopStatus: 'CANCELLED',
        status: 'CANCELLED',
        cancelReason,
      });
    }

    setIsCancelling(false);
    setOrderToCancel(null);

    // 4. Hiển thị thông báo thành công cho người dùng
    setNotificationAlert({
      type: 'success',
      title: 'Đã hủy đơn hàng thành công!',
      message: `Đơn hàng #${displayCode} từ "${orderToCancel.shopName}" đã được hủy thành công với lý do: "${cancelReason}". Bạn có thể xem lại tại tab "Đã hủy".`,
    });
    setTimeout(() => setNotificationAlert(null), 6000);
  };

  // Lọc đơn hàng theo tab và từ khóa trên danh sách đã tách theo Shop
  const filteredOrders = splitOrders.filter((order) => {
    const s = (order.shopStatus || order.status || '').toUpperCase();

    // Tab 'ALL': HIỆN HẾT TẤT CẢ CÁC ĐƠN HÀNG (đã đặt, đã hủy, đang giao...)
    if (activeTab === 'PENDING') {
      if (!['PENDING', 'PENDING_PAYMENT', 'DRAFT', 'CREATED'].includes(s)) return false;
    } else if (activeTab === 'PROCESSING') {
      if (!['PROCESSING', 'PAID', 'CONFIRMED', 'PACKING'].includes(s)) return false;
    } else if (activeTab === 'DELIVERING') {
      if (!['DELIVERING', 'SHIPPING', 'PICKED'].includes(s)) return false;
    } else if (activeTab === 'COMPLETED') {
      if (!['COMPLETED', 'DELIVERED'].includes(s)) return false;
    } else if (activeTab === 'CANCELLED') {
      if (!['CANCELLED', 'REFUNDED'].includes(s)) return false;
    }

    // Lọc theo từ khóa tìm kiếm
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      const codeMatch = (order.displayOrderCode || order.orderCode || String(order.id)).toLowerCase().includes(q);
      const productMatch = (order.shopItems || []).some((it) => (it.productName || '').toLowerCase().includes(q));
      const shopMatch = (order.shopName || '').toLowerCase().includes(q);
      return codeMatch || productMatch || shopMatch;
    }

    return true;
  });

  // Mở modal theo dõi vận chuyển cho đơn hàng
  const handleOpenTracking = (subOrder, order) => {
    const trackingCode = subOrder.trackingCode;
    const currentShipments = getStoredShipments();
    let found = currentShipments.find((s) => s.trackingCode === trackingCode || String(s.subOrderId) === String(subOrder.id));

    if (!found) {
      found = {
        id: Date.now(),
        trackingCode: trackingCode || `GHN-VNM-${Math.floor(100000 + Math.random() * 900000)}`,
        carrierName: subOrder.carrierName || 'Giao Hàng Nhanh (GHN)',
        shippingStatus: subOrder.status === 'COMPLETED' ? 'DELIVERED' : subOrder.status === 'DELIVERING' ? 'DELIVERING' : 'PICKED',
        recipientName: order.recipientName || 'Khách Hàng',
        recipientPhone: order.recipientPhone || '0988 123 456',
        toAddress: order.deliveryAddress,
        shipperName: 'Nguyễn Văn Hùng',
        shipperPhone: '0979 123 456',
        shipperVehicle: '29-D1 567.89 (Honda Wave)',
        codAmount: order.paymentStatus === 'PAID' ? 0 : order.finalAmount,
        timeline: [
          { step: 1, title: 'Đơn hàng đã đặt', description: 'Đã xác nhận đơn hàng thành công', location: 'QuickShop Mall', time: order.createdAt || 'Hôm nay', completed: true },
          { step: 2, title: 'Đã xuất kho', description: 'Bàn giao cho bưu cục vận chuyển', location: 'Kho người bán', time: 'Hôm nay', completed: true },
          { step: 3, title: 'Đang vận chuyển', description: 'Shipper đang trên tuyến giao hàng đến bạn', location: 'Cầu Giấy, Hà Nội', time: 'Hiện tại', completed: true, current: true },
        ],
      };
    }

    setSelectedShipment(found);
    setTrackingModalOpen(true);
  };

  const handleSimulateNextStep = async () => {
    if (!selectedShipment) return;
    setIsSimulating(true);
    try {
      await axiosClient.post(`/api/shipping/simulate-step/${selectedShipment.trackingCode}`);
    } catch {}
    simulateNextCarrierStepInStore(selectedShipment.trackingCode);
    const updatedShipments = getStoredShipments();
    const updated = updatedShipments.find((s) => s.trackingCode === selectedShipment.trackingCode);
    if (updated) setSelectedShipment(updated);
    setOrders(getStoredOrders());
    setIsSimulating(false);
  };

  const handleConfirmReceived = (subOrder, order) => {
    const all = getStoredOrders();
    const updated = all.map((o) => {
      if (String(o.id) === String(order.id) || o.orderCode === order.orderCode) {
        return {
          ...o,
          status: 'COMPLETED',
          subOrders: (o.subOrders || []).map((s) =>
            s.id === subOrder.id ? { ...s, status: 'COMPLETED' } : s
          ),
        };
      }
      return o;
    });
    setOrders(updated);
    saveStoredOrders(updated);
    setNotificationAlert({
      type: 'success',
      title: 'Đã xác nhận nhận hàng thành công!',
      message: 'Cảm ơn bạn đã mua sắm tại QuickShop! Hãy để lại đánh giá để nhận xu thưởng nhé.',
    });
    setTimeout(() => setNotificationAlert(null), 5000);
  };

  const handleBuyAgain = async (item) => {
    await addToCart(
      { id: item.productId, name: item.productName, price: item.price, mainImage: item.imageUrl },
      { variantId: item.variantId, variantName: item.variantName, color: item.color, size: item.size, price: item.price },
      1
    );
    if (onBuyAgain) {
      onBuyAgain();
    } else {
      setNotificationAlert({
        type: 'success',
        title: 'Đã thêm sản phẩm vào giỏ!',
        message: `Đã thêm lại sản phẩm "${item.productName}" vào giỏ hàng của bạn.`,
      });
      setTimeout(() => setNotificationAlert(null), 4000);
    }
  };

  const handleOpenReview = (item, subOrder) => {
    setReviewTargetItem(item);
    setReviewTargetSubOrder(subOrder);
    setReviewRating(5);
    setReviewComment('');
    setReviewModalOpen(true);
  };

  const handleSubmitReview = () => {
    if (!reviewTargetItem) return;
    createCustomerReviewInStore({
      shopId: reviewTargetSubOrder?.shopId || 1,
      productId: reviewTargetItem.productId || 1,
      productName: reviewTargetItem.productName || 'Sản phẩm chính hãng',
      customerName: user?.fullName || 'Khách Hàng',
      rating: reviewRating,
      comment: reviewComment || 'Sản phẩm rất tốt, giao hàng nhanh chóng, đóng gói cẩn thận 5 sao!',
    });
    setReviewedItems((prev) => ({ ...prev, [reviewTargetItem.id || reviewTargetItem.productId]: true }));
    setReviewModalOpen(false);
    setNotificationAlert({
      type: 'success',
      title: 'Đánh giá thành công!',
      message: 'Cảm ơn bạn đã đánh giá sản phẩm. Bạn nhận được 200 Xu thưởng QuickShop!',
    });
    setTimeout(() => setNotificationAlert(null), 5000);
  };

  const handleOpenRefund = (subOrder, order) => {
    setRefundTargetSubOrder(subOrder);
    setRefundTargetOrder(order);
    setRefundReason('Sản phẩm nhận được bị lỗi kỹ thuật / không đúng mô tả');
    setRefundProofUrl('');
    setRefundModalOpen(true);
  };

  const handleSubmitRefund = () => {
    if (!refundTargetSubOrder || !refundTargetOrder) return;
    submitCustomerRefundRequest({
      orderId: refundTargetOrder.orderCode || `MO-${refundTargetOrder.id}`,
      subOrderId: refundTargetSubOrder.id,
      shopId: refundTargetSubOrder.shopId || 1,
      shopName: refundTargetSubOrder.shopName || 'TechWorld Official Store',
      customerName: user?.fullName || refundTargetOrder.recipientName || 'Khách Hàng',
      customerPhone: refundTargetOrder.recipientPhone || '0988 123 456',
      productName: refundTargetSubOrder.items?.[0]?.productName || 'Sản phẩm',
      amount: refundTargetSubOrder.subTotal || refundTargetOrder.finalAmount || 0,
      reason: refundReason,
      proofImages: refundProofUrl ? [refundProofUrl] : [],
    });
    void loadData();
    setRefundModalOpen(false);
    setNotificationAlert({
      type: 'info',
      title: 'Đã gửi yêu cầu Trả hàng / Hoàn tiền!',
      message: 'Người bán sẽ phản hồi yêu cầu của bạn trong vòng 24 giờ. Tiền hoàn được bảo hộ bởi Escrow.',
    });
    setTimeout(() => setNotificationAlert(null), 6000);
  };

  const getStatusBadge = (status) => {
    const s = (status || '').toUpperCase();
    switch (s) {
      case 'PENDING':
      case 'PENDING_PAYMENT':
      case 'DRAFT':
      case 'CREATED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-amber-50 text-amber-700 border border-amber-200/80 rounded-full text-[11px] font-bold">
            <Clock className="w-3 h-3 text-amber-600" />
            <span>Chờ xác nhận</span>
          </span>
        );
      case 'PROCESSING':
      case 'PAID':
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-blue-50 text-blue-700 border border-blue-200/80 rounded-full text-[11px] font-bold">
            <RefreshCw className="w-3 h-3 text-blue-600" />
            <span>Đang chuẩn bị hàng</span>
          </span>
        );
      case 'PACKING':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-indigo-50 text-indigo-700 border border-indigo-200/80 rounded-full text-[11px] font-bold">
            <Box className="w-3 h-3 text-indigo-600" />
            <span>Shop đã đóng gói</span>
          </span>
        );
      case 'DELIVERING':
      case 'SHIPPING':
      case 'PICKED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-orange-50 text-orange-700 border border-orange-200/80 rounded-full text-[11px] font-bold animate-pulse">
            <Truck className="w-3 h-3 text-orange-600" />
            <span>Đang giao hàng</span>
          </span>
        );
      case 'COMPLETED':
      case 'DELIVERED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200/80 rounded-full text-[11px] font-bold">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>Giao thành công</span>
          </span>
        );
      case 'CANCELLED':
      case 'REFUNDED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-rose-50 text-rose-700 border border-rose-200/80 rounded-full text-[11px] font-bold">
            <XCircle className="w-3 h-3 text-rose-600" />
            <span>Đã hủy đơn</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-slate-100 text-slate-700 rounded-full text-[11px] font-medium">
            <span>{status}</span>
          </span>
        );
    }
  };

  return (
    <div className="min-h-screen bg-slate-50/70 py-6 sm:py-8 px-3 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Banner thông báo người dùng nổi bật khi hủy hoặc cập nhật */}
        {notificationAlert && (
          <div
            className={`p-4 rounded-2xl border shadow-md flex items-start justify-between gap-3 transition-all ${
              notificationAlert.type === 'success'
                ? 'bg-emerald-50/95 border-emerald-300 text-emerald-900'
                : notificationAlert.type === 'error'
                ? 'bg-rose-50/95 border-rose-300 text-rose-900'
                : 'bg-blue-50/95 border-blue-300 text-blue-900'
            }`}
          >
            <div className="flex items-start gap-3">
              {notificationAlert.type === 'success' ? (
                <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
              ) : notificationAlert.type === 'error' ? (
                <AlertTriangle className="w-5 h-5 text-rose-600 flex-shrink-0 mt-0.5" />
              ) : (
                <Info className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
              )}
              <div>
                <h4 className="font-bold text-sm">{notificationAlert.title}</h4>
                <p className="text-xs opacity-90 mt-0.5">{notificationAlert.message}</p>
              </div>
            </div>
            <button
              onClick={() => setNotificationAlert(null)}
              className="text-slate-400 hover:text-slate-700 text-xs p-1"
            >
              ✕
            </button>
          </div>
        )}

        {/* Navigation Top Bar */}
        <div className="flex items-center justify-between">
          <button
            onClick={onBackToHome}
            className="inline-flex items-center gap-2 text-xs font-semibold text-slate-600 hover:text-orange-600 transition group cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
            <span>Tiếp tục mua sắm</span>
          </button>
          <div className="text-xs text-slate-400 hidden sm:block">
            Tra cứu đơn hàng trực tiếp với tiến trình vận chuyển bưu tá
          </div>
        </div>

        {/* Page Header */}
        <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center shadow-md shadow-orange-500/20">
              <Package className="w-6 h-6 stroke-[2.5]" />
            </div>
            <div>
              <h1 className="text-lg sm:text-xl font-black text-slate-900 tracking-tight">
                Đơn Mua Của Bạn
              </h1>
              <p className="text-xs text-slate-500">
                Hiển thị toàn bộ đơn hàng đã đặt, theo dõi tiến trình và hủy đơn linh hoạt
              </p>
            </div>
          </div>

          <div className="relative w-full sm:w-80">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo Mã đơn, Sản phẩm, Tên shop..."
              className="w-full pl-9 pr-4 py-2 bg-slate-100 focus:bg-white text-xs rounded-xl border border-slate-200 focus:border-orange-500 outline-none transition"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          </div>
        </div>

        {/* Status Filter Tabs (Shopee Style) */}
        <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm overflow-x-auto no-scrollbar">
          <div className="flex border-b border-slate-100 min-w-max">
            {[
              { id: 'ALL', label: 'Tất cả đơn', count: splitOrders.length },
              {
                id: 'PENDING',
                label: 'Chờ thanh toán / Chờ xác nhận',
                count: splitOrders.filter((o) => ['PENDING', 'PENDING_PAYMENT', 'DRAFT', 'CREATED'].includes((o.shopStatus || o.status || '').toUpperCase())).length,
              },
              {
                id: 'PROCESSING',
                label: 'Đang xử lý / Đóng hàng',
                count: splitOrders.filter((o) => ['PROCESSING', 'PAID', 'CONFIRMED', 'PACKING'].includes((o.shopStatus || o.status || '').toUpperCase())).length,
              },
              {
                id: 'DELIVERING',
                label: 'Đang giao hàng',
                count: splitOrders.filter((o) => ['DELIVERING', 'SHIPPING', 'PICKED'].includes((o.shopStatus || o.status || '').toUpperCase())).length,
              },
              {
                id: 'COMPLETED',
                label: 'Đã giao',
                count: splitOrders.filter((o) => ['COMPLETED', 'DELIVERED'].includes((o.shopStatus || o.status || '').toUpperCase())).length,
              },
              {
                id: 'CANCELLED',
                label: 'Đã hủy',
                count: splitOrders.filter((o) => ['CANCELLED', 'REFUNDED'].includes((o.shopStatus || o.status || '').toUpperCase())).length,
              },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 sm:px-5 py-3.5 text-xs font-bold transition border-b-2 whitespace-nowrap flex items-center gap-1.5 cursor-pointer ${
                  activeTab === tab.id
                    ? 'border-orange-600 text-orange-600 bg-orange-50/40'
                    : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <span>{tab.label}</span>
                {tab.count > 0 && (
                  <span
                    className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                      activeTab === tab.id ? 'bg-orange-600 text-white' : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {tab.count}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Orders List */}
        {loading ? (
          <div className="bg-white rounded-3xl p-12 text-center border border-slate-200/90 space-y-3">
            <RefreshCw className="w-8 h-8 text-orange-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500 font-semibold">Đang đồng bộ danh sách đơn hàng...</p>
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 text-center border border-slate-200/90 space-y-3">
            <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 mx-auto flex items-center justify-center">
              <Package className="w-8 h-8" />
            </div>
            <h3 className="font-bold text-slate-800 text-sm">Chưa có đơn hàng nào ở trạng thái này</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Hãy tiếp tục khám phá hàng ngàn deal giảm giá và voucher miễn phí vận chuyển trên sàn QuickShop!
            </p>
            <button
              onClick={onBackToHome}
              className="mt-2 px-6 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-md transition active:scale-95 cursor-pointer"
            >
              Mua sắm ngay
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredOrders.map((order) => {
              const isCancelable = canCancelOrder(order);

              return (
                <div
                  key={order.cardKey || order.id || order.orderCode}
                  className="bg-white rounded-3xl border border-slate-200/90 shadow-xs hover:border-orange-300 hover:shadow-md transition-all overflow-hidden"
                >
                  {/* Header card: Mã đơn riêng theo từng Shop, Thời gian, PTTT, Badge trạng thái */}
                  <div className="p-4 sm:p-5 bg-slate-50/70 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-2.5 flex-wrap">
                      <span className="font-mono font-black text-xs sm:text-sm text-slate-900 bg-white px-2.5 py-1 rounded-lg border border-slate-200">
                        #{order.displayOrderCode}
                      </span>
                      <span className="text-slate-300">|</span>
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <Calendar className="w-3 h-3 text-slate-400" />
                        <span>{new Date(order.createdAt).toLocaleString('vi-VN')}</span>
                      </span>
                      <span className="text-slate-300">|</span>
                      <span className="text-[11px] font-semibold text-slate-700 flex items-center gap-1">
                        <CreditCard className="w-3 h-3 text-slate-400" />
                        <span>{order.paymentMethod || 'COD'}</span>
                        <span
                          className={`text-[10px] px-1.5 py-0.2 rounded font-bold ${
                            order.paymentStatus === 'PAID'
                              ? 'bg-emerald-100 text-emerald-800'
                              : order.paymentStatus === 'CANCELLED'
                              ? 'bg-slate-100 text-slate-600'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {order.paymentStatus === 'PAID'
                            ? 'Đã thanh toán'
                            : order.paymentStatus === 'CANCELLED'
                            ? 'Đã hủy'
                            : 'Chờ thanh toán'}
                        </span>
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      {getStatusBadge(order.shopStatus || order.status)}
                    </div>
                  </div>

                  {/* Body: Thông tin Shop & Các sản phẩm của riêng Shop này */}
                  <div
                    className="relative p-4 sm:p-5 space-y-3.5 cursor-pointer hover:bg-slate-50/30 transition-colors"
                    title="Bấm vào đơn hàng để xem chi tiết đầy đủ"
                  >
                    <button
                      type="button"
                      onClick={() => setSelectedOrderDetails(order)}
                      aria-label={`Xem chi tiết đơn hàng ${order.orderCode || order.id}`}
                      className="absolute inset-0 z-0 w-full h-full bg-transparent border-0 cursor-pointer"
                    />
                    {/* Shop Header */}
                    <div className="relative z-10 pointer-events-none flex items-center justify-between pb-2 border-b border-slate-100">
                      <div className="flex items-center gap-2">
                        <img
                          src={order.shopAvatar || 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=50&auto=format&fit=crop&q=80'}
                          alt=""
                          className="w-6 h-6 rounded-lg object-cover border border-slate-200"
                        />
                        <span className="font-bold text-xs text-slate-900">{order.shopName}</span>
                        <span className="text-[9px] bg-rose-100 text-rose-700 px-1.5 py-0.2 rounded font-black uppercase">
                          Mall
                        </span>
                      </div>

                      {order.shopTrackingCode && (
                        <div className="flex items-center gap-2 text-xs">
                          <span className="text-slate-400 text-[11px]">Vận đơn:</span>
                          <span className="font-mono font-bold text-blue-600">{order.shopTrackingCode}</span>
                        </div>
                      )}
                    </div>

                    {/* Items list của riêng gian hàng này */}
                    <div className="relative z-10 pointer-events-none space-y-3">
                      {(order.shopItems || []).map((item) => (
                        <div key={item.id} className="flex items-center gap-3.5">
                          <img
                            src={item.imageUrl}
                            alt={item.productName}
                            className="w-16 h-16 rounded-xl object-cover border border-slate-200 flex-shrink-0"
                          />
                          <div className="flex-1 min-w-0">
                            <h4 className="text-xs font-semibold text-slate-900 line-clamp-1">
                              {fixMojibakeText(item.productName)}
                            </h4>
                            <div className="text-[11px] text-slate-500 pt-0.5">
                              Phân loại: {fixMojibakeText(item.variantName) || fixMojibakeText(`${item.color || ''} ${item.size || ''}`) || 'Bản Tiêu Chuẩn'}
                            </div>
                            <div className="text-[11px] text-slate-400">Số lượng: x{item.quantity}</div>
                          </div>
                          <div className="text-right flex-shrink-0 pointer-events-auto">
                            <div className="text-xs font-bold text-slate-900">{formatVND(item.price)}</div>
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                void handleBuyAgain(item);
                              }}
                              className="mt-1 text-[11px] text-orange-600 hover:underline font-semibold flex items-center gap-1 ml-auto cursor-pointer"
                            >
                              <ShoppingCart className="w-3 h-3" />
                              <span>Mua lại</span>
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Footer card: Địa chỉ, Tổng tiền shop, Nút Xem chi tiết & Hủy đơn */}
                  <div className="p-4 sm:p-5 bg-slate-50/60 border-t border-slate-200/80 flex flex-col sm:flex-row items-center justify-between gap-4">
                    <div className="text-xs text-slate-500 text-center sm:text-left space-y-1">
                      <div className="flex items-center gap-1.5 justify-center sm:justify-start">
                        <MapPin className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                        <span>
                          Địa chỉ nhận: <strong className="text-slate-800">{order.deliveryAddress || 'Số 18, Ngõ 45 Cầu Giấy, Hà Nội'}</strong>
                        </span>
                      </div>
                      {order.cancelReason && (
                        <div className="text-rose-600 font-bold flex items-center gap-1 justify-center sm:justify-start">
                          <AlertCircle className="w-3.5 h-3.5" />
                          <span>Lý do hủy: {order.cancelReason}</span>
                        </div>
                      )}
                    </div>

                    <div className="flex items-center gap-3 flex-wrap justify-center sm:justify-end">
                      <div className="text-right mr-2">
                        <span className="text-[11px] text-slate-400">Tổng thanh toán:</span>
                        <div className="text-base sm:text-lg font-black text-orange-600 font-mono">
                          {formatVND(order.shopTotal)}
                        </div>
                      </div>

                      {/* Nút Xem Chi Tiết Đơn Hàng (YÊU CẦU ĐỀ BÀI) */}
                      <button
                        type="button"
                        onClick={() => setSelectedOrderDetails(order)}
                        className="px-3.5 py-2 bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 rounded-xl text-xs font-bold transition active:scale-95 flex items-center gap-1.5 shadow-2xs cursor-pointer"
                      >
                        <Eye className="w-3.5 h-3.5 text-slate-500" />
                        <span>Xem chi tiết</span>
                      </button>

                      {/* Nút Theo dõi vận chuyển */}
                      {!['CANCELLED', 'REFUNDED'].includes((order.shopStatus || order.status || '').toUpperCase()) && (
                        <button
                          type="button"
                          onClick={() => handleOpenTracking(order.subOrder || {}, order.parentOrder || order)}
                          className="px-3.5 py-2 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200 rounded-xl text-xs font-bold transition active:scale-95 flex items-center gap-1.5 shadow-2xs cursor-pointer"
                        >
                          <Truck className="w-3.5 h-3.5 text-orange-600" />
                          <span>Theo dõi bưu tá</span>
                        </button>
                      )}

                      {/* Nút Xác nhận đã nhận hàng */}
                      {['DELIVERING', 'SHIPPING'].includes((order.shopStatus || order.status || '').toUpperCase()) && (
                        <button
                          type="button"
                          onClick={() => handleConfirmReceived(order.subOrder || {}, order.parentOrder || order)}
                          className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition active:scale-95 shadow-sm cursor-pointer"
                        >
                          Đã nhận hàng
                        </button>
                      )}

                      {/* Nút Hủy đơn hàng (YÊU CẦU ĐỀ BÀI: Khi shop chưa đóng hàng thì vẫn được hủy) */}
                      {isCancelable && (
                        <button
                          type="button"
                          onClick={() => setOrderToCancel(order)}
                          className="px-3.5 py-2 border border-rose-300 text-rose-600 hover:bg-rose-50 rounded-xl text-xs font-bold transition active:scale-95 shadow-2xs cursor-pointer"
                        >
                          Hủy đơn hàng
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* MODAL 1: XEM CHI TIẾT ĐƠN HÀNG TOÀN DIỆN (ORDER DETAILS MODAL) */}
      {/* ========================================================================= */}
      <AnimatedPresence show={Boolean(selectedOrderDetails)} mode="modal" dataLenisPrevent={true} className="p-3 sm:p-6 no-scrollbar">
        {selectedOrderDetails && (
          <div
            style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
            className="bg-white rounded-3xl max-w-2xl w-full p-6 sm:p-7 shadow-2xl border border-slate-200 space-y-6 max-h-[90vh] overflow-y-auto overflow-x-hidden no-scrollbar"
          >
            {/* Header Modal */}
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center">
                  <Package className="w-5 h-5 stroke-[2.5]" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-slate-900">Chi Tiết Đơn Hàng</h3>
                  <p className="text-xs font-mono font-bold text-orange-600">
                    Mã đơn: #{selectedOrderDetails.displayOrderCode || selectedOrderDetails.orderCode || selectedOrderDetails.id}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedOrderDetails(null)}
                className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Trạng thái & Timeline Tóm tắt */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500 font-medium">Trạng thái hiện tại:</span>
                <div>{getStatusBadge(selectedOrderDetails.shopStatus || selectedOrderDetails.status)}</div>
              </div>

              {selectedOrderDetails.cancelReason ? (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800 space-y-1">
                  <div className="font-bold flex items-center gap-1.5">
                    <XCircle className="w-4 h-4 text-rose-600" />
                    <span>Đơn hàng đã bị hủy</span>
                  </div>
                  <p className="opacity-90">Lý do hủy đơn: {selectedOrderDetails.cancelReason}</p>
                </div>
              ) : (
                <div className="grid grid-cols-4 gap-2 pt-2 text-center text-[10px]">
                  <div className="space-y-1">
                    <div className="w-6 h-6 rounded-full bg-emerald-500 text-white mx-auto flex items-center justify-center font-bold">
                      ✓
                    </div>
                    <span className="font-bold text-slate-800">Đã đặt đơn</span>
                  </div>
                  <div className="space-y-1">
                    <div className="w-6 h-6 rounded-full bg-emerald-500 text-white mx-auto flex items-center justify-center font-bold">
                      ✓
                    </div>
                    <span className="font-bold text-slate-800">Đã xác nhận</span>
                  </div>
                  <div className="space-y-1">
                    <div
                      className={`w-6 h-6 rounded-full mx-auto flex items-center justify-center font-bold ${
                        ['PACKING', 'DELIVERING', 'SHIPPING', 'COMPLETED'].includes((selectedOrderDetails.status || '').toUpperCase())
                          ? 'bg-emerald-500 text-white'
                          : 'bg-slate-200 text-slate-500'
                      }`}
                    >
                      3
                    </div>
                    <span className="font-bold text-slate-800">Đóng gói</span>
                  </div>
                  <div className="space-y-1">
                    <div
                      className={`w-6 h-6 rounded-full mx-auto flex items-center justify-center font-bold ${
                        ['DELIVERING', 'SHIPPING', 'COMPLETED'].includes((selectedOrderDetails.status || '').toUpperCase())
                          ? 'bg-emerald-500 text-white'
                          : 'bg-slate-200 text-slate-500'
                      }`}
                    >
                      4
                    </div>
                    <span className="font-bold text-slate-800">Giao hàng</span>
                  </div>
                </div>
              )}
            </div>

            {/* Thông tin người nhận & Giao hàng */}
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-orange-600" />
                <span>Địa Chỉ Nhận Hàng</span>
              </h4>
              <div className="p-4 rounded-2xl bg-white border border-slate-200 space-y-1.5 text-xs">
                <div className="font-bold text-slate-900 text-sm">
                  {selectedOrderDetails.recipientName || 'Lê Đức Anh'} - {selectedOrderDetails.recipientPhone || '0988 123 456'}
                </div>
                <div className="text-slate-600 leading-relaxed">
                  {selectedOrderDetails.deliveryAddress}
                </div>
                {selectedOrderDetails.note && (
                  <div className="text-slate-400 italic pt-1">Ghi chú: "{selectedOrderDetails.note}"</div>
                )}
              </div>
            </div>

            {/* Danh sách Kiện hàng & Sản phẩm */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5 text-orange-600" />
                <span>Kiện Hàng & Sản Phẩm</span>
              </h4>

              {selectedOrderDetails.shopItems && selectedOrderDetails.shopItems.length > 0 ? (
                <div className="p-4 rounded-2xl border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-100">
                    <span className="font-bold text-slate-800">{selectedOrderDetails.shopName || 'TechWorld Official Store'}</span>
                    <span className="text-slate-500 font-mono text-[11px]">Vận đơn: {selectedOrderDetails.shopTrackingCode || 'Đang cập nhật'}</span>
                  </div>

                  <div className="divide-y divide-slate-100 space-y-2">
                    {selectedOrderDetails.shopItems.map((item) => (
                      <div key={item.id} className="pt-2 flex items-center gap-3">
                        <img
                          src={item.imageUrl}
                          alt=""
                          className="w-14 h-14 rounded-xl object-cover border border-slate-200 flex-shrink-0"
                        />
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-bold text-slate-900 line-clamp-1">{fixMojibakeText(item.productName)}</p>
                          <p className="text-[11px] text-slate-400">
                            Phân loại: {fixMojibakeText(item.variantName) || 'Tiêu chuẩn'} • Số lượng: x{item.quantity}
                          </p>
                        </div>
                        <div className="text-xs font-black text-slate-900 font-mono">
                          {formatVND(item.price * item.quantity)}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                (selectedOrderDetails.subOrders || []).map((sub, sIdx) => (
                  <div key={sub.id || sIdx} className="p-4 rounded-2xl border border-slate-200 space-y-3">
                    <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-100">
                      <span className="font-bold text-slate-800">{sub.shopName || 'TechWorld Official Store'}</span>
                      <span className="text-slate-500 font-mono text-[11px]">Vận đơn: {sub.trackingCode || 'Đang cập nhật'}</span>
                    </div>

                    <div className="divide-y divide-slate-100 space-y-2">
                      {(sub.items || []).map((item) => (
                        <div key={item.id} className="pt-2 flex items-center gap-3">
                          <img
                            src={item.imageUrl}
                            alt=""
                            className="w-14 h-14 rounded-xl object-cover border border-slate-200 flex-shrink-0"
                          />
                          <div className="flex-1 min-w-0">
                            <p className="text-xs font-bold text-slate-900 line-clamp-1">{fixMojibakeText(item.productName)}</p>
                            <p className="text-[11px] text-slate-400">
                              Phân loại: {fixMojibakeText(item.variantName) || 'Tiêu chuẩn'} • Số lượng: x{item.quantity}
                            </p>
                          </div>
                          <div className="text-xs font-black text-slate-900 font-mono">
                            {formatVND(item.price * item.quantity)}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Bảng Chi Tiết Tài Chính */}
            {(() => {
              const modalItems = (selectedOrderDetails.shopItems && selectedOrderDetails.shopItems.length > 0)
                ? selectedOrderDetails.shopItems
                : (selectedOrderDetails.subOrders || []).flatMap(s => s.items || []);

              const itemsSum = modalItems.reduce((acc, item) => acc + (Number(item.price || 0) * Number(item.quantity || 1)), 0);

              const subTotal = (selectedOrderDetails.isSplit && itemsSum > 0)
                ? itemsSum
                : (selectedOrderDetails.shopSubTotal || (itemsSum > 0 ? itemsSum : (selectedOrderDetails.subtotalAmount || selectedOrderDetails.totalAmount || 0)));

              const shippingFee = (selectedOrderDetails.isSplit && selectedOrderDetails.shopShippingFee != null)
                ? selectedOrderDetails.shopShippingFee
                : (selectedOrderDetails.shopShippingFee ?? selectedOrderDetails.shippingFee ?? 25000);

              const discount = selectedOrderDetails.shopDiscount || selectedOrderDetails.discountAmount || selectedOrderDetails.voucherDiscount || 0;
              const grandTotal = selectedOrderDetails.shopTotal || selectedOrderDetails.finalAmount || Math.max(0, subTotal + shippingFee - discount);

              return (
                <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
                  <div className="flex justify-between text-slate-600">
                    <span>Tiền hàng (Tạm tính):</span>
                    <span className="font-mono">{formatVND(subTotal)}</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Phí vận chuyển:</span>
                    <span className="font-mono">{formatVND(shippingFee)}</span>
                  </div>
                  {discount > 0 && (
                    <div className="flex justify-between text-emerald-600 font-semibold">
                      <span>Giảm giá Voucher:</span>
                      <span className="font-mono">-{formatVND(discount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-sm font-black text-slate-900 pt-2 border-t border-slate-200">
                    <span>Tổng cộng thanh toán:</span>
                    <span className="text-orange-600 font-mono text-base">{formatVND(grandTotal)}</span>
                  </div>
                </div>
              );
            })()}

            {/* Action buttons inside details modal */}
            <div className="flex items-center justify-end gap-3 pt-2">
              {canCancelOrder(selectedOrderDetails) && (
                <button
                  type="button"
                  onClick={() => {
                    setOrderToCancel(selectedOrderDetails);
                  }}
                  className="px-4 py-2.5 border border-rose-300 text-rose-600 hover:bg-rose-50 rounded-xl text-xs font-bold transition cursor-pointer"
                >
                  Hủy đơn hàng này
                </button>
              )}
              <button
                type="button"
                onClick={() => setSelectedOrderDetails(null)}
                className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition cursor-pointer"
              >
                Đóng chi tiết
              </button>
            </div>
          </div>
        )}
      </AnimatedPresence>

      {/* ========================================================================= */}
      {/* MODAL 2: XÁC NHẬN HỦY ĐƠN HÀNG (CANCEL ORDER MODAL) */}
      {/* ========================================================================= */}
      <AnimatedPresence show={Boolean(orderToCancel)} mode="modal" dataLenisPrevent={true} className="p-4">
        {orderToCancel && (
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-5">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="w-10 h-10 rounded-2xl bg-rose-100 flex items-center justify-center">
                <AlertCircle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-sm text-slate-900">Xác Nhận Hủy Đơn Hàng</h3>
                <p className="text-xs text-slate-500">Mã đơn: #{orderToCancel.orderCode || orderToCancel.id}</p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Bạn có chắc chắn muốn hủy đơn hàng này không? Sau khi hủy, đơn hàng sẽ được chuyển vào mục "Đã hủy" và không thể khôi phục.
            </p>

            <div className="space-y-2">
              <label htmlFor="order-cancel-reason" className="text-xs font-bold text-slate-700">Lý do hủy đơn hàng:</label>
              <select
                id="order-cancel-reason"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:border-orange-500 font-medium"
              >
                <option value="Đổi ý muốn mua sản phẩm khác">Đổi ý muốn mua sản phẩm khác</option>
                <option value="Muốn thay đổi địa chỉ nhận hàng">Muốn thay đổi địa chỉ nhận hàng</option>
                <option value="Muốn thay đổi phương thức thanh toán">Muốn thay đổi phương thức thanh toán</option>
                <option value="Tìm thấy giá rẻ hơn ở nơi khác">Tìm thấy giá rẻ hơn ở nơi khác</option>
                <option value="Thời gian giao hàng dự kiến quá lâu">Thời gian giao hàng dự kiến quá lâu</option>
                <option value="Lý do cá nhân khác">Lý do cá nhân khác</option>
              </select>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setOrderToCancel(null)}
                className="px-4 py-2.5 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl text-xs font-bold transition cursor-pointer"
              >
                Không hủy nữa
              </button>
              <button
                type="button"
                disabled={isCancelling}
                onClick={handleConfirmCancelOrder}
                className="px-5 py-2.5 bg-rose-600 hover:bg-rose-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition active:scale-95 shadow-md flex items-center gap-1.5 cursor-pointer"
              >
                {isCancelling ? 'Đang hủy...' : 'Xác nhận hủy đơn'}
              </button>
            </div>
          </div>
        )}
      </AnimatedPresence>

      {/* ========================================================================= */}
      {/* MODAL 3: THEO DÕI ĐƠN HÀNG & TIẾN TRÌNH SHIPPER */}
      {/* ========================================================================= */}
      <AnimatedPresence show={Boolean(trackingModalOpen && selectedShipment)} mode="modal" dataLenisPrevent={true} className="p-4">
        {selectedShipment && (
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
                  <Truck className="w-5 h-5 stroke-[2.5]" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-slate-900">Chi Tiết Vận Chuyển</h3>
                  <p className="text-[11px] font-mono font-bold text-orange-600">
                    {selectedShipment.trackingCode} • {selectedShipment.carrierName}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setTrackingModalOpen(false)}
                className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Shipper info card */}
            <div className="bg-gradient-to-r from-orange-50 to-amber-50 p-4 rounded-2xl border border-orange-200/80 space-y-2">
              <div className="flex items-center justify-between">
                <div className="font-bold text-xs text-slate-900 flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Bưu tá phụ trách: {selectedShipment.shipperName || 'Bưu tá GHN Express'}</span>
                </div>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white text-orange-700 shadow-xs">
                  {selectedShipment.shipperVehicle || 'Honda Wave'}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs text-slate-600">
                <div className="flex items-center gap-1">
                  <Phone className="w-3.5 h-3.5 text-orange-600" />
                  <span>{selectedShipment.shipperPhone || '0988 123 456'}</span>
                </div>
                <span className="text-[11px] font-bold text-emerald-600">Đang hoạt động tuyến giao</span>
              </div>
            </div>

            {/* Timeline */}
            <div className="space-y-3 pt-2">
              <h4 className="text-xs font-bold text-slate-900">Hành Trình Bưu Gửi</h4>
              <div className="space-y-4 border-l-2 border-orange-200 ml-3 pl-4">
                {(selectedShipment.timeline || []).map((step, idx) => (
                  <div key={idx} className="relative">
                    <span
                      className={`absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white ${
                        step.current
                          ? 'bg-orange-600 ring-4 ring-orange-100'
                          : step.completed
                          ? 'bg-emerald-500'
                          : 'bg-slate-300'
                      }`}
                    />
                    <div className="text-xs font-bold text-slate-900">{step.title}</div>
                    <div className="text-[11px] text-slate-500">{step.description}</div>
                    <div className="text-[10px] text-slate-400 font-mono mt-0.5">{step.time}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Bottom Actions */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-100">
              <button
                type="button"
                disabled={isSimulating}
                onClick={handleSimulateNextStep}
                className="px-3.5 py-2 bg-orange-50 hover:bg-orange-100 text-orange-700 rounded-xl text-xs font-bold transition flex items-center gap-1 cursor-pointer"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isSimulating ? 'animate-spin' : ''}`} />
                <span>Mô phỏng bước bưu tá</span>
              </button>

              <button
                type="button"
                onClick={() => setTrackingModalOpen(false)}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </AnimatedPresence>

      {/* ========================================================================= */}
      {/* MODAL 4: ĐÁNH GIÁ SẢN PHẨM (REVIEW MODAL) */}
      {/* ========================================================================= */}
      <AnimatedPresence show={reviewModalOpen} mode="modal" dataLenisPrevent={true} className="p-4">
        <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="font-bold text-sm text-slate-900">Đánh Giá Sản Phẩm</h3>
            <button onClick={() => setReviewModalOpen(false)} className="text-slate-400 hover:text-slate-600">✕</button>
          </div>

          {reviewTargetItem && (
            <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-2xl">
              <img src={reviewTargetItem.imageUrl} alt="" className="w-12 h-12 rounded-xl object-cover" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-bold text-slate-900 truncate">{reviewTargetItem.productName}</p>
                <p className="text-[11px] text-slate-400">{reviewTargetItem.variantName || 'Bản Tiêu Chuẩn'}</p>
              </div>
            </div>
          )}

          <div className="space-y-2 text-center">
            <div className="text-xs font-bold text-slate-700">Chất lượng sản phẩm:</div>
            <div className="flex justify-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setReviewRating(star)}
                  className="p-1 text-2xl transition hover:scale-110"
                >
                  <Star className={`w-6 h-6 ${star <= reviewRating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`} />
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-1">
            <label htmlFor="order-review-comment" className="text-xs font-bold text-slate-700">Cảm nhận chi tiết:</label>
            <textarea
              id="order-review-comment"
              rows={3}
              value={reviewComment}
              onChange={(e) => setReviewComment(e.target.value)}
              placeholder="Chia sẻ trải nghiệm sử dụng, thời gian giao hàng và chất lượng đóng gói..."
              className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:border-orange-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              onClick={() => setReviewModalOpen(false)}
              className="px-4 py-2 border border-slate-200 rounded-xl text-xs font-bold text-slate-600"
            >
              Hủy
            </button>
            <button
              onClick={handleSubmitReview}
              className="px-5 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-bold shadow-md"
            >
              Gửi Đánh Giá
            </button>
          </div>
        </div>
      </AnimatedPresence>

      {/* ========================================================================= */}
      {/* MODAL 5: TRẢ HÀNG & HOÀN TIỀN (REFUND MODAL) */}
      {/* ========================================================================= */}
      <AnimatedPresence show={refundModalOpen} mode="modal" dataLenisPrevent={true} className="p-4">
        <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="font-bold text-sm text-slate-900">Yêu Cầu Trả Hàng / Hoàn Tiền</h3>
            <button onClick={() => setRefundModalOpen(false)} className="text-slate-400 hover:text-slate-600">✕</button>
          </div>

          <div className="space-y-2">
            <label htmlFor="order-refund-reason" className="text-xs font-bold text-slate-700">Lý do yêu cầu hoàn tiền:</label>
            <select
              id="order-refund-reason"
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs"
            >
              <option value="Sản phẩm nhận được bị lỗi kỹ thuật / không đúng mô tả">
                Sản phẩm nhận được bị lỗi kỹ thuật / không đúng mô tả
              </option>
              <option value="Hàng bị móp méo, vỡ hỏng trong quá trình vận chuyển">
                Hàng bị móp méo, vỡ hỏng trong quá trình vận chuyển
              </option>
              <option value="Nhận thiếu hàng / phụ kiện so với đơn">
                Nhận thiếu hàng / phụ kiện so với đơn
              </option>
              <option value="Chưa nhận được kiện hàng dù bưu tá báo giao thành công">
                Chưa nhận được kiện hàng dù bưu tá báo giao thành công
              </option>
            </select>
          </div>

          <div className="space-y-1">
            <label htmlFor="order-refund-proof-url" className="text-xs font-bold text-slate-700">Link ảnh / video mở hộp chứng minh:</label>
            <input
              id="order-refund-proof-url"
              type="text"
              value={refundProofUrl}
              onChange={(e) => setRefundProofUrl(e.target.value)}
              placeholder="https://images.unsplash.com/..."
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              onClick={() => setRefundModalOpen(false)}
              className="px-4 py-2 border border-slate-200 rounded-xl text-xs font-bold text-slate-600"
            >
              Hủy
            </button>
            <button
              onClick={handleSubmitRefund}
              className="px-5 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold shadow-md"
            >
              Gửi Khiếu Nại
            </button>
          </div>
        </div>
      </AnimatedPresence>
    </div>
  );
}
