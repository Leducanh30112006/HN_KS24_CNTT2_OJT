# QuickShop - Ma Trận Phân Quyền (RBAC Security Matrix) & Bảo Mật API Gateway

## 1. Tổng quan Kiến trúc Bảo mật API Gateway

Hệ thống **QuickShop** áp dụng mô hình kiến trúc **API Gateway Pattern** làm chốt chặn duy nhất (Single Point of Entry) bảo vệ toàn bộ mạng lưới microservices nội bộ. Mọi yêu cầu từ máy khách (Web SPA, Mobile App, Postman) đều phải đi qua **API Gateway** để:
- **Dual Authentication**: Tự động nhận diện Access Token từ cả **Header** (`Authorization: Bearer <token>`) và **HttpOnly Cookie** (`accessToken`).
- **Phân định & Ngăn chặn Token**: Chặn hoàn toàn việc sử dụng nhầm Refresh Token để truy cập tài nguyên nghiệp vụ.
- **Kiểm soát Truy cập Dựa trên Vai trò (RBAC)**: Đối chiếu vai trò người dùng trong JWT Claims với cấu hình `requiredRole` hoặc danh sách `allowedRoles`.
- **Chuẩn hóa Phản hồi Lỗi (Standardized Error Responses)**: Chuẩn hóa 100% các lỗi bảo mật thành cấu trúc JSON `ApiResponse` đồng nhất (`401 Unauthorized`, `403 Forbidden`).
- **Header Propagation**: Sau khi xác thực hợp lệ, Gateway bổ sung các headers định danh an toàn (`X-Auth-Username`, `X-User-Id`, `X-User-Email`, `X-User-Role`) chuyển tiếp tới các microservices phía sau.

---

## 2. Các Vai Trò Trong Hệ Thống (Roles)

| Vai trò (Role) | Ký hiệu hệ thống | Phạm vi & Trách nhiệm |
| :--- | :--- | :--- |
| **Khách vãng lai** | `GUEST` / `ANONYMOUS` | Người dùng chưa đăng nhập. Chỉ được xem danh mục, sản phẩm công khai, tìm kiếm và xem thông tin gian hàng. |
| **Người mua hàng** | `ROLE_BUYER` | Đã đăng nhập & xác thực OTP. Quản lý hồ sơ cá nhân, giỏ hàng, đặt hàng, thanh toán và đăng ký mở gian hàng (Seller KYC). |
| **Nhà bán hàng** | `ROLE_SELLER` | Gian hàng đã được Admin phê duyệt. Có toàn bộ quyền của Người mua hàng, cộng thêm quyền quản lý kho hàng, tạo sản phẩm, quản lý SubOrder của Shop. |
| **Quản trị viên** | `ROLE_ADMIN` | Quản trị tối cao toàn sàn: duyệt/từ chối hồ sơ Shop KYC, quản lý người dùng, cấu hình voucher sàn và giám sát hệ thống. |

---

## 3. Bảng Ma Trận Phân Quyền Chi Tiết (RBAC Matrix)

| Nhóm chức năng | Endpoint URL / Pattern | Method | Vai trò cho phép | Hành vi khi vi phạm |
| :--- | :--- | :---: | :---: | :---: |
| **Xác thực & OTP** | `/api/auth/**` | POST | `Mọi người (Public)` | Cho phép truy cập không điều kiện |
| **Duyệt Danh mục & Sản phẩm** | `/api/catalog/**` | GET | `Mọi người (Public)` | Cho phép xem sản phẩm, phân loại, review |
| **Xem Gian hàng Công khai** | `/api/shops`, `/api/shops/{id}`, `/api/shops/owner/{ownerId}` | GET | `Mọi người (Public)` | Khách vãng lai xem được thông tin shop |
| **Tài khoản cá nhân** | `/api/users/me` | GET, PUT | `BUYER`, `SELLER`, `ADMIN` | Thiếu token -> `401 Unauthorized` |
| **Đăng ký mở Gian hàng (KYC)** | `/api/shops/register` | POST | `BUYER`, `SELLER`, `ADMIN` | Thiếu token -> `401 Unauthorized` |
| **Giỏ hàng & Đơn hàng** | `/api/cart/**`, `/api/orders/**` | ALL | `BUYER`, `SELLER`, `ADMIN` | Thiếu token -> `401 Unauthorized` |
| **Thanh toán & Vận chuyển** | `/api/payments/**`, `/api/shipping/**` | ALL | `BUYER`, `SELLER`, `ADMIN` | Thiếu token -> `401 Unauthorized` |
| **Mã giảm giá Voucher** | `/api/vouchers/**` | ALL | `BUYER`, `SELLER`, `ADMIN` | Thiếu token -> `401 Unauthorized` |
| **Quản lý Sản phẩm Shop** | `/api/catalog/products/manage/**` | POST, PUT, DELETE | `SELLER`, `ADMIN` | Buyer truy cập -> `403 Forbidden` |
| **Phê duyệt / Từ chối Shop KYC**| `/api/shops/{id}/approve`, `/api/shops/{id}/reject` | PUT | `ADMIN` | Buyer/Seller truy cập -> `403 Forbidden` |
| **Quản trị toàn bộ Người dùng** | `/api/users` | GET | `ADMIN` | Buyer/Seller truy cập -> `403 Forbidden` |

---

## 4. Chuẩn hóa Cấu trúc Phản hồi Lỗi Bảo mật

### A. Lỗi 401 Unauthorized (Chưa xác thực danh tính)
Xảy ra khi yêu cầu không có token, token hết hạn, sai chữ ký, hoặc gửi nhầm Refresh Token.
```json
{
  "success": false,
  "message": "Yêu cầu chưa được xác thực (Thiếu Token hoặc Cookie)",
  "data": null,
  "timestamp": 1730784000000
}
```

### B. Lỗi 403 Forbidden (Không đủ quyền truy cập)
Xảy ra khi người dùng đã đăng nhập hợp lệ nhưng vai trò không nằm trong danh sách cho phép của tài nguyên đó (Ví dụ: Buyer cố tình gọi API duyệt Shop của Admin).
```json
{
  "success": false,
  "message": "Bạn không có quyền truy cập tài nguyên này (Yêu cầu vai trò phù hợp)",
  "data": null,
  "timestamp": 1730784000000
}
```

---

## 5. Danh mục Kiểm thử Tự động (Automated Verification)

Toàn bộ ma trận bảo mật và phân quyền được kiểm thử tự động 100% trong:
1. `api-gateway/src/test/java/com/quickshop/gateway/filter/JwtAuthGatewayFilterFactoryTest.java`:
   - Kiểm tra trích xuất Token qua Bearer Header và Cookie `accessToken`.
   - Kiểm tra từ chối Token hết hạn, chữ ký giả mạo và Refresh Token.
   - Kiểm tra chuẩn hóa tiền tố `ROLE_` (`SELLER` tương đương `ROLE_SELLER`).
2. `api-gateway/src/test/java/com/quickshop/gateway/filter/RbacSecurityMatrixTest.java`:
   - Kiểm tra 4 ma trận bảo vệ: Admin-only, Seller & Admin, Người dùng chung (Buyer/Seller/Admin) và Dual Auth.
3. `api-gateway/src/test/java/com/quickshop/gateway/exception/GatewayGlobalExceptionHandlerTest.java`:
   - Kiểm tra chuẩn hóa toàn bộ lỗi WebFlux (401, 403, 404, 503, 500).
