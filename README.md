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
for f in V1__extensions_and_enums.sql V2__users_and_addresses.sql V3__branches_catalog_inventory.sql V4__wishlist_and_cart.sql V5__cart_uniqueness_constraints.sql V6__product_discounts.sql V7__daily_fresh_products.sql V8__product_sku_sequence.sql; do
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1 < "$f" || break
done
```

**Trên Windows (PowerShell)** — `<` không dùng được như Linux, phải dùng `Get-Content`:
```powershell
cd db\migrations
$files = "V1__extensions_and_enums.sql","V2__users_and_addresses.sql","V3__branches_catalog_inventory.sql","V4__wishlist_and_cart.sql","V5__cart_uniqueness_constraints.sql","V6__product_discounts.sql","V7__daily_fresh_products.sql","V8__product_sku_sequence.sql"
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

Các bảng chính gồm: `users`, `addresses`, `branches`, `categories`, `brands`, `products`, `branch_inventory`, `wishlists`, `carts`, `cart_items`, `product_discounts`.

Migration `V5` thêm ràng buộc để mỗi user chỉ có một giỏ hàng trên mỗi chi nhánh và mỗi sản phẩm chỉ xuất hiện một lần trong cùng giỏ hàng. Nếu database đã có dữ liệu trùng, cần xử lý các bản ghi đó trước khi chạy migration.

Migration `V6` tạo bảng `product_discounts`, giới hạn phần trăm và thời gian áp dụng, đồng thời không cho phép các đợt giảm giá của cùng sản phẩm chồng lấn. Migration này cũng bỏ các cột giảm giá cũ khỏi `products`; giá gốc vẫn nằm ở `products.base_price`.

Migration `V7` thêm cột `products.is_daily_fresh`, mặc định `false`, để quản lý nhóm sản phẩm hàng ngày.

Migration `V8` tạo sequence `product_sku_seq` dùng để sinh SKU tự động khi tạo sản phẩm và khởi tạo sequence tiếp nối mã SKU `SP...` hiện có.

Nếu database đã chạy V1–V6, chạy V7 và V8 trước khi dùng code mới:

```powershell
Get-Content -Raw .\db\migrations\V7__daily_fresh_products.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
Get-Content -Raw .\db\migrations\V8__product_sku_sequence.sql |
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart -v ON_ERROR_STOP=1 -1
```

## Bước 3.1 — Nạp dữ liệu mẫu (không bắt buộc)

Chạy sau khi đã áp dụng migration V1–V8. Seed mẫu tạo hai tài khoản `BRANCH` (gắn lần lượt với chi nhánh Hà Đông và Cầu Giấy), chi nhánh, danh mục, thương hiệu, sản phẩm, tồn kho, nhóm sản phẩm hàng ngày và các đợt giảm giá đang hiệu lực/sắp diễn ra/đã kết thúc. Chỉ chạy một lần trên database trống vì các mã SKU và tên chi nhánh là duy nhất.

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

## API hiện có và tuần 4

| Nhóm API | Endpoint / chức năng | Trạng thái |
|---|---|---|
| Đăng ký + xác thực OTP | `/api/auth/register`, `/verify-otp`, `/resend-otp` | ✅ Đã code |
| Đăng nhập, quên/đổi mật khẩu | `/api/auth/login`, `/forgot-password`, `/reset-password`, `/change-password` | ✅ Đã code |
| Sửa thông tin cá nhân | `/api/users/me` | ✅ Đã code |
| CRUD địa chỉ | `/api/addresses` | ✅ Đã code |
| Chi nhánh gần khách | `/api/branches/nearby` | ✅ Đã code |
| Danh mục và thương hiệu | `/api/categories`, `/api/brands` | ✅ Đã code |
| Sản phẩm theo chi nhánh | `/api/products`, `/api/products/{productId}` | ✅ Có phân trang, tìm kiếm, lọc và sắp xếp |
| Sản phẩm theo danh mục | `/api/products/category/{categoryId}` | ✅ Có thể lấy sản phẩm thuộc danh mục con |
| Sản phẩm đang giảm giá | `/api/products/promotions` | ✅ Chỉ trả các đợt giảm đang hiệu lực |
| Sản phẩm hàng ngày | `/api/products/daily-fresh` | ✅ Lọc theo cờ `is_daily_fresh` |
| CRUD đợt giảm giá (admin) | `/api/admin/product-discounts` | ✅ Chỉ role `ADMIN` |
| Sản phẩm bán chạy | Chưa triển khai | ⏳ Chưa làm theo phạm vi hiện tại |
| Yêu thích sản phẩm | `/api/wishlists` | ✅ Đã code |
| Giỏ hàng | `/api/cart` | ✅ Đã code |

### API danh sách sản phẩm tuần 4

Các API danh sách sản phẩm cần `branchId`; phân trang dùng `page` bắt đầu từ `0` và `size` từ `1` đến `100`. Các endpoint lấy sản phẩm trả `basePrice` (giá gốc), `displayPrice` (giá khách trả), thông tin giảm giá nếu đang có đợt hiệu lực và `dailyFresh` cho biết sản phẩm có thuộc nhóm hàng ngày không.

```http
GET /api/products?branchId={branchId}&page=0&size=20
GET /api/products/category/{categoryId}?branchId={branchId}&includeDescendants=true&page=0&size=20
GET /api/products/promotions?branchId={branchId}&page=0&size=20
GET /api/products/daily-fresh?branchId={branchId}&page=0&size=20
```

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

Sản phẩm đã ngừng kinh doanh có thể được khôi phục riêng, không cần mở quyền sửa `status` trong request cập nhật thông tin sản phẩm:

```http
PATCH /api/admin/products/{productId}/restore
```

Endpoint này chuyển trạng thái sản phẩm về `ACTIVE`; endpoint `DELETE /api/admin/products/{productId}` vẫn chuyển sang `DISCONTINUED`.

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
│   │   └── V8__product_sku_sequence.sql
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
