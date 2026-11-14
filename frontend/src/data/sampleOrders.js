/**
 * sampleOrders.js
 * Quản lý dữ liệu mẫu, kho lưu trữ orders & shipments cho tính năng:
 * - Lịch sử mua hàng & Theo dõi tiến trình đơn hàng (Buyer Order History & Tracking)
 * - Cổng thông tin Quản lý vận chuyển cho Shipper (Shipper Logistics Portal)
 */

const ORDERS_STORAGE_KEY = 'quickshop_mock_orders';
const SHIPMENTS_STORAGE_KEY = 'quickshop_mock_shipments';

export const initialShipments = [
  {
    id: 101,
    subOrderId: 201,
    orderId: 'MO-20261113-001',
    trackingCode: 'GHN-VNM-892134',
    carrierName: 'Giao Hàng Nhanh (GHN)',
    shippingFee: 25000,
    shippingStatus: 'DELIVERING', // READY_TO_PICK | PICKED | DELIVERING | DELIVERED | DELIVERY_FAILED
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    fromAddress: 'Kho TechWorld, Tầng 3 Tòa Dolphin Plaza, 28 Trần Bình, Nam Từ Liêm, Hà Nội',
    toAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    weightGrams: 850,
    codAmount: 0, // Đã thanh toán trước qua MB Bank
    paymentMethod: 'MB_BANK_QR',
    paymentStatus: 'PAID',
    shipperName: 'Trần Văn Thành',
    shipperPhone: '0978 456 789',
    shipperVehicle: '29-B1 688.92 (Honda Wave Alpha)',
    estimatedDeliveryDate: '2026-11-14T17:30:00',
    createdAt: '2026-11-13T09:15:00',
    timeline: [
      {
        step: 1,
        title: 'Đơn hàng đã được đặt thành công',
        description: 'Người mua đã thanh toán trực tuyến qua MB Bank QR',
        location: 'Hệ thống QuickShop',
        time: '13/11/2026 09:15',
        completed: true,
      },
      {
        step: 2,
        title: 'Người bán đã chuẩn bị xong kiện hàng',
        description: 'Đã đóng gói và in vận đơn GHN-VNM-892134',
        location: 'Kho TechWorld Official Store - Hà Nội',
        time: '13/11/2026 11:30',
        completed: true,
      },
      {
        step: 3,
        title: 'Shipper GHN đã lấy hàng thành công',
        description: 'Bưu tá Trần Văn Thành đã tiếp nhận bưu gửi tại bưu cục Nam Từ Liêm',
        location: 'Bưu cục GHN Cầu Giấy - Hà Nội',
        time: '13/11/2026 14:00',
        completed: true,
      },
      {
        step: 4,
        title: 'Đang vận chuyển giao đến bạn',
        description: 'Bưu tá đang trên đường giao kiện hàng tới địa chỉ người nhận',
        location: 'Tuyến đường Cầu Giấy - Quan Hoa',
        time: '14/11/2026 08:30',
        completed: true,
        current: true,
      },
      {
        step: 5,
        title: 'Giao hàng thành công',
        description: 'Người nhận kiểm tra hàng và ký nhận hoàn tất',
        location: 'Địa chỉ người nhận',
        time: 'Dự kiến: 14/11/2026 17:30',
        completed: false,
      },
    ],
  },
  {
    id: 102,
    subOrderId: 202,
    orderId: 'MO-20261112-004',
    trackingCode: 'GHTK-VNM-481923',
    carrierName: 'Giao Hàng Tiết Kiệm (GHTK)',
    shippingFee: 22000,
    shippingStatus: 'DELIVERED',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    fromAddress: 'Kho Thời Trang Coolmate Official, 163 Thái Hà, Đống Đa, Hà Nội',
    toAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    weightGrams: 420,
    codAmount: 538000,
    paymentMethod: 'COD',
    paymentStatus: 'PAID',
    shipperName: 'Lê Quốc Bảo',
    shipperPhone: '0912 345 678',
    shipperVehicle: '29-C1 543.21 (Yamaha Sirius)',
    estimatedDeliveryDate: '2026-11-13T10:00:00',
    createdAt: '2026-11-12T08:00:00',
    timeline: [
      {
        step: 1,
        title: 'Đơn hàng đã đặt',
        description: 'Phương thức thanh toán tiền mặt khi nhận hàng (COD)',
        location: 'Hệ thống QuickShop',
        time: '12/11/2026 08:00',
        completed: true,
      },
      {
        step: 2,
        title: 'Kho shop xuất kho',
        description: 'Đã bàn giao cho đối tác vận chuyển GHTK',
        location: 'Kho Coolmate Đống Đa',
        time: '12/11/2026 10:20',
        completed: true,
      },
      {
        step: 3,
        title: 'Đã nhập kho trung chuyển',
        description: 'Bưu kiện đã đến kho phân loại Cầu Giấy SOC',
        location: 'Kho phân loại Hà Nội SOC',
        time: '12/11/2026 18:45',
        completed: true,
      },
      {
        step: 4,
        title: 'Bưu tá xuất kho đi giao',
        description: 'Shipper Lê Quốc Bảo đang giao hàng',
        location: 'Cầu Giấy, Hà Nội',
        time: '13/11/2026 08:15',
        completed: true,
      },
      {
        step: 5,
        title: 'Giao hàng thành công',
        description: 'Khách hàng đã nhận kiện hàng và thanh toán đủ số tiền COD 538.000₫',
        location: 'Số 18, Ngõ 45 Cầu Giấy',
        time: '13/11/2026 09:40',
        completed: true,
        current: true,
      },
    ],
  },
  {
    id: 103,
    subOrderId: 203,
    orderId: 'MO-20261114-009',
    trackingCode: 'GHN-VNM-739102',
    carrierName: 'Giao Hàng Nhanh (GHN)',
    shippingFee: 30000,
    shippingStatus: 'READY_TO_PICK',
    recipientName: 'Nguyễn Văn Mua Hàng',
    recipientPhone: '0905 112 233',
    fromAddress: 'Kho Gia Dụng Sunhouse, KCN Ngọc Hồi, Thanh Trì, Hà Nội',
    toAddress: 'Chung cư Skylight, 125D Minh Khai, Hai Bà Trưng, Hà Nội',
    weightGrams: 3200,
    codAmount: 1450000,
    paymentMethod: 'COD',
    paymentStatus: 'PENDING',
    shipperName: 'Phạm Hồng Quân',
    shipperPhone: '0936 789 012',
    shipperVehicle: '30-F2 789.10 (Xe tải nhẹ 500kg)',
    estimatedDeliveryDate: '2026-11-15T16:00:00',
    createdAt: '2026-11-14T07:30:00',
    timeline: [
      {
        step: 1,
        title: 'Đơn hàng mới tạo',
        description: 'Người bán đang chuẩn bị đóng gói hàng',
        location: 'Kho Gia Dụng Thông Minh',
        time: '14/11/2026 07:30',
        completed: true,
        current: true,
      },
      {
        step: 2,
        title: 'Chờ shipper lấy hàng',
        description: 'Đã phát lệnh điều phối tài xế bưu tá đến kho',
        location: 'Bưu cục GHN Thanh Trì',
        time: 'Dự kiến: 14/11/2026 14:00',
        completed: false,
      },
    ],
  },
  {
    id: 104,
    subOrderId: 204,
    orderId: 'MO-20261114-012',
    trackingCode: 'GHTK-VNM-662910',
    carrierName: 'Giao Hàng Tiết Kiệm (GHTK)',
    shippingFee: 22000,
    shippingStatus: 'PICKED',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    fromAddress: 'Nhà sách Nhã Nam, 59 Đỗ Quang, Trung Hòa, Cầu Giấy, Hà Nội',
    toAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    weightGrams: 900,
    codAmount: 0,
    paymentMethod: 'VNPAY',
    paymentStatus: 'PAID',
    shipperName: 'Vũ Minh Tuấn',
    shipperPhone: '0983 999 111',
    shipperVehicle: '29-V1 123.45',
    estimatedDeliveryDate: '2026-11-15T11:00:00',
    createdAt: '2026-11-14T08:10:00',
    timeline: [
      {
        step: 1,
        title: 'Đã đặt hàng & Thanh toán VNPAY',
        description: 'Giao dịch VNPAY mã GD #98231 hoàn tất',
        location: 'Cổng VNPAY',
        time: '14/11/2026 08:10',
        completed: true,
      },
      {
        step: 2,
        title: 'Đã lấy kiện hàng',
        description: 'Shipper đã nhận kiện hàng từ cửa hàng sách Nhã Nam',
        location: '59 Đỗ Quang, Cầu Giấy',
        time: '14/11/2026 10:15',
        completed: true,
        current: true,
      },
      {
        step: 3,
        title: 'Trung chuyển bưu cục',
        description: 'Đang chuyển đến trạm trung chuyển gần bạn',
        location: 'Trạm Cầu Giấy',
        time: 'Dự kiến: 14/11/2026 15:30',
        completed: false,
      },
    ],
  },
  {
    id: 105,
    subOrderId: 205,
    orderId: 'MO-20261111-002',
    trackingCode: 'QEX-VNM-331829',
    carrierName: 'QuickExpress Hỏa Tốc',
    shippingFee: 45000,
    shippingStatus: 'DELIVERY_FAILED',
    recipientName: 'Nguyễn Thu Trang',
    recipientPhone: '0915 999 888',
    fromAddress: 'Apple Flagship Store, Times City, Minh Khai, Hai Bà Trưng, Hà Nội',
    toAddress: 'Số 99 Nguyễn Trãi, Thanh Xuân, Hà Nội',
    weightGrams: 550,
    codAmount: 12900000,
    paymentMethod: 'COD',
    paymentStatus: 'PENDING',
    shipperName: 'Đỗ Mạnh Hùng',
    shipperPhone: '0904 555 666',
    shipperVehicle: '29-G1 456.78',
    estimatedDeliveryDate: '2026-11-13T17:00:00',
    createdAt: '2026-11-11T14:00:00',
    failureReason: 'Khách hàng bận họp, hẹn giao lại vào ca sáng ngày mai',
    timeline: [
      {
        step: 1,
        title: 'Tạo đơn hỏa tốc',
        description: 'Giao hàng 2h QuickExpress',
        location: 'Times City',
        time: '11/11/2026 14:00',
        completed: true,
      },
      {
        step: 2,
        title: 'Đang đi giao',
        description: 'Tài xế đã đến gần khu vực Nguyễn Trãi',
        location: 'Thanh Xuân',
        time: '11/11/2026 15:20',
        completed: true,
      },
      {
        step: 3,
        title: 'Giao hàng chưa thành công (Lần 1)',
        description: 'Lý do: Khách hàng bận họp, hẹn giao lại vào ca sáng ngày mai',
        location: 'Số 99 Nguyễn Trãi',
        time: '11/11/2026 16:15',
        completed: true,
        current: true,
      },
    ],
  },
];

export const initialOrders = [
  {
    id: 1,
    orderCode: 'MO-20261113-001',
    userId: 2,
    createdAt: '2026-11-13T09:15:00',
    status: 'DELIVERING', // PENDING, PAID, PROCESSING, DELIVERING, COMPLETED, CANCELLED
    totalAmount: 34990000,
    discountAmount: 100000,
    shippingFee: 25000,
    finalAmount: 34915000,
    paymentMethod: 'MB_BANK_QR',
    paymentStatus: 'PAID',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    deliveryAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    note: 'Giao giờ hành chính, gọi trước 15 phút khi đến.',
    subOrders: [
      {
        id: 201,
        masterOrderId: 1,
        shopId: 1,
        shopName: 'TechWorld Official Store',
        shopAvatar: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
        status: 'DELIVERING',
        subTotal: 34990000,
        shippingFee: 25000,
        trackingCode: 'GHN-VNM-892134',
        carrierName: 'Giao Hàng Nhanh (GHN)',
        items: [
          {
            id: 1001,
            productId: 1,
            variantId: 12,
            sku: 'QP-PRO-BLACK-512',
            productName: 'Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế',
            variantName: 'Đen Không Gian - 512GB',
            color: 'Đen Không Gian',
            size: '512GB',
            price: 34990000,
            quantity: 1,
            imageUrl: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=300&auto=format&fit=crop&q=80',
          },
        ],
      },
    ],
  },
  {
    id: 2,
    orderCode: 'MO-20261112-004',
    userId: 2,
    createdAt: '2026-11-12T08:00:00',
    status: 'COMPLETED',
    totalAmount: 516000,
    discountAmount: 0,
    shippingFee: 22000,
    finalAmount: 538000,
    paymentMethod: 'COD',
    paymentStatus: 'PAID',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    deliveryAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    note: 'Kiểm tra hàng trước khi nhận',
    subOrders: [
      {
        id: 202,
        masterOrderId: 2,
        shopId: 2,
        shopName: 'Thời Trang Coolmate Official',
        shopAvatar: 'https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=100&auto=format&fit=crop&q=80',
        status: 'COMPLETED',
        subTotal: 516000,
        shippingFee: 22000,
        trackingCode: 'GHTK-VNM-481923',
        carrierName: 'Giao Hàng Tiết Kiệm (GHTK)',
        items: [
          {
            id: 1002,
            productId: 4,
            variantId: 41,
            sku: 'POLO-CAFE-BLACK-L',
            productName: 'Áo Polo Nam Sợi Cafe Khử Mùi Thoáng Khí Tự Nhiên Coolmate',
            variantName: 'Đen Huyền Bí - L',
            color: 'Đen Huyền Bí',
            size: 'L',
            price: 258000,
            quantity: 2,
            imageUrl: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=300&auto=format&fit=crop&q=80',
          },
        ],
      },
    ],
  },
  {
    id: 3,
    orderCode: 'MO-20261114-012',
    userId: 2,
    createdAt: '2026-11-14T08:10:00',
    status: 'PROCESSING',
    totalAmount: 495000,
    discountAmount: 50000,
    shippingFee: 22000,
    finalAmount: 467000,
    paymentMethod: 'VNPAY',
    paymentStatus: 'PAID',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    deliveryAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    note: 'Đóng gói cẩn thận giúp mình bìa sách không bị quăn mép',
    subOrders: [
      {
        id: 204,
        masterOrderId: 3,
        shopId: 4,
        shopName: 'Nhà Sách Nhã Nam Official',
        shopAvatar: 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=100&auto=format&fit=crop&q=80',
        status: 'PICKED',
        subTotal: 495000,
        shippingFee: 22000,
        trackingCode: 'GHTK-VNM-662910',
        carrierName: 'Giao Hàng Tiết Kiệm (GHTK)',
        items: [
          {
            id: 1003,
            productId: 10,
            variantId: 101,
            sku: 'BOOK-ATOMIC-HABITS',
            productName: 'Sách Atomic Habits - Thay Đổi Tí Hon Hiệu Quả Bất Ngờ (Bìa Cứng)',
            variantName: 'Bản Bìa Cứng - Giới Hạn',
            color: 'Bìa Cứng',
            size: 'Tiêu chuẩn',
            price: 245000,
            quantity: 1,
            imageUrl: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&auto=format&fit=crop&q=80',
          },
          {
            id: 1004,
            productId: 11,
            variantId: 111,
            sku: 'BOOK-PSYCH-MONEY',
            productName: 'Sách Tâm Lý Học Về Tiền - The Psychology of Money',
            variantName: 'Tái bản mới nhất',
            color: 'Tiêu chuẩn',
            size: 'Tiêu chuẩn',
            price: 250000,
            quantity: 1,
            imageUrl: 'https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=300&auto=format&fit=crop&q=80',
          },
        ],
      },
    ],
  },
  {
    id: 4,
    orderCode: 'MO-20261114-025',
    userId: 2,
    createdAt: '2026-11-14T11:45:00',
    status: 'PENDING',
    totalAmount: 1890000,
    discountAmount: 50000,
    shippingFee: 35000,
    finalAmount: 1875000,
    paymentMethod: 'COD',
    paymentStatus: 'PENDING',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    deliveryAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    note: 'Chờ shop gọi xác nhận đơn trước khi đóng hàng',
    subOrders: [
      {
        id: 206,
        masterOrderId: 4,
        shopId: 3,
        shopName: 'Gia Dụng Sunhouse Mall',
        shopAvatar: 'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=100&auto=format&fit=crop&q=80',
        status: 'PENDING',
        subTotal: 1890000,
        shippingFee: 35000,
        trackingCode: 'GHN-VNM-PENDING1',
        carrierName: 'Giao Hàng Nhanh (GHN)',
        items: [
          {
            id: 1005,
            productId: 7,
            variantId: 71,
            sku: 'AIRFRYER-9L-TOUCH',
            productName: 'Nồi Chiên Không Dầu Điện Tử Dung Tích Lớn 9.2L Cảm Ứng Đa Năng',
            variantName: 'Mặt Kính Cảm Ứng Đen - 9.2L',
            color: 'Đen Titan',
            size: '9.2L',
            price: 1890000,
            quantity: 1,
            imageUrl: 'https://images.unsplash.com/photo-1585338107529-13afc5f02586?w=300&auto=format&fit=crop&q=80',
          },
        ],
      },
    ],
  },
  {
    id: 5,
    orderCode: 'MO-20261108-011',
    userId: 2,
    createdAt: '2026-11-08T15:20:00',
    status: 'CANCELLED',
    cancelReason: 'Đổi ý muốn chọn màu khác của sản phẩm',
    totalAmount: 4990000,
    discountAmount: 100000,
    shippingFee: 0,
    finalAmount: 4890000,
    paymentMethod: 'COD',
    paymentStatus: 'CANCELLED',
    recipientName: 'Lê Đức Anh',
    recipientPhone: '0988 123 456',
    deliveryAddress: 'Số 18, Ngõ 45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội',
    note: '',
    subOrders: [
      {
        id: 207,
        masterOrderId: 5,
        shopId: 1,
        shopName: 'TechWorld Official Store',
        shopAvatar: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
        status: 'CANCELLED',
        subTotal: 4990000,
        shippingFee: 0,
        trackingCode: 'CANCELLED-001',
        carrierName: 'Giao Hàng Nhanh (GHN)',
        items: [
          {
            id: 1006,
            productId: 3,
            variantId: 31,
            sku: 'QPODS-PRO-WHITE',
            productName: 'Tai nghe Bluetooth True Wireless QuickPods Pro 2 Chống Ồn ANC',
            variantName: 'Trắng Tinh Khôi - Tiêu chuẩn',
            color: 'Trắng Tinh Khôi',
            size: 'Tiêu chuẩn',
            price: 4990000,
            quantity: 1,
            imageUrl: 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=300&auto=format&fit=crop&q=80',
          },
        ],
      },
    ],
  },
];

// Helper đọc và ghi dữ liệu mock từ localStorage
export const getStoredOrders = () => {
  if (typeof localStorage === 'undefined') return initialOrders;
  try {
    const raw = localStorage.getItem(ORDERS_STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(ORDERS_STORAGE_KEY, JSON.stringify(initialOrders));
      return initialOrders;
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : initialOrders;
  } catch {
    return initialOrders;
  }
};

export const saveStoredOrders = (orders) => {
  if (typeof localStorage === 'undefined') return;
  try {
    localStorage.setItem(ORDERS_STORAGE_KEY, JSON.stringify(orders));
  } catch (err) {
    console.warn('Lỗi khi lưu mock orders vào localStorage:', err);
  }
};

export const getStoredShipments = () => {
  if (typeof localStorage === 'undefined') return initialShipments;
  try {
    const raw = localStorage.getItem(SHIPMENTS_STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(SHIPMENTS_STORAGE_KEY, JSON.stringify(initialShipments));
      return initialShipments;
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : initialShipments;
  } catch {
    return initialShipments;
  }
};

export const saveStoredShipments = (shipments) => {
  if (typeof localStorage === 'undefined') return;
  try {
    localStorage.setItem(SHIPMENTS_STORAGE_KEY, JSON.stringify(shipments));
  } catch (err) {
    console.warn('Lỗi khi lưu mock shipments vào localStorage:', err);
  }
};

// Tra cứu danh sách đơn hàng theo userId
export const getOrdersByUserId = (userId) => {
  const all = getStoredOrders();
  if (!userId) return all;
  // Hỗ trợ hiển thị các đơn mẫu cho user demo hoặc chính user đang login
  return all.filter((o) => String(o.userId) === String(userId) || o.userId === 2 || o.userId === 1001);
};

// Hủy đơn hàng
export const cancelOrderById = (orderId, reason = 'Khách hàng yêu cầu hủy đơn') => {
  const orders = getStoredOrders();
  const updated = orders.map((o) => {
    if (String(o.id) === String(orderId) || String(o.orderCode) === String(orderId)) {
      return {
        ...o,
        status: 'CANCELLED',
        cancelReason: reason,
        paymentStatus: o.paymentStatus === 'PAID' ? 'REFUNDED' : 'CANCELLED',
        subOrders: (o.subOrders || []).map((sub) => ({ ...sub, status: 'CANCELLED' })),
      };
    }
    return o;
  });
  saveStoredOrders(updated);
  return updated;
};

// Cập nhật trạng thái vận đơn (dành cho Shipper Portal)
export const updateShipmentStatusInStore = (shipmentId, newStatus, reason = '') => {
  const shipments = getStoredShipments();
  const updated = shipments.map((s) => {
    if (String(s.id) === String(shipmentId) || s.trackingCode === shipmentId) {
      const nowStr = new Date().toLocaleString('vi-VN');
      const newTimeline = [...(s.timeline || [])];
      
      let stepTitle = 'Cập nhật trạng thái';
      let stepDesc = reason || `Trạng thái vận chuyển: ${newStatus}`;
      if (newStatus === 'PICKED') {
        stepTitle = 'Shipper đã lấy hàng';
        stepDesc = 'Bưu kiện đã được bàn giao từ người bán cho bưu tá';
      } else if (newStatus === 'DELIVERING') {
        stepTitle = 'Đang giao hàng';
        stepDesc = 'Bưu tá đang di chuyển tới địa chỉ nhận hàng';
      } else if (newStatus === 'DELIVERED') {
        stepTitle = 'Giao hàng thành công';
        stepDesc = `Người nhận đã kiểm tra và nhận hàng hoàn tất. Đã thu COD: ${(s.codAmount || 0).toLocaleString()}₫`;
      } else if (newStatus === 'DELIVERY_FAILED') {
        stepTitle = 'Giao hàng chưa thành công';
        stepDesc = reason || 'Không liên lạc được người nhận, hẹn lại ca sau';
      }

      newTimeline.push({
        step: newTimeline.length + 1,
        title: stepTitle,
        description: stepDesc,
        location: s.toAddress ? s.toAddress.split(',')[1] || 'Tuyến giao hàng' : 'Tuyến giao',
        time: nowStr,
        completed: true,
        current: true,
      });

      return {
        ...s,
        shippingStatus: newStatus,
        failureReason: newStatus === 'DELIVERY_FAILED' ? (reason || 'Hẹn giao lại') : null,
        timeline: newTimeline,
      };
    }
    return s;
  });
  saveStoredShipments(updated);

  // Đồng bộ sang orders tương ứng
  const orders = getStoredOrders();
  const targetShipment = updated.find((s) => String(s.id) === String(shipmentId) || s.trackingCode === shipmentId);
  if (targetShipment && targetShipment.orderId) {
    const nextOrders = orders.map((o) => {
      if (o.orderCode === targetShipment.orderId || String(o.id) === String(targetShipment.orderId)) {
        let orderStatus = o.status;
        if (newStatus === 'DELIVERED') orderStatus = 'COMPLETED';
        else if (newStatus === 'DELIVERING') orderStatus = 'DELIVERING';
        else if (newStatus === 'PICKED') orderStatus = 'PROCESSING';
        return {
          ...o,
          status: orderStatus,
          subOrders: (o.subOrders || []).map((sub) =>
            sub.id === targetShipment.subOrderId ? { ...sub, status: orderStatus } : sub
          ),
        };
      }
      return o;
    });
    saveStoredOrders(nextOrders);
  }

  return updated;
};

// Mô phỏng bước tiếp theo của vận đơn
export const simulateNextCarrierStepInStore = (trackingCode) => {
  const shipments = getStoredShipments();
  const target = shipments.find((s) => s.trackingCode === trackingCode);
  if (!target) return null;

  let nextStatus = 'DELIVERING';
  if (target.shippingStatus === 'READY_TO_PICK') nextStatus = 'PICKED';
  else if (target.shippingStatus === 'PICKED') nextStatus = 'DELIVERING';
  else if (target.shippingStatus === 'DELIVERING') nextStatus = 'DELIVERED';
  else if (target.shippingStatus === 'DELIVERY_FAILED') nextStatus = 'DELIVERING';

  updateShipmentStatusInStore(target.id, nextStatus);
  return nextStatus;
};

// Tạo đơn hàng mới sau khi Checkout thành công
export const recordNewOrder = (orderData) => {
  const currentOrders = getStoredOrders();
  const currentShipments = getStoredShipments();

  const newId = Date.now();
  const newOrderCode = `MO-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.floor(100 + Math.random() * 900)}`;
  const trackingCode = `GHN-VNM-${Math.floor(100000 + Math.random() * 900000)}`;

  const subOrders = (orderData.shopGroups || []).map((sg, idx) => ({
    id: newId + idx + 1,
    masterOrderId: newId,
    shopId: sg.shopId || 1,
    shopName: sg.shopName || 'QuickShop Partner',
    shopAvatar: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
    status: orderData.paymentStatus === 'PAID' ? 'PROCESSING' : 'PENDING',
    subTotal: sg.items.reduce((s, it) => s + it.price * it.quantity, 0),
    shippingFee: 25000,
    trackingCode,
    carrierName: 'Giao Hàng Nhanh (GHN)',
    items: sg.items.map((it, itemIdx) => ({
      id: newId + 100 + itemIdx,
      productId: it.productId,
      variantId: it.variantId,
      sku: it.sku || `SKU-${it.variantId}`,
      productName: it.productName,
      variantName: it.variantName || 'Mặc định',
      color: it.color || '',
      size: it.size || '',
      price: it.price,
      quantity: it.quantity,
      imageUrl: it.mainImage || '',
    })),
  }));

  const newMasterOrder = {
    id: newId,
    orderCode: newOrderCode,
    userId: orderData.userId || 2,
    createdAt: new Date().toISOString(),
    status: orderData.paymentStatus === 'PAID' ? 'PROCESSING' : 'PENDING',
    totalAmount: orderData.subTotal || 0,
    discountAmount: orderData.totalDiscount || 0,
    shippingFee: orderData.actualShippingFee || 25000,
    finalAmount: orderData.finalTotal || 0,
    paymentMethod: orderData.paymentMethod || 'COD',
    paymentStatus: orderData.paymentStatus || 'PENDING',
    recipientName: orderData.shippingInfo?.fullName || 'Khách Hàng',
    recipientPhone: orderData.shippingInfo?.phone || '0988 123 456',
    deliveryAddress: `${orderData.shippingInfo?.address || ''}, ${orderData.shippingInfo?.ward || ''}, ${orderData.shippingInfo?.district || ''}, ${orderData.shippingInfo?.city || ''}`,
    note: orderData.shippingInfo?.note || '',
    subOrders,
  };

  const newShipment = {
    id: newId + 500,
    subOrderId: newId + 1,
    orderId: newOrderCode,
    trackingCode,
    carrierName: 'Giao Hàng Nhanh (GHN)',
    shippingFee: orderData.actualShippingFee || 25000,
    shippingStatus: 'READY_TO_PICK',
    recipientName: newMasterOrder.recipientName,
    recipientPhone: newMasterOrder.recipientPhone,
    fromAddress: 'Kho Trung Tâm QuickShop Mall, KCN Tân Bình, TP. Hồ Chí Minh',
    toAddress: newMasterOrder.deliveryAddress,
    weightGrams: 750,
    codAmount: orderData.paymentStatus === 'PAID' ? 0 : (orderData.finalTotal || 0),
    paymentMethod: orderData.paymentMethod,
    paymentStatus: orderData.paymentStatus,
    shipperName: 'Phạm Đức Cường',
    shipperPhone: '0982 777 888',
    shipperVehicle: '29-H1 888.66',
    estimatedDeliveryDate: new Date(Date.now() + 2 * 24 * 3600 * 1000).toISOString(),
    createdAt: new Date().toISOString(),
    timeline: [
      {
        step: 1,
        title: 'Đơn hàng mới tạo thành công',
        description: `Phương thức thanh toán: ${orderData.paymentMethod} (${orderData.paymentStatus})`,
        location: 'Hệ thống QuickShop Mall',
        time: new Date().toLocaleString('vi-VN'),
        completed: true,
        current: true,
      },
      {
        step: 2,
        title: 'Người bán chuẩn bị đóng gói kiện hàng',
        description: 'Vận đơn GHN tự động cấp mã thành công',
        location: 'Kho Tổng QuickShop',
        time: 'Dự kiến: Hôm nay',
        completed: false,
      },
    ],
  };

  const updatedOrders = [newMasterOrder, ...currentOrders];
  const updatedShipments = [newShipment, ...currentShipments];
  saveStoredOrders(updatedOrders);
  saveStoredShipments(updatedShipments);

  return { order: newMasterOrder, shipment: newShipment };
};
