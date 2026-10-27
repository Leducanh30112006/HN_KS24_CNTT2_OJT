import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const testData = JSON.parse(open('../data/test-data.json'));
const BASE_URL = __ENV.BASE_URL || testData.baseUrl || 'http://localhost:8083';

// BẢO VỆ PRODUCTION (Yêu cầu số 9)
if ((BASE_URL.includes('quickshop.com') || BASE_URL.includes('api.quickshop.vn')) && __ENV.ALLOW_PROD_TEST !== 'true') {
  throw new Error('[CRITICAL] Không được phép chạy kiểm thử tải trên production!');
}

const writeDuration = new Trend('write_duration', true);
const checkoutDuration = new Trend('checkout_duration', true);
const writeSuccessRate = new Rate('write_success_rate');
const ordersCreated = new Counter('orders_created');

const targetRps = parseInt(__ENV.TARGET_RPS || '200', 10);

export const options = {
  scenarios: {
    write_heavy: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 300,
      stages: [
        { target: Math.round(targetRps * 0.25), duration: '10s' }, // 50 RPS
        { target: Math.round(targetRps * 0.60), duration: '15s' }, // 120 RPS
        { target: targetRps, duration: '20s' },                    // 200 RPS Peak
        { target: targetRps, duration: '15s' },                    // Duy trì
        { target: 10, duration: '5s' },                            // Cooldown
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.03'],
    http_req_duration: ['p(50)<150', 'p(95)<500', 'p(99)<900'],
    checkout_duration: ['p(95)<700'],
    write_success_rate: ['rate>0.97'],
  },
};

function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function generateIdempotencyKey(prefix = 'req') {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000000)}`;
}

export default function () {
  const user = getRandomElement(testData.users);
  const variant = getRandomElement(testData.variants);
  const address = getRandomElement(testData.addresses);
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': String(user.id),
    'X-User-Role': user.role,
  };

  const roll = Math.random();

  if (roll < 0.50) {
    // 50% Thêm sản phẩm vào giỏ hàng
    const payload = JSON.stringify({
      variantId: variant.variantId,
      quantity: 1,
    });
    const res = http.post(`${BASE_URL}/api/cart/items`, payload, { headers });
    const ok = check(res, { 'POST /api/cart/items 200 or 201': (r) => r.status === 200 || r.status === 201 });
    writeDuration.add(res.timings.duration);
    writeSuccessRate.add(ok);

  } else if (roll < 0.75) {
    // 25% Hợp nhất giỏ hàng Guest Cart
    const key = generateIdempotencyKey('merge');
    const mergeHeaders = Object.assign({}, headers, { 'X-Idempotency-Key': key });
    const payload = JSON.stringify({
      requestId: key,
      items: [{ variantId: variant.variantId, quantity: 1 }],
    });
    const res = http.post(`${BASE_URL}/api/cart/merge`, payload, { headers: mergeHeaders });
    const ok = check(res, { 'POST /api/cart/merge 200': (r) => r.status === 200 });
    writeDuration.add(res.timings.duration);
    writeSuccessRate.add(ok);

  } else {
    // 25% Checkout đơn hàng đa shop
    const checkoutKey = generateIdempotencyKey('chk');
    const chkHeaders = Object.assign({}, headers, { 'X-Idempotency-Key': checkoutKey });
    const payload = JSON.stringify({
      requestId: checkoutKey,
      shippingAddress: address,
      recipientName: user.name,
      recipientPhone: user.phone,
      paymentMethod: 'VNPAY',
      voucherCode: 'QUICKSHOP10',
      shopVoucherCodes: { '1': 'SHOP1_SALE10' },
      items: [{ variantId: variant.variantId, quantity: 1 }],
    });

    const res = http.post(`${BASE_URL}/api/orders/checkout`, payload, { headers: chkHeaders });
    const ok = check(res, { 'POST /api/orders/checkout 200 or 201': (r) => r.status === 200 || r.status === 201 });
    writeDuration.add(res.timings.duration);
    checkoutDuration.add(res.timings.duration);
    writeSuccessRate.add(ok);
    if (ok) {
      ordersCreated.add(1);
    }
  }
}
