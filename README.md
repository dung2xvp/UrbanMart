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
for f in V1__extensions_and_enums.sql V2__users_and_addresses.sql V3__branches_catalog_inventory.sql V4__wishlist_and_cart.sql; do
  docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart < "$f" || break
done
```

**Trên Windows (PowerShell)** — `<` không dùng được như Linux, phải dùng `Get-Content`:
```powershell
cd db\migrations
$files = "V1__extensions_and_enums.sql","V2__users_and_addresses.sql","V3__branches_catalog_inventory.sql","V4__wishlist_and_cart.sql"
foreach ($f in $files) {
    Get-Content $f | docker exec -i urbanmart-db psql -U urbanmart_user -d urbanmart
    if ($LASTEXITCODE -ne 0) { break }
}
```

Kiểm tra đã tạo đủ 10 bảng:

```bash
docker exec -it urbanmart-db psql -U urbanmart_user -d urbanmart -c "\dt"
```

Phải thấy: `users`, `addresses`, `branches`, `categories`, `brands`, `products`, `branch_inventory`, `wishlists`, `carts`, `cart_items`.

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

## Danh sách API đợt 1 (đã có bảng hỗ trợ)

Trạng thái hiện tại:

| Nhóm API | Trạng thái |
|---|---|
| Đăng ký + xác thực OTP (`/api/auth/register`, `/verify-otp`, `/resend-otp`) | ✅ Đã code xong |
| Đăng nhập, quên/đổi mật khẩu (`/api/auth/login`, `/forgot-password`, `/reset-password`, `/change-password`) | ✅ Đã code xong |
| Sửa thông tin cá nhân (`/api/users/me`) | ✅ Đã code xong |
| CRUD địa chỉ (`/api/addresses`) | ✅ Đã code xong |
| Chi nhánh gần khách (`/api/branches/nearby`) | ✅ Đã code xong |
| Danh mục (`/api/categories`) | ✅ Đã code xong |
| Thương hiệu (`/api/brands`) | ✅ Đã code xong |
| Sản phẩm theo chi nhánh (phân trang, tìm kiếm) | ⏳ Chưa làm |
| Yêu thích sản phẩm | ⏳ Chưa làm |
| Giỏ hàng (thêm, xem, đổi số lượng) | ⏳ Chưa làm |

Các nhóm API còn lại (đặt hàng, thanh toán, AI, chat, dự báo tồn kho...) sẽ có migration `V5` trở đi khi triển khai tới.

## Cấu trúc thư mục liên quan tới DB & hạ tầng

```
UrbanMart/
├── docker-compose.yml
├── db/
│   └── migrations/
│       ├── V1__extensions_and_enums.sql
│       ├── V2__users_and_addresses.sql
│       ├── V3__branches_catalog_inventory.sql
│       └── V4__wishlist_and_cart.sql
└── src/main/
    ├── java/com/haui/UrbanMart/
    │   ├── entity/       (10 entity JPA + 4 enum)
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
| `relation "xxx" does not exist` | Chưa chạy đủ/đúng thứ tự migration | Chạy lại Bước 3, kiểm tra bằng `\dt` |
| `'<' operator is reserved for future use` (PowerShell) | Dùng `<` để redirect như Linux, PowerShell không hỗ trợ | Dùng `Get-Content file.sql \| docker exec -i ...` thay vì `< file.sql` |
| `Could not resolve placeholder 'jwt.secret'` | Thiếu `jwt.secret`/`jwt.expiration-ms` trong `application.properties` | Thêm đúng 2 dòng như Bước 5 |
| `GenerationTime cannot be resolved` (entity) | Bản Hibernate mới đã bỏ `org.hibernate.annotations.GenerationTime` | Dùng `@Generated(event = EventType.INSERT)` với `org.hibernate.generator.EventType` thay vì `GenerationTime.INSERT` |
| `NullPointerException` khi service gọi repository dù đã `@RequiredArgsConstructor` | Quên khai báo `final` cho field — Lombok chỉ inject field `final` | Thêm `final` vào mọi field muốn Spring tự inject qua constructor |
| Muốn xóa sạch làm lại từ đầu | — | `docker compose down -v` (xóa cả volume) rồi làm lại từ Bước 2 |
