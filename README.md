# UrbanMart Backend

Web bán thực phẩm đa chi nhánh ứng dụng AI hỗ trợ mua sắm thông minh.

## Yêu cầu hệ thống

Trước khi bắt đầu, cài đặt các phần mềm sau:

| Phần mềm | Phiên bản | Dùng để |
|---|---|---|
| [Docker Desktop](https://www.docker.com/) | mới nhất | Chạy PostgreSQL + Redis + Adminer |
| [JDK](https://adoptium.net/) | 21 | Chạy Spring Boot |
| Maven | 3.9+ (hoặc dùng `./mvnw` có sẵn trong repo, không cần cài riêng) | Build project |
| [Git](https://git-scm.com/) | mới nhất | Clone code |

**Lưu ý quan trọng**: nếu máy bạn đã từng cài PostgreSQL trực tiếp (không qua Docker), nó có thể chiếm sẵn cổng `5432` và gây lỗi kết nối nhầm. Kiểm tra bằng `Get-NetTCPConnection -LocalPort 5432` (PowerShell) — nếu có, tắt service đó (Services → tìm "postgresql-x64-..." → Stop) trước khi qua bước tiếp theo.

## Bước 1 — Clone code

```bash
git clone <đường-dẫn-repo-của-nhóm>
cd UrbanMart
```

## Bước 2 — Khởi động PostgreSQL + Redis bằng Docker

```bash
docker compose up -d
```

Kiểm tra 3 container đã chạy:

```bash
docker ps
```

Phải thấy `urbanmart-db`, `urbanmart-redis`, `urbanmart-adminer` ở trạng thái `Up`. Adminer (giao diện xem DB qua web) chạy ở `http://localhost:8081`.

## Bước 3 — Nạp schema (chạy migration)

Các file migration nằm trong `db/migrations/`, đặt tên `V1`, `V2`... và **phải chạy đúng thứ tự** vì file sau phụ thuộc bảng/kiểu dữ liệu của file trước.

**Trên macOS/Linux:**
```bash
cd db/migrations
for f in V1__extensions_and_enums.sql V2__users_and_addresses.sql V3__branches_catalog_inventory.sql V4__wishlist_and_cart.sql V5__cart_uniqueness_constraints.sql V6__product_discounts.sql V7__daily_fresh_products.sql V8__product_sku_sequence.sql V9__orders_payments_and_promotions.sql V10__inventory_reservations_and_movements.sql V11__backfill_branch_inventory_products.sql V12__branch_inventory_expiry_lots.sql V13__order_handover_confirmation.sql V14__voucher_details_and_conditions.sql; do
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1 < "$f" || break
done
```

**Trên Windows (PowerShell)** — `<` không dùng được như Linux, phải dùng `Get-Content`:
```powershell
cd db\migrations
$files = "V1__extensions_and_enums.sql","V2__users_and_addresses.sql","V3__branches_catalog_inventory.sql","V4__wishlist_and_cart.sql","V5__cart_uniqueness_constraints.sql","V6__product_discounts.sql","V7__daily_fresh_products.sql","V8__product_sku_sequence.sql","V9__orders_payments_and_promotions.sql","V10__inventory_reservations_and_movements.sql","V11__backfill_branch_inventory_products.sql","V12__branch_inventory_expiry_lots.sql","V13__order_handover_confirmation.sql","V14__voucher_details_and_conditions.sql"
foreach ($f in $files) {
    Get-Content -Raw $f | docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
    if ($LASTEXITCODE -ne 0) { break }
}
```

Nếu database đã chạy V1–V5, chỉ chạy migration mới:

```powershell
Get-Content -Raw .\db\migrations\V6__product_discounts.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

Nếu database đã chạy V1–V6, chỉ chạy migration mới:

```powershell
Get-Content -Raw .\db\migrations\V7__daily_fresh_products.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
Get-Content -Raw .\db\migrations\V8__product_sku_sequence.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

Nếu database đã chạy V1–V7 nhưng POST `/api/admin/products` báo `relation "product_sku_seq" does not exist`, chỉ cần chạy V8:

```powershell
Get-Content -Raw .\db\migrations\V8__product_sku_sequence.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

Kiểm tra các bảng:

```bash
docker exec -it urbanmart-db psql -U urbanmart_user -d urbanmart -c "\dt"
```

Các bảng chính gồm: `users`, `addresses`, `branches`, `categories`, `brands`, `products`, `branch_inventory`, `inventory_movements`, `wishlists`, `carts`, `cart_items`, `product_discounts`, `orders`, `order_items`, `payments`, `promotions`, `order_promotions`, `promotion_user_usage`.

Migration `V5` thêm ràng buộc để mỗi user chỉ có một giỏ hàng trên mỗi chi nhánh và mỗi sản phẩm chỉ xuất hiện một lần trong cùng giỏ hàng. Nếu database đã có dữ liệu trùng, cần xử lý các bản ghi đó trước khi chạy migration.

Migration `V6` tạo bảng `product_discounts`, giới hạn phần trăm và thời gian áp dụng, đồng thời không cho phép các đợt giảm giá của cùng sản phẩm chồng lấn. Migration này cũng bỏ các cột giảm giá cũ khỏi `products`; giá gốc vẫn nằm ở `products.base_price`.

Migration `V7` thêm cột `products.is_daily_fresh`, mặc định `false`, để quản lý nhóm sản phẩm hàng ngày.

Migration `V8` tạo sequence `product_sku_seq` dùng để sinh SKU tự động khi tạo sản phẩm và khởi tạo sequence tiếp nối mã SKU `SP...` hiện có.

Migration `V9` tạo schema đơn hàng, chi tiết đơn, payment attempts và voucher/redemption. Migration `V10` bổ sung số lượng giữ/ngưỡng tồn thấp và sổ lịch sử biến động tồn kho. Migration `V13` bổ sung trạng thái chờ khách xác nhận đã nhận hàng và thời điểm bàn giao. Chỉ chạy V9/V10 sau khi database đã áp dụng V1–V8; nếu đã có migration lịch sử riêng cho các bảng này, cần đối chiếu schema thực tế trước khi áp dụng.
Migration `V14` bổ sung tiêu đề, mô tả và điều kiện danh mục/đơn đầu tiên/ngày đặc biệt cho voucher.

Nếu database đã chạy đến V12, áp dụng V13 trước khi khởi động phiên bản code này:

```powershell
Get-Content -Raw .\db\migrations\V13__order_handover_confirmation.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

Nếu database đã chạy đến V13, áp dụng V14 trước khi khởi động phiên bản code này:

```powershell
Get-Content -Raw .\db\migrations\V14__voucher_details_and_conditions.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

Nếu database đã chạy V1–V6, chạy các migration V7–V10 trước khi dùng code mới:

```powershell
Get-Content -Raw .\db\migrations\V7__daily_fresh_products.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
Get-Content -Raw .\db\migrations\V8__product_sku_sequence.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
Get-Content -Raw .\db\migrations\V9__orders_payments_and_promotions.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
Get-Content -Raw .\db\migrations\V10__inventory_reservations_and_movements.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

## Bước 3.1 — Nạp dữ liệu mẫu (không bắt buộc)

Chạy sau khi đã áp dụng migration V1–V10. Seed mẫu tạo hai tài khoản `BRANCH` (gắn lần lượt với chi nhánh Hà Đông và Cầu Giấy), chi nhánh, danh mục, thương hiệu, sản phẩm, tồn kho, nhóm sản phẩm hàng ngày và các đợt giảm giá đang hiệu lực/sắp diễn ra/đã kết thúc. Chỉ chạy một lần trên database trống vì các mã SKU và tên chi nhánh là duy nhất.

Tài khoản đăng nhập mẫu chỉ dùng cho dev/local:

| Chi nhánh | Số điện thoại | Mật khẩu |
|---|---|---|
| Hà Đông | `0900000001` | `Branch@123` |
| Cầu Giấy | `0900000002` | `Branch@123` |

**Trên macOS/Linux:**
```bash
docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1 < db/seed/seed-sample-data.sql
```

**Trên Windows (PowerShell):**
```powershell
Get-Content -Raw .\db\seed\seed-sample-data.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

Nếu đã chạy seed trước khi bổ sung tài khoản chi nhánh, **không chạy lại toàn bộ seed** vì các dữ liệu mẫu khác có thể bị trùng. Với database dev/local đã có hai chi nhánh mẫu, chạy đoạn SQL sau để tạo tài khoản (nếu chưa có) và gắn vào chi nhánh hiện tại:

```powershell
@'
BEGIN;

INSERT INTO users (full_name, phone, password_hash, role)
VALUES
    ('UrbanMart Ha Dong', '0900000001', crypt('Branch@123', gen_salt('bf')), 'BRANCH'),
    ('UrbanMart Cau Giay', '0900000002', crypt('Branch@123', gen_salt('bf')), 'BRANCH')
ON CONFLICT (phone) DO NOTHING;

UPDATE branches b
SET account_id = u.id
FROM users u
WHERE (b.name, u.phone) IN (
    ('UrbanMart Ha Dong', '0900000001'),
    ('UrbanMart Cau Giay', '0900000002')
)
AND u.role = 'BRANCH'
AND b.account_id IS NULL;

COMMIT;
'@ | docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1
```

Đoạn cập nhật chỉ gắn tài khoản cho chi nhánh đang chưa có `account_id`. Nếu số điện thoại mẫu đã được dùng bởi tài khoản không phải `BRANCH`, hoặc chi nhánh đã gắn với tài khoản khác, cần kiểm tra và xử lý dữ liệu đó trước. Có thể xác nhận kết quả bằng:

```sql
SELECT b.name, b.account_id, u.phone, u.role
FROM branches b
LEFT JOIN users u ON u.id = b.account_id
WHERE b.name IN ('UrbanMart Ha Dong', 'UrbanMart Cau Giay');
```

## Bước 4 — Kiểm tra dependency trong `pom.xml`

Ngoài các dependency mặc định lúc tạo project bằng Spring Initializr (Web, Data JPA, PostgreSQL Driver, Security, Validation, Lombok), cần có thêm:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>

<!-- Redis (OTP, du lieu tam) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## Bước 5 — Cấu hình `application.properties`

Copy file mẫu và điền thông tin (file thật không đưa lên Git để tránh lộ mật khẩu/secret):

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Nội dung mặc định đã khớp với `docker-compose.yml` của repo, thường không cần sửa gì ở môi trường dev local:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/urbanmart
spring.datasource.username=urbanmart_user
spring.datasource.password=urbanmart_password
spring.jpa.hibernate.ddl-auto=validate

spring.data.redis.host=localhost
spring.data.redis.port=6379

jwt.secret=day-la-chuoi-bi-mat-cua-ban-can-du-dai-it-nhat-32-ky-tu-doi-thanh-gia-tri-that
jwt.expiration-ms=86400000
```

**Lưu ý**: `jwt.secret` bắt buộc đủ dài (tối thiểu ~32 ký tự, do dùng thuật toán HS256) — dùng chuỗi ngắn hơn sẽ lỗi `WeakKeyException` lúc khởi động.

## Bước 6 — Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

Hoặc mở project bằng IntelliJ/VS Code rồi Run trực tiếp class `UrbanMartApplication`. Server mặc định chạy ở `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Test nhanh xác nhận chạy đúng:
```bash
curl http://localhost:8080/api/health
```

## API hiện có (tuần 4–7)

| Nhóm API | Endpoint / chức năng | Trạng thái |
|---|---|---|
| Đăng ký + xác thực OTP | `/api/auth/register`, `/verify-otp`, `/resend-otp` | ✅ Đã code |
| Đăng nhập, quên/đổi mật khẩu | `/api/auth/login`, `/forgot-password`, `/reset-password`, `/change-password` | ✅ Đã code |
| Sửa thông tin cá nhân | `/api/users/me` | ✅ Đã code |
| CRUD địa chỉ | `/api/addresses` | ✅ Đã code |
| Chi nhánh gần khách | `/api/branches/nearby` | ✅ Đã code |
| Danh mục và thương hiệu | `/api/categories`, `/api/brands` | ✅ Đã code |
| Sản phẩm theo chi nhánh | `/api/products`, `/api/products/{productId}` | ✅ Có phân trang, tìm kiếm, lọc và sắp xếp |
| Gợi ý tìm kiếm sản phẩm | `/api/products/search-suggestions` | ✅ Tìm theo tên hoặc SKU, trả tối đa 3 sản phẩm mặc định |
| Sản phẩm theo danh mục | `/api/products/category/{categoryId}` | ✅ Có thể lấy sản phẩm thuộc danh mục con |
| Sản phẩm theo danh mục (admin) | `/api/admin/products/category/{categoryId}` | ✅ Có phân trang, chỉ lấy danh mục được yêu cầu |
| Sản phẩm đang giảm giá | `/api/products/promotions` | ✅ Chỉ trả các đợt giảm đang hiệu lực |
| Sản phẩm hàng ngày | `/api/products/daily-fresh` | ✅ Lọc theo cờ `is_daily_fresh` |
| Danh sách/chi tiết voucher | `/api/vouchers`, `/api/vouchers/{id}` | ✅ Public; danh sách có phân trang và lọc |
| Áp dụng thử voucher | `/api/vouchers/apply` | ✅ CUSTOMER; tính theo giỏ và giá hiện tại |
| Quản lý voucher (admin) | `/api/admin/vouchers` | ✅ Tạo, cập nhật, vô hiệu hóa và lọc |
| CRUD đợt giảm giá (admin) | `/api/admin/product-discounts` | ✅ Chỉ role `ADMIN` |
| Sản phẩm bán chạy | Chưa triển khai | ⏳ Chưa làm theo phạm vi hiện tại |
| Yêu thích sản phẩm | `/api/wishlists` | ✅ Đã code |
| Giỏ hàng | `/api/cart` | ✅ Đã code |

### API danh sách sản phẩm tuần 4

Các API danh sách sản phẩm cần `branchId`; phân trang dùng `page` bắt đầu từ `0` và `size` từ `1` đến `100`. Các endpoint lấy sản phẩm trả `basePrice` (giá gốc), `displayPrice` (giá khách trả), thông tin giảm giá nếu đang có đợt hiệu lực và `dailyFresh` cho biết sản phẩm có thuộc nhóm hàng ngày không.

```http
GET /api/products?branchId={branchId}&page=0&size=20
GET /api/products/search-suggestions?keyword={keyword}&branchId={branchId}&limit=3
GET /api/products/category/{categoryId}?branchId={branchId}&includeDescendants=true&page=0&size=20
GET /api/products/promotions?branchId={branchId}&page=0&size=20
GET /api/products/daily-fresh?branchId={branchId}&page=0&size=20
```

API gợi ý tìm kiếm yêu cầu `keyword` và `branchId`, `limit` mặc định là `3` và chấp nhận từ `1` đến `10`. Kết quả chỉ gồm sản phẩm đang kinh doanh tại chi nhánh, cùng tên, ảnh, giá gốc, giá hiện tại, đơn vị và tên danh mục.

### Voucher

```http
GET    /api/vouchers?keyword={keyword}&isAvailableOnly=true&page=0&size=10
GET    /api/vouchers/{voucherId}
POST   /api/vouchers/apply
GET    /api/admin/vouchers?keyword={keyword}&isActive=true&page=0&size=20
GET    /api/admin/vouchers/{voucherId}
POST   /api/admin/vouchers
PUT    /api/admin/vouchers/{voucherId}
DELETE /api/admin/vouchers/{voucherId}
```

Request tạo/cập nhật nhận `code`, `title`, `description`, `discountType` (`FIXED` hoặc `PERCENT`), `discountValue`, `maxDiscountAmount`, `minOrderAmount`, `startDate`, `endDate`, `branchId` (tùy chọn), `usageLimit`, `userLimit`, `isActive` và `conditions`. Trong `conditions`, có thể truyền `isFirstOrder`, `applicableCategoryIds` và `specialDaysOfMonth`. Voucher xóa mềm bằng cách chuyển sang không hoạt động để giữ lịch sử đơn hàng.

`POST /api/vouchers/apply` nhận `{ "code": "...", "branchId": "..." }`; backend lấy giỏ hàng và giá hiện tại, không nhận subtotal từ client. API chỉ báo giá, không giữ lượt sử dụng. Khi tạo đơn, voucher được kiểm tra lại và lượt dùng được ghi nhận trong cùng transaction. Giá sale áp dụng cho toàn bộ số lượng; voucher tính tiếp trên giá sau sale. Với voucher giới hạn danh mục, khoản giảm chỉ tính trên sản phẩm thuộc danh mục được chọn và các danh mục con; ngưỡng đơn tối thiểu vẫn so với tổng tiền hàng sau sale, trước voucher. Ngày đặc biệt được tính theo múi giờ Việt Nam.

Danh sách chung, theo danh mục, khuyến mãi và hàng ngày hỗ trợ:

| Query param | Ý nghĩa |
|---|---|
| `keyword` | Tìm theo tên hoặc SKU |
| `categoryId` | Lọc theo danh mục trên `/api/products` |
| `includeDescendants` | Mặc định `true`; lấy cả danh mục con |
| `brandIds` | Một hoặc nhiều UUID thương hiệu; lặp tham số để truyền nhiều giá trị |
| `minPrice`, `maxPrice` | Lọc theo `displayPrice` sau khi áp dụng giảm giá |
| `inStock` | `true` chỉ còn hàng, `false` hết hàng |
| `sort` | `name_asc`, `name_desc`, `price_asc`, `price_desc`, `discount_desc`, `newest` |

Sản phẩm được đưa vào nhóm hàng ngày bằng cột `products.is_daily_fresh`. Mặc định cờ là `false`; với dữ liệu mẫu, thịt bò, thịt heo, cá lóc và sữa được đánh dấu. Hiện chưa có API quản trị để bật/tắt cờ này; có thể cập nhật trực tiếp trong DB:

```sql
UPDATE products SET is_daily_fresh = true WHERE sku IN ('SP006', 'SP007', 'SP008');
UPDATE products SET is_daily_fresh = false WHERE sku = 'SP008';
```

API hàng ngày cũng nhận các bộ lọc và sắp xếp ở bảng trên, ví dụ:

```http
GET /api/products/daily-fresh?branchId={branchId}&inStock=true&sort=price_asc&page=0&size=20
```

Ví dụ lọc sản phẩm theo danh mục, thương hiệu, tồn kho và khoảng giá:

```http
GET /api/products/category/{categoryId}?branchId={branchId}&includeDescendants=true&brandIds={brandId}&minPrice=10000&maxPrice=50000&inStock=true&sort=price_asc&page=0&size=20
```

API giảm giá dành cho admin:

```http
GET    /api/admin/product-discounts
GET    /api/admin/product-discounts/{id}
POST   /api/admin/product-discounts
PUT    /api/admin/product-discounts/{id}
DELETE /api/admin/product-discounts/{id}
```

Request tạo/cập nhật gồm `productId`, `discountPercent`, `startsAt`, `endsAt`. Thời gian dùng định dạng ISO-8601 có timezone. Không cho phép mức giảm của cùng sản phẩm chồng lấn thời gian.

### API tồn kho chi nhánh

Admin chỉ có quyền xem tồn kho; tài khoản role `BRANCH` chỉ xem và cập nhật tồn kho của chi nhánh được gắn với tài khoản đó:

```http
GET /api/admin/branches/inventory/summary
GET /api/admin/branches/{branchId}/inventory/by-categories
GET /api/admin/branches/{branchId}/inventory/summary
GET /api/admin/branches/{branchId}/inventory/categories
GET /api/admin/inventory/by-branch?branchId={branchId}&categoryId={categoryId}&keyword={keyword}&page=0&size=20
GET /api/branches/{branchId}/inventory/by-categories
GET /api/branches/{branchId}/inventory/summary
GET /api/branches/{branchId}/inventory/categories
GET /api/branch/inventory/summary?branchId={branchId}
GET /api/branch/inventory/categories?branchId={branchId}
GET /api/branches/{branchId}/inventory/{productId}/lots
POST /api/branches/{branchId}/inventory/lots
PATCH /api/branches/{branchId}/inventory/lots/{lotId}
PUT /api/branches/{branchId}/inventory
```

API `PUT` nhận `productId`, `stockQuantity` (số tồn mới, bắt buộc thấp hơn tồn hiện tại) và `reason` (`EXPIRED` hoặc `DAMAGED`). Không thể giảm tồn thấp hơn số lượng đang được giữ; các lần giảm được ghi vào lịch sử biến động cùng lý do. API không dùng để nhập/tăng tồn.

Summary chi nhánh trả số SKU, tồn thực tế, tồn khả dụng, tồn đang giữ, số SKU sắp hết (theo `min_stock_level`), hết hàng và SKU có lô hết hạn trong vòng 7 ngày. Báo cáo danh mục tổng hợp các chỉ số tồn kho theo danh mục cấp 1 và cấp 2. Admin tra cứu tồn kho theo chi nhánh có lọc danh mục, từ khóa tên/SKU và phân trang.

Các lô được nhập qua `POST /api/branches/{branchId}/inventory/lots` với `productId`, `quantity` và `expiryDate` (ISO `YYYY-MM-DD`, có thể bỏ trống với hàng không có hạn sử dụng); tồn tổng và lịch sử nhập kho được cập nhật đồng thời. API `GET .../{productId}/lots` trả các lô hiện có; `PATCH .../lots/{lotId}` gán/cập nhật ngày hết hạn của lô cũ chưa có ngày. Hàng xuất/giảm được trừ vào các lô theo hạn dùng gần nhất. Dữ liệu tồn cũ được migration V12 đưa vào lô không có ngày hết hạn; cần cập nhật hạn dùng của các lô này để chỉ số sắp hết hạn chính xác.

Khi tạo sản phẩm hoặc chi nhánh, hệ thống tạo liên kết tồn kho với số lượng ban đầu bằng 0. Migration V11 bổ sung các liên kết còn thiếu cho dữ liệu hiện có; V12 tạo lô không có ngày hết hạn cho tồn kho cũ.

### API đơn hàng của khách hàng

Khách hàng có thể xem lịch sử đơn hàng theo trang và xem chi tiết một đơn của chính mình:

```http
GET   /api/orders?page=0&size=20&group=ALL
GET   /api/orders/{orderId}
PATCH /api/orders/{orderId}/confirm-received
POST  /api/orders/quote
POST  /api/orders
```

Checkout chấp nhận `promotionCode` tùy chọn và chỉ nhận `receiveFrom`/`receiveTo` cho đơn `PICKUP`. Hai thời điểm phải cùng được truyền hoặc cùng bỏ trống; nếu có thì phải nằm trong tương lai và thời gian kết thúc sau thời gian bắt đầu. Đây là giờ khách dự kiến đến nhận, không phải slot được giữ chỗ; bỏ trống nghĩa là nhận sớm nhất. Đơn `DELIVERY` không nhận giờ giao do khách chọn.

Có thể gọi `POST /api/orders/quote` trước khi đặt để kiểm tra voucher và xem `subtotal`, `discountAmount`, `totalAmount`; API báo giá không giữ lượt voucher. Khi `POST /api/orders`, backend tính lại giá và kiểm tra/ghi nhận lượt voucher nguyên tử trong giao dịch tạo đơn, nên tổng tiền từ báo giá chỉ mang tính tham khảo. Mỗi đơn chỉ áp dụng một voucher. Voucher được hoàn lại lượt nếu chi nhánh hủy đơn trước khi giao/khách đến nhận.

Ví dụ request báo giá:

```json
{
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "promotionCode": "WELCOME10"
}
```

Ví dụ request tạo đơn tự lấy có voucher và khung giờ dự kiến:

```json
{
  "clientRequestId": "4c5b4df4-f213-4c5a-90fb-81ab8e55d813",
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "deliveryType": "PICKUP",
  "promotionCode": "WELCOME10",
  "receiveFrom": "2026-10-16T10:00:00+07:00",
  "receiveTo": "2026-10-16T11:00:00+07:00",
  "recipientName": "Nguyen Van A",
  "recipientPhone": "0987654321",
  "note": "Gọi trước khi đến"
}
```

Danh sách sắp xếp đơn mới nhất trước và trả về `content`, `page`, `size`, `totalElements`, `totalPages`. `group` không bắt buộc, mặc định là `ALL`; các giá trị hỗ trợ:

| `group` | Các trạng thái |
|---|---|
| `ALL` | Tất cả trạng thái |
| `PENDING` | Chờ xác nhận |
| `PROCESSING` | Đang chuẩn bị hàng |
| `WAITING` | `SHIPPING`, `READY_FOR_PICKUP`, `DELIVERY_FAILED` (đơn giao thất bại có thể được giao lại) |
| `COMPLETED` | `DELIVERED_PENDING_CONFIRMATION`, `COMPLETED` (hiển thị Đã giao / Đã nhận hàng) |
| `CANCELLED` | Đã hủy |
| `RETURNED` | Trả hàng |

Chi tiết trả về trạng thái, chi nhánh, thanh toán, tổng tiền, mặt hàng, `deliveredAt` và `completedAt`. Cả ba endpoint chỉ dùng role `CUSTOMER`; API chi tiết và xác nhận nhận hàng chỉ thao tác trên đơn của khách đang đăng nhập. Khách chỉ xác nhận được khi đơn ở trạng thái `DELIVERED_PENDING_CONFIRMATION`.

### Vòng đời đơn hàng

Nhân viên chi nhánh chỉ thao tác trên đơn thuộc chi nhánh được gán cho tài khoản của mình. Danh sách có thể lọc bằng `status` (giá trị `OrderStatus`) và phân trang:

```http
GET   /api/branches/{branchId}/orders?status=PENDING&page=0&size=20
GET   /api/branches/{branchId}/orders/{orderId}
PATCH /api/branches/{branchId}/orders/{orderId}/confirm
PATCH /api/branches/{branchId}/orders/{orderId}/cancel
PATCH /api/branches/{branchId}/orders/{orderId}/preparation-complete
PATCH /api/branches/{branchId}/orders/{orderId}/handover
PATCH /api/branches/{branchId}/orders/{orderId}/delivery-failed
PATCH /api/branches/{branchId}/orders/{orderId}/retry-delivery
```

Luồng giao tận nơi: `PENDING` → `PROCESSING` → `SHIPPING` → `DELIVERED_PENDING_CONFIRMATION` → `COMPLETED`. Luồng khách tự lấy: `PENDING` → `PROCESSING` → `READY_FOR_PICKUP` → `DELIVERED_PENDING_CONFIRMATION` → `COMPLETED`. Nhân viên xác nhận bàn giao bằng `handover` sau khi giao thành công hoặc khách đã nhận hàng tại chi nhánh; việc này ghi `deliveredAt`. Khách có thể xác nhận nhận hàng bằng endpoint riêng. Nếu khách không xác nhận, hệ thống tự chuyển sang `COMPLETED` sau 72 giờ từ `deliveredAt` (tác vụ kiểm tra chạy mỗi phút). Đơn giao thất bại được ghi lý do và có thể chuyển lại sang `SHIPPING`; trạng thái này chưa khởi động thời hạn tự hoàn tất.

Sản phẩm đã ngừng kinh doanh có thể được khôi phục riêng, không cần mở quyền sửa `status` trong request cập nhật thông tin sản phẩm:

```http
PATCH /api/admin/products/{productId}/restore
```

Endpoint này chuyển trạng thái sản phẩm về `ACTIVE`; endpoint `DELETE /api/admin/products/{productId}` vẫn chuyển sang `DISCONTINUED`.

Danh sách sản phẩm quản trị có thể lọc theo một danh mục cụ thể và phân trang:

```http
GET /api/admin/products/category/{categoryId}?page=0&size=20
```

### Quản lý tài khoản (admin)

Các endpoint dưới đây chỉ dành cho tài khoản `ADMIN`. API danh sách hỗ trợ lọc theo từ khóa (họ tên, số điện thoại, email), vai trò, trạng thái và phân trang:

```http
GET    /api/admin/users?keyword={keyword}&role={CUSTOMER|BRANCH|ADMIN}&status={ACTIVE|LOCKED}&page=0&size=20
GET    /api/admin/users/{userId}
POST   /api/admin/users
PUT    /api/admin/users/{userId}
DELETE /api/admin/users/{userId}
```

Tạo tài khoản yêu cầu `fullName`, `phone`, `password` và `role`; `email`, `birthDate`, `gender` là tùy chọn. Tài khoản `BRANCH` phải có `branchId` và mỗi chi nhánh chỉ được gán một tài khoản. Khi cập nhật, truyền đầy đủ `fullName`, `phone`, `role`, `status`; `password` không bắt buộc và nếu bỏ trống sẽ giữ nguyên. `DELETE` khóa tài khoản (`LOCKED`) thay vì xóa vật lý, đồng thời gỡ liên kết chi nhánh nếu có để giữ nguyên lịch sử đơn hàng. Không thể tự khóa/hạ quyền hoặc khóa tài khoản admin cuối cùng.

Ví dụ tạo tài khoản nhân viên chi nhánh:

```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0987654321",
  "email": "a@example.com",
  "password": "matkhau123",
  "role": "BRANCH",
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

```json
{
  "productId": "UUID-san-pham",
  "discountPercent": 20.00,
  "startsAt": "2026-10-01T00:00:00+07:00",
  "endsAt": "2026-10-08T00:00:00+07:00"
}
```

Các nhóm API còn lại (đặt hàng, thanh toán, AI, chat, dự báo tồn kho...) sẽ được bổ sung khi triển khai các giai đoạn tiếp theo.

## Cấu trúc thư mục liên quan tới DB & hạ tầng

```
UrbanMart/
├── docker-compose.yml
├── db/
│   ├── migrations/
│   │   ├── V1__extensions_and_enums.sql
│   │   ├── V2__users_and_addresses.sql
│   │   ├── V3__branches_catalog_inventory.sql
│   │   ├── V4__wishlist_and_cart.sql
│   │   ├── V5__cart_uniqueness_constraints.sql
│   │   ├── V6__product_discounts.sql
│   │   ├── V7__daily_fresh_products.sql
│   │   ├── V8__product_sku_sequence.sql
│   │   ├── V9__orders_payments_and_promotions.sql
│   │   ├── V10__inventory_reservations_and_movements.sql
│   │   ├── V11__backfill_branch_inventory_products.sql
│   │   ├── V12__branch_inventory_expiry_lots.sql
│   │   ├── V13__order_handover_confirmation.sql
│   │   └── V14__voucher_details_and_conditions.sql
│   └── seed/
│   │   └── seed-sample-data.sql
└── src/main/
    ├── java/com/haui/UrbanMart/
    │   ├── entity/       (JPA entities and enums)
    │   ├── repository/   (Spring Data JPA)
    │   ├── security/     (JWT, UserDetails, filter, entry point/access denied)
    │   ├── config/        (SecurityConfig)
    │   ├── service/       (business logic, vd AuthService, UserService, OtpService)
    │   ├── controller/    (REST endpoint)
    │   ├── dto/request/   (input tu client)
    │   ├── dto/response/  (output tra ve, vd ApiResponse, UserResponse)
    │   └── exception/     (custom exception + GlobalExceptionHandler)
    └── resources/
        ├── application-example.properties   (mẫu, có trong Git)
        └── application.properties           (thật, KHÔNG đưa lên Git)
```

## Xử lý sự cố thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `password authentication failed` khi connect DBeaver/app | Có PostgreSQL khác chiếm cổng 5432, hoặc password đã bị đổi trên container cũ | Kiểm tra `Get-NetTCPConnection -LocalPort 5432`; hoặc chạy `ALTER USER urbanmart_user WITH PASSWORD 'urbanmart_password';` trong container |
| `relation "xxx" does not exist` | Chưa chạy đủ/đúng thứ tự migration | Chạy migration còn thiếu ở Bước 3; lỗi `product_sku_seq` cần chạy V8 |
| `column sale_price does not exist` | Code hoặc schema chưa đồng bộ sau khi chuyển giảm giá sang bảng riêng | Chạy V6 nếu database chưa áp dụng và dùng code mới |
| `'<' operator is reserved for future use` (PowerShell) | Dùng `<` để redirect như Linux, PowerShell không hỗ trợ | Dùng `Get-Content file.sql \| docker exec -i ...` thay vì `< file.sql` |
| `Could not resolve placeholder 'jwt.secret'` | Thiếu `jwt.secret`/`jwt.expiration-ms` trong `application.properties` | Thêm đúng 2 dòng như Bước 5 |
| `GenerationTime cannot be resolved` (entity) | Bản Hibernate mới đã bỏ `org.hibernate.annotations.GenerationTime` | Dùng `@Generated(event = EventType.INSERT)` với `org.hibernate.generator.EventType` thay vì `GenerationTime.INSERT` |
| `NullPointerException` khi service gọi repository dù đã `@RequiredArgsConstructor` | Quên khai báo `final` cho field — Lombok chỉ inject field `final` | Thêm `final` vào mọi field muốn Spring tự inject qua constructor |
| Muốn xóa sạch làm lại từ đầu | — | `docker compose down -v` (xóa cả volume) rồi làm lại từ Bước 2 |
