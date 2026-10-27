import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const testData = JSON.parse(open('../data/test-data.json'));
const BASE_URL = __ENV.BASE_URL || testData.baseUrl || 'http://localhost:8083';

// BẢO VỆ PRODUCTION (Yêu cầu số 9)
if ((BASE_URL.includes('quickshop.com') || BASE_URL.includes('api.quickshop.vn')) && __ENV.ALLOW_PROD_TEST !== 'true') {
  throw new Error('[CRITICAL] Không được phép chạy kiểm thử tải trên production!');
}

const readDuration = new Trend('read_duration', true);
const readSuccessRate = new Rate('read_success_rate');
const cartReads = new Counter('cart_reads');
const voucherReads = new Counter('voucher_reads');

const targetRps = parseInt(__ENV.TARGET_RPS || '800', 10);

export const options = {
  scenarios: {
    read_heavy: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 500,
      stages: [
        { target: Math.round(targetRps * 0.25), duration: '10s' }, // 200 RPS
        { target: Math.round(targetRps * 0.60), duration: '15s' }, // 480 RPS
        { target: targetRps, duration: '20s' },                    // 800 RPS Peak
        { target: targetRps, duration: '15s' },                    // Duy trì
        { target: 50, duration: '5s' },                            // Cooldown
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(50)<80', 'p(95)<250', 'p(99)<500'],
    read_duration: ['p(95)<250'],
    read_success_rate: ['rate>0.99'],
  },
};

function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const user = getRandomElement(testData.users);
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': String(user.id),
    'X-User-Role': user.role,
  };

  const roll = Math.random();

  if (roll < 0.50) {
    // 50% Đọc giỏ hàng từ Redis
    const res = http.get(`${BASE_URL}/api/cart`, { headers });
    const ok = check(res, { 'GET /api/cart 200': (r) => r.status === 200 });
    readDuration.add(res.timings.duration);
    readSuccessRate.add(ok);
    cartReads.add(1);
  } else if (roll < 0.80) {
    // 30% Tra cứu voucher
    const voucherList = testData.vouchers.platform.concat(testData.vouchers.shops['1']);
    const voucher = getRandomElement(voucherList);
    const res = http.get(`${BASE_URL}/api/vouchers/${voucher}`, { headers });
    const ok = check(res, { 'GET /api/vouchers 200': (r) => r.status === 200 });
    readDuration.add(res.timings.duration);
    readSuccessRate.add(ok);
    voucherReads.add(1);
  } else {
    // 20% Preview voucher
    const payload = JSON.stringify({
      code: 'QUICKSHOP10',
      shopId: null,
      orderAmount: 350000,
    });
    const res = http.post(`${BASE_URL}/api/vouchers/preview`, payload, { headers });
    const ok = check(res, { 'POST /api/vouchers/preview 200': (r) => r.status === 200 });
    readDuration.add(res.timings.duration);
    readSuccessRate.add(ok);
    voucherReads.add(1);
  }
}
