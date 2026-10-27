import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Load test data
const testData = JSON.parse(open('../data/test-data.json'));

const BASE_URL = __ENV.BASE_URL || testData.baseUrl || 'http://localhost:8083';

// BẢO VỆ PRODUCTION (Yêu cầu số 9)
if ((BASE_URL.includes('quickshop.com') || BASE_URL.includes('api.quickshop.vn')) && __ENV.ALLOW_PROD_TEST !== 'true') {
  throw new Error('[CRITICAL] Không được phép chạy kiểm thử tải trên production!');
}

export const options = {
  vus: 5,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.05'], // Tỷ lệ lỗi dưới 5%
    http_req_duration: ['p(95)<500'], // 95% request phản hồi dưới 500ms
  },
};

const smokeSuccessRate = new Rate('smoke_success_rate');

export default function () {
  const user = testData.users[Math.floor(Math.random() * testData.users.length)];
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': String(user.id),
    'X-User-Role': user.role,
  };

  // 1. Kiểm tra API Đọc: Lấy thông tin giỏ hàng
  const cartRes = http.get(`${BASE_URL}/api/cart`, { headers });
  const cartOk = check(cartRes, {
    'GET /api/cart status is 200': (r) => r.status === 200,
  });
  smokeSuccessRate.add(cartOk);

  // 2. Kiểm tra API Đọc: Lấy voucher toàn sàn
  const voucherRes = http.get(`${BASE_URL}/api/vouchers/QUICKSHOP10`, { headers });
  const voucherOk = check(voucherRes, {
    'GET /api/vouchers/{code} status is 200': (r) => r.status === 200,
  });
  smokeSuccessRate.add(voucherOk);

  // 3. Kiểm tra API Ghi: Thêm sản phẩm vào giỏ
  const variant = testData.variants[Math.floor(Math.random() * testData.variants.length)];
  const addPayload = JSON.stringify({
    variantId: variant.variantId,
    quantity: 1,
  });
  const addRes = http.post(`${BASE_URL}/api/cart/items`, addPayload, { headers });
  const addOk = check(addRes, {
    'POST /api/cart/items status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });
  smokeSuccessRate.add(addOk);

  // 4. Kiểm tra API Ghi: Checkout đơn hàng
  const checkoutKey = `smoke-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  const checkoutHeaders = Object.assign({}, headers, {
    'X-Idempotency-Key': checkoutKey,
  });
  const checkoutPayload = JSON.stringify({
    requestId: checkoutKey,
    shippingAddress: testData.addresses[0],
    recipientName: user.name,
    recipientPhone: user.phone,
    paymentMethod: 'VNPAY',
    voucherCode: 'QUICKSHOP10',
    shopVoucherCodes: { '1': 'SHOP1_SALE10' },
    items: [{ variantId: variant.variantId, quantity: 1 }],
  });
  const checkoutRes = http.post(`${BASE_URL}/api/orders/checkout`, checkoutPayload, { headers: checkoutHeaders });
  const checkoutOk = check(checkoutRes, {
    'POST /api/orders/checkout status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });
  smokeSuccessRate.add(checkoutOk);

  sleep(0.5);
}
