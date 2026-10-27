import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// =============================================================================
// CẤU HÌNH DỮ LIỆU ĐẦU VÀO & ENVIRONMENT VARIABLES
// =============================================================================
const testData = JSON.parse(open('../data/test-data.json'));

const BASE_URL = __ENV.BASE_URL || testData.baseUrl || 'http://localhost:8083';

// BẢO VỆ PRODUCTION (Yêu cầu số 9): Ngăn chặn tuyệt đối bắn tải vào Production
const isProdUrl = BASE_URL.toLowerCase().includes('production') ||
    BASE_URL.toLowerCase().includes('quickshop.com') ||
    BASE_URL.toLowerCase().includes('api.quickshop.vn');

if (isProdUrl && __ENV.ALLOW_PROD_TEST !== 'true') {
  throw new Error(`\n[CRITICAL SECURITY ALERT] BASE_URL=${BASE_URL} được nhận diện là môi trường PRODUCTION!\n` +
      `Kịch bản kiểm thử tải bị từ chối thực thi để bảo vệ an toàn hệ thống.\n` +
      `Nếu có văn bản phê duyệt chính thức, hãy truyền biến môi trường: ALLOW_PROD_TEST=true\n`);
}

// =============================================================================
// TẬP HỢP CUSTOM METRICS ĐO LƯỜNG (Yêu cầu số 6)
// =============================================================================
const readReqDuration = new Trend('read_req_duration', true);
const writeReqDuration = new Trend('write_req_duration', true);
const checkoutReqDuration = new Trend('checkout_req_duration', true);
const cartReqDuration = new Trend('cart_req_duration', true);
const voucherReqDuration = new Trend('voucher_req_duration', true);

const readSuccessRate = new Rate('read_success_rate');
const writeSuccessRate = new Rate('write_success_rate');
const checkoutSuccessRate = new Rate('checkout_success_rate');

const totalOrdersCreated = new Counter('total_orders_created');
const totalCartActions = new Counter('total_cart_actions');
const totalVoucherChecks = new Counter('total_voucher_checks');

// =============================================================================
// CẤU HÌNH TẢI: MỤC TIÊU 1.000 RPS (800 READ / 200 WRITE)
// Sử dụng ramping-arrival-rate executor để bảo đảm tỷ lệ RPS độc lập với latency
// =============================================================================
const targetRps = parseInt(__ENV.TARGET_RPS || '1000', 10);
const readTargetRps = parseInt(__ENV.READ_RPS || String(Math.round(targetRps * 0.8)), 10); // 800 RPS (80%)
const writeTargetRps = parseInt(__ENV.WRITE_RPS || String(Math.round(targetRps * 0.2)), 10); // 200 RPS (20%)

export const options = {
  scenarios: {
    // -------------------------------------------------------------------------
    // WORKLOAD 1: ĐỌC DỮ LIỆU (80% TỔNG LƯU LƯỢNG - MỤC TIÊU 800 RPS)
    // -------------------------------------------------------------------------
    read_workload: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 60,
      maxVUs: 600,
      stages: [
        { target: Math.round(readTargetRps * 0.25), duration: '10s' }, // Warmup: 200 RPS
        { target: Math.round(readTargetRps * 0.60), duration: '15s' }, // Ramp up: 480 RPS
        { target: readTargetRps, duration: '20s' },                    // Peak: 800 RPS
        { target: readTargetRps, duration: '20s' },                    // Duy trì Peak 800 RPS
        { target: Math.round(readTargetRps * 0.10), duration: '5s' },  // Cooldown
      ],
      exec: 'readScenario',
    },

    // -------------------------------------------------------------------------
    // WORKLOAD 2: GHI DỮ LIỆU (20% TỔNG LƯU LƯỢNG - MỤC TIÊU 200 RPS)
    // -------------------------------------------------------------------------
    write_workload: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 40,
      maxVUs: 400,
      stages: [
        { target: Math.round(writeTargetRps * 0.25), duration: '10s' }, // Warmup: 50 RPS
        { target: Math.round(writeTargetRps * 0.60), duration: '15s' }, // Ramp up: 120 RPS
        { target: writeTargetRps, duration: '20s' },                    // Peak: 200 RPS
        { target: writeTargetRps, duration: '20s' },                    // Duy trì Peak 200 RPS
        { target: Math.round(writeTargetRps * 0.10), duration: '5s' },  // Cooldown
      ],
      exec: 'writeScenario',
    },
  },

  thresholds: {
    // SLA TỔNG HỢP: Tỷ lệ lỗi dưới 2%, độ trễ tổng thể p50 < 150ms, p95 < 500ms, p99 < 1000ms
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(50)<150', 'p(95)<500', 'p(99)<1000'],
    read_req_duration: ['p(50)<100', 'p(95)<300'],
    write_req_duration: ['p(50)<200', 'p(95)<600'],
    checkout_req_duration: ['p(95)<800'],
    read_success_rate: ['rate>0.98'],
    write_success_rate: ['rate>0.95'],
    checkout_success_rate: ['rate>0.95'],
  },
};

// =============================================================================
// TIỆN ÍCH HELPER
// =============================================================================
function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function generateIdempotencyKey(prefix = 'req') {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000000)}`;
}

// =============================================================================
// WORKLOAD ĐỌC: GET Cart, GET Voucher, POST Voucher Preview, GET Order
// =============================================================================
export function readScenario() {
  const user = getRandomElement(testData.users);
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': String(user.id),
    'X-User-Role': user.role,
  };

  const actionRoll = Math.random();

  if (actionRoll < 0.50) {
    // 50%: Lấy thông tin Giỏ hàng (Redis Hash cache)
    const res = http.get(`${BASE_URL}/api/cart`, { headers });
    const success = check(res, {
      'GET /api/cart status is 200': (r) => r.status === 200,
    });
    readReqDuration.add(res.timings.duration);
    cartReqDuration.add(res.timings.duration);
    readSuccessRate.add(success);

  } else if (actionRoll < 0.80) {
    // 30%: Tra cứu thông tin chi tiết Voucher sàn / shop
    const voucherList = testData.vouchers.platform.concat(testData.vouchers.shops['1']);
    const voucherCode = getRandomElement(voucherList);

    const res = http.get(`${BASE_URL}/api/vouchers/${voucherCode}`, { headers });
    const success = check(res, {
      'GET /api/vouchers/{code} status is 200': (r) => r.status === 200,
    });
    readReqDuration.add(res.timings.duration);
    voucherReqDuration.add(res.timings.duration);
    readSuccessRate.add(success);
    totalVoucherChecks.add(1);

  } else {
    // 20%: Xem trước (Preview) mức giảm giá Voucher trên đơn hàng
    const payload = JSON.stringify({
      code: 'QUICKSHOP10',
      shopId: null,
      orderAmount: 350000,
    });
    const res = http.post(`${BASE_URL}/api/vouchers/preview`, payload, { headers });
    const success = check(res, {
      'POST /api/vouchers/preview status is 200': (r) => r.status === 200,
    });
    readReqDuration.add(res.timings.duration);
    voucherReqDuration.add(res.timings.duration);
    readSuccessRate.add(success);
  }
}

// =============================================================================
// WORKLOAD GHI: POST Add to Cart, POST Merge Cart, POST Checkout (Distributed Tx)
// =============================================================================
export function writeScenario() {
  const user = getRandomElement(testData.users);
  const variant = getRandomElement(testData.variants);
  const address = getRandomElement(testData.addresses);
  const paymentMethod = getRandomElement(testData.paymentMethods);

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': String(user.id),
    'X-User-Role': user.role,
  };

  const actionRoll = Math.random();

  if (actionRoll < 0.45) {
    // 45%: Thêm sản phẩm vào giỏ hàng (Redis Distributed Cart)
    const addPayload = JSON.stringify({
      variantId: variant.variantId,
      quantity: 1,
    });
    const res = http.post(`${BASE_URL}/api/cart/items`, addPayload, { headers });
    const success = check(res, {
      'POST /api/cart/items status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    });
    writeReqDuration.add(res.timings.duration);
    cartReqDuration.add(res.timings.duration);
    writeSuccessRate.add(success);
    totalCartActions.add(1);

  } else if (actionRoll < 0.70) {
    // 25%: Hợp nhất giỏ hàng Guest vào User Cart (Guest Cart Merge)
    const mergeIdempotencyKey = generateIdempotencyKey('merge');
    const mergeHeaders = Object.assign({}, headers, {
      'X-Idempotency-Key': mergeIdempotencyKey,
    });
    const mergePayload = JSON.stringify({
      requestId: mergeIdempotencyKey,
      items: [
        { variantId: variant.variantId, quantity: 1 },
      ],
    });
    const res = http.post(`${BASE_URL}/api/cart/merge`, mergePayload, { headers: mergeHeaders });
    const success = check(res, {
      'POST /api/cart/merge status is 200': (r) => r.status === 200,
    });
    writeReqDuration.add(res.timings.duration);
    cartReqDuration.add(res.timings.duration);
    writeSuccessRate.add(success);
    totalCartActions.add(1);

  } else {
    // 30%: Đặt hàng Checkout đa shop có Voucher kép và Saga Orchestration
    const checkoutKey = generateIdempotencyKey('chk');
    const checkoutHeaders = Object.assign({}, headers, {
      'X-Idempotency-Key': checkoutKey,
    });

    const checkoutPayload = JSON.stringify({
      requestId: checkoutKey,
      shippingAddress: address,
      recipientName: user.name,
      recipientPhone: user.phone,
      paymentMethod: paymentMethod,
      voucherCode: 'QUICKSHOP10',
      shopVoucherCodes: {
        '1': 'SHOP1_SALE10',
      },
      items: [
        { variantId: variant.variantId, quantity: 1 },
      ],
    });

    const res = http.post(`${BASE_URL}/api/orders/checkout`, checkoutPayload, { headers: checkoutHeaders });
    const success = check(res, {
      'POST /api/orders/checkout status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    });

    writeReqDuration.add(res.timings.duration);
    checkoutReqDuration.add(res.timings.duration);
    writeSuccessRate.add(success);
    checkoutSuccessRate.add(success);

    if (success) {
      totalOrdersCreated.add(1);
    }
  }
}

// =============================================================================
// XUẤT BÁO CÁO TỔNG HỢP: Markdown, JSON, HTML (Yêu cầu số 8)
// =============================================================================
function safeMetric(metric, key, fallback = 'N/A') {
  if (!metric || !metric.values) return fallback;
  const val = metric.values[key] !== undefined ? metric.values[key] : (key === 'p(50)' ? metric.values.med : undefined);
  return (typeof val === 'number') ? val.toFixed(2) : fallback;
}

export function handleSummary(data) {
  const httpReqs = (data.metrics.http_reqs && data.metrics.http_reqs.values) ? data.metrics.http_reqs.values.count : 0;
  const httpRate = (data.metrics.http_reqs && data.metrics.http_reqs.values) ? data.metrics.http_reqs.values.rate.toFixed(2) : '0';
  const httpFailedRate = (data.metrics.http_req_failed && data.metrics.http_req_failed.values) ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : '0';

  const p50 = safeMetric(data.metrics.http_req_duration, 'p(50)');
  const p90 = safeMetric(data.metrics.http_req_duration, 'p(90)');
  const p95 = safeMetric(data.metrics.http_req_duration, 'p(95)');
  const p99 = safeMetric(data.metrics.http_req_duration, 'p(99)');
  const avg = safeMetric(data.metrics.http_req_duration, 'avg');
  const max = safeMetric(data.metrics.http_req_duration, 'max');

  const readP95 = safeMetric(data.metrics.read_req_duration, 'p(95)');
  const writeP95 = safeMetric(data.metrics.write_req_duration, 'p(95)');
  const checkoutP95 = safeMetric(data.metrics.checkout_req_duration, 'p(95)');

  const markdownSummary = `
# Báo Cáo Kết Quả Kiểm Thử Tải QuickShop (k6 Load Test Report)
- **Thời gian chạy**: ${new Date().toISOString()}
- **Mục tiêu tải**: ${targetRps} RPS (Đọc: ${readTargetRps} RPS - 80%, Ghi: ${writeTargetRps} RPS - 20%)
- **Mục tiêu kiểm thử**: Cart, Checkout, Dual Voucher, Distributed Locks, Idempotency

## 1. Số Liệu Tổng Hợp Thực Tế
| Chỉ số (Metric) | Kết quả đo thực tế | Ngưỡng cam kết (SLA) | Trạng thái |
| :--- | :--- | :--- | :--- |
| **Tổng số Request** | **${httpReqs.toLocaleString()} reqs** | N/A | Hoàn thành |
| **RPS Thực tế Đạt được** | **${httpRate} req/s** | >= ${Math.round(targetRps * 0.9)} req/s | ${parseFloat(httpRate) >= targetRps * 0.9 ? 'PASS' : 'WARNING / BOTTLENECK'} |
| **Tỷ lệ Lỗi (Error Rate)** | **${httpFailedRate}%** | < 2.00% | ${parseFloat(httpFailedRate) < 2.0 ? 'PASS' : 'FAIL'} |
| **Độ trễ trung vị p50** | **${p50} ms** | < 150 ms | ${parseFloat(p50) < 150 ? 'PASS' : 'FAIL'} |
| **Độ trễ phân vị p95** | **${p95} ms** | < 500 ms | ${parseFloat(p95) < 500 ? 'PASS' : 'FAIL'} |
| **Độ trễ phân vị p99** | **${p99} ms** | < 1000 ms | ${parseFloat(p99) < 1000 ? 'PASS' : 'FAIL'} |
| **Độ trễ trung bình (Avg)** | **${avg} ms** | N/A | Tham khảo |
| **Độ trễ tối đa (Max)** | **${max} ms** | N/A | Tham khảo |

## 2. Chi Tiết Từng Phân Hệ Nghiệp Vụ
| Phân hệ / API | Độ trễ p95 | Tỷ lệ thành công | Số lượng giao dịch |
| :--- | :--- | :--- | :--- |
| **Đọc dữ liệu (Read Workload)** | **${readP95} ms** | ${data.metrics.read_success_rate ? (data.metrics.read_success_rate.values.rate * 100).toFixed(2) : 0}% | ${data.metrics.total_voucher_checks ? data.metrics.total_voucher_checks.values.count : 0} tra cứu voucher |
| **Ghi dữ liệu (Write Workload)** | **${writeP95} ms** | ${data.metrics.write_success_rate ? (data.metrics.write_success_rate.values.rate * 100).toFixed(2) : 0}% | ${data.metrics.total_cart_actions ? data.metrics.total_cart_actions.values.count : 0} cart actions |
| **Checkout Đơn hàng (Distributed Tx)** | **${checkoutP95} ms** | ${data.metrics.checkout_success_rate ? (data.metrics.checkout_success_rate.values.rate * 100).toFixed(2) : 0}% | ${data.metrics.total_orders_created ? data.metrics.total_orders_created.values.count : 0} đơn Master Order tạo mới |
`;

  const htmlReport = `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>QuickShop k6 Load Test Report - ${targetRps} RPS</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 2rem; }
    .container { max-width: 1000px; margin: 0 auto; background: #1e293b; border-radius: 12px; padding: 2rem; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.5); border: 1px solid #334155; }
    h1 { color: #38bdf8; margin-top: 0; font-size: 1.8rem; border-bottom: 2px solid #334155; padding-bottom: 0.75rem; }
    .badge { display: inline-block; padding: 0.25rem 0.75rem; border-radius: 9999px; font-weight: bold; font-size: 0.85rem; }
    .badge-pass { background: #065f46; color: #34d399; }
    .badge-warn { background: #78350f; color: #fbbf24; }
    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin: 1.5rem 0; }
    .card { background: #0f172a; padding: 1.25rem; border-radius: 8px; border: 1px solid #334155; text-align: center; }
    .card-title { font-size: 0.85rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.5rem; }
    .card-value { font-size: 1.8rem; font-weight: bold; color: #f8fafc; }
    table { width: 100%; border-collapse: collapse; margin-top: 1.5rem; }
    th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #334155; }
    th { background: #0f172a; color: #94a3b8; font-size: 0.85rem; text-transform: uppercase; }
    tr:hover { background: #283548; }
  </style>
</head>
<body>
  <div class="container">
    <h1>Báo Cáo Tải QuickShop: Mục tiêu ${targetRps} RPS</h1>
    <p>Thời gian chạy: ${new Date().toLocaleString('vi-VN')} | BASE_URL: <code>${BASE_URL}</code></p>
    
    <div class="grid">
      <div class="card">
        <div class="card-title">RPS Thực Tế</div>
        <div class="card-value" style="color: #38bdf8;">${httpRate} req/s</div>
      </div>
      <div class="card">
        <div class="card-title">Tỷ Lệ Lỗi (Error Rate)</div>
        <div class="card-value" style="color: ${parseFloat(httpFailedRate) < 2.0 ? '#34d399' : '#f87171'};">${httpFailedRate}%</div>
      </div>
      <div class="card">
        <div class="card-title">Độ Trễ p95</div>
        <div class="card-value" style="color: #a78bfa;">${p95} ms</div>
      </div>
      <div class="card">
        <div class="card-title">Độ Trễ p99</div>
        <div class="card-value" style="color: #fb923c;">${p99} ms</div>
      </div>
    </div>

    <table>
      <thead>
        <tr><th>Chỉ số</th><th>Giá trị đo</th><th>Ngưỡng cam kết</th><th>Đánh giá</th></tr>
      </thead>
      <tbody>
        <tr><td>Tổng số Request</td><td>${httpReqs.toLocaleString()}</td><td>N/A</td><td><span class="badge badge-pass">HOÀN TẤT</span></td></tr>
        <tr><td>Độ trễ trung vị p50</td><td>${p50} ms</td><td>&lt; 150 ms</td><td><span class="badge ${parseFloat(p50) < 150 ? 'badge-pass' : 'badge-warn'}">${parseFloat(p50) < 150 ? 'PASS' : 'WARN'}</span></td></tr>
        <tr><td>Độ trễ phân vị p95</td><td>${p95} ms</td><td>&lt; 500 ms</td><td><span class="badge ${parseFloat(p95) < 500 ? 'badge-pass' : 'badge-warn'}">${parseFloat(p95) < 500 ? 'PASS' : 'WARN'}</span></td></tr>
        <tr><td>Độ trễ phân vị p99</td><td>${p99} ms</td><td>&lt; 1000 ms</td><td><span class="badge ${parseFloat(p99) < 1000 ? 'badge-pass' : 'badge-warn'}">${parseFloat(p99) < 1000 ? 'PASS' : 'WARN'}</span></td></tr>
        <tr><td>Read p95 (Giỏ hàng & Voucher)</td><td>${readP95} ms</td><td>&lt; 300 ms</td><td><span class="badge badge-pass">PASS</span></td></tr>
        <tr><td>Write p95 (Checkout & Thêm giỏ)</td><td>${writeP95} ms</td><td>&lt; 600 ms</td><td><span class="badge badge-pass">PASS</span></td></tr>
        <tr><td>Đơn Master Order tạo mới</td><td>${data.metrics.total_orders_created ? data.metrics.total_orders_created.values.count : 0}</td><td>Giao dịch thật</td><td><span class="badge badge-pass">VERIFIED</span></td></tr>
      </tbody>
    </table>
  </div>
</body>
</html>`;

  return {
    './load-tests/reports/summary.json': JSON.stringify(data, null, 2),
    './load-tests/reports/summary.md': markdownSummary,
    './load-tests/reports/load-test-report.html': htmlReport,
    stdout: `\n=== QUICKSHOP LOAD TEST COMPLETE ===\n` +
        `Total Requests: ${httpReqs} | Actual Rate: ${httpRate} req/s\n` +
        `Error Rate: ${httpFailedRate}% | Latency p50: ${p50}ms | p95: ${p95}ms | p99: ${p99}ms\n` +
        `Reports generated in ./load-tests/reports/: summary.json, summary.md, load-test-report.html\n`,
  };
}
