# API tuần 9: Món ăn, đánh giá và thực phẩm không tương thích

Tài liệu này mô tả các API đã được triển khai trong backend. Các ID UUID trong ví dụ chỉ minh họa định dạng; khi gọi API cần thay bằng ID có thật trong database.

## Quy ước chung

- API dùng tiền tố `/api/v1` theo từng endpoint bên dưới.
- ID của món ăn, sản phẩm, chi nhánh, đơn hàng và quy tắc là UUID.
- Response thành công dùng cấu trúc `{"status":"success","message":"...","data":...}`.
- API danh sách dùng phân trang `page` bắt đầu từ `0`; `size` tối đa `100`.
- `categoryMonAn` nhận mã enum, ví dụ `VEGETABLE`, không nhận nhãn giao diện như `Món rau, củ`.
- API chatbot AI và gợi ý món ăn theo danh sách sản phẩm chưa được triển khai trong phạm vi tài liệu/code hiện tại.

## 1. Món ăn

### Phân quyền và endpoint

| Tác nhân | Method | Endpoint | Mô tả |
|---|---|---|---|
| Admin | `POST` | `/api/v1/admin/recipes` | Tạo món |
| Admin | `GET` | `/api/v1/admin/recipes` | Danh sách món, có lọc |
| Admin | `GET` | `/api/v1/admin/recipes/{id}` | Chi tiết món |
| Admin | `PUT` | `/api/v1/admin/recipes/{id}` | Cập nhật món và toàn bộ nguyên liệu |
| Admin | `DELETE` | `/api/v1/admin/recipes/{id}` | Ẩn món (xóa mềm) |
| Khách | `GET` | `/api/v1/recipes` | Danh sách món đang hoạt động |
| Khách | `GET` | `/api/v1/recipes/{id}/ingredients?branchId={branchId}` | Nguyên liệu, giá và tồn kho tại chi nhánh |

API admin yêu cầu vai trò `ADMIN`. Các API `GET /api/v1/recipes/**` công khai. API lấy nguyên liệu phải có `branchId` vì giá và tồn kho được truy vấn theo chi nhánh.

### Tạo/cập nhật món

`POST /api/v1/admin/recipes` và `PUT /api/v1/admin/recipes/{id}` dùng cùng request. `slug` có thể bỏ trống để backend tự sinh từ tên món. `ingredients` phải có ít nhất một phần tử; `quantity` từ 1 đến 1000; một sản phẩm không được lặp trong cùng món.

```json
{
  "name": "Củ su hào xào tàu hũ",
  "slug": "cu-su-hao-xao-tau-hu",
  "description": "Món xào thanh tịnh cho bữa cơm gia đình.",
  "imageUrl": "https://cdn.urbanmart.vn/recipes/su-hao-tau-hu.jpg",
  "categoryMonAn": "VEGETABLE",
  "isActive": true,
  "ingredients": [
    {
      "productId": "00000000-0000-0000-0000-000000000101",
      "quantity": 1,
      "isRequired": true,
      "group": "MAIN",
      "note": "Nguyên liệu chính"
    },
    {
      "productId": "00000000-0000-0000-0000-000000000102",
      "quantity": 1,
      "isRequired": false,
      "group": "SPICE",
      "note": "Gia vị"
    }
  ]
}
```

Giá trị `categoryMonAn`: `VEGETABLE`, `MEAT`, `SEAFOOD`, `SOUP`, `HOTPOT`, `VEGETARIAN`, `DESSERT`, `OTHER`. Giá trị `group`: `MAIN`, `SPICE`, `SIDE`. Khi bỏ `group`, backend mặc định `MAIN` nếu nguyên liệu bắt buộc và `SPICE` nếu không bắt buộc. `isActive` mặc định `true`.

Tạo mới trả HTTP `201`; cập nhật trả HTTP `200`. Dữ liệu `data` là `RecipeAdminResponse`:

```json
{
  "status": "success",
  "message": "Tạo món ăn thành công",
  "data": {
    "id": "00000000-0000-0000-0000-000000000201",
    "name": "Củ su hào xào tàu hũ",
    "slug": "cu-su-hao-xao-tau-hu",
    "description": "Món xào thanh tịnh cho bữa cơm gia đình.",
    "imageUrl": "https://cdn.urbanmart.vn/recipes/su-hao-tau-hu.jpg",
    "categoryMonAn": "VEGETABLE",
    "categoryLabel": "Món rau, củ",
    "isActive": true,
    "createdAt": "2026-11-01T09:00:00Z",
    "updatedAt": "2026-11-01T09:00:00Z",
    "ingredients": [
      {
        "id": "00000000-0000-0000-0000-000000000301",
        "productId": "00000000-0000-0000-0000-000000000101",
        "productName": "Su hào",
        "sku": "SP000001",
        "imageUrl": "/images/su-hao.jpg",
        "unit": "Kg",
        "price": 29900,
        "productStatus": "ACTIVE",
        "quantity": 1,
        "isRequired": true,
        "group": "MAIN",
        "note": "Nguyên liệu chính"
      }
    ]
  }
}
```

`price` trong response admin là giá gốc của sản phẩm (`base_price`), không phải giá khuyến mãi tại một chi nhánh. Danh sách admin nhận query tùy chọn `keyword`, `categoryMonAn`, `isActive`, `page`, `size`; response phân trang có `content`, `page`, `size`, `totalElements`, `totalPages`. Xóa món đặt `isActive=false`, không xóa dữ liệu nguyên liệu.

### Danh sách món công khai

Ví dụ: `GET /api/v1/recipes?categoryMonAn=VEGETABLE&page=0&size=20&keyword=su%20hao`

```json
{
  "status": "success",
  "message": "Lấy danh sách món ăn thành công",
  "data": {
    "content": [
      {
        "id": "00000000-0000-0000-0000-000000000201",
        "name": "Củ su hào xào tàu hũ",
        "slug": "cu-su-hao-xao-tau-hu",
        "imageUrl": "https://cdn.urbanmart.vn/recipes/su-hao-tau-hu.jpg",
        "categoryMonAn": "VEGETABLE",
        "categoryLabel": "Món rau, củ",
        "totalIngredients": 2
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### Lấy nguyên liệu theo chi nhánh

Ví dụ: `GET /api/v1/recipes/00000000-0000-0000-0000-000000000201/ingredients?branchId=00000000-0000-0000-0000-000000000401`

```json
{
  "status": "success",
  "message": "Lấy danh sách nguyên liệu thành công",
  "data": {
    "recipeId": "00000000-0000-0000-0000-000000000201",
    "recipeName": "Củ su hào xào tàu hũ",
    "branchId": "00000000-0000-0000-0000-000000000401",
    "groups": [
      {
        "group": "MAIN",
        "groupName": "Nguyên liệu chính",
        "items": [
          {
            "productId": "00000000-0000-0000-0000-000000000101",
            "name": "Su hào",
            "imageUrl": "/images/su-hao.jpg",
            "price": 29900,
            "originalPrice": 29900,
            "discountPercent": 0,
            "unit": "Kg",
            "quantity": 1,
            "isRequired": true,
            "note": "Nguyên liệu chính",
            "inStock": true,
            "availableQuantity": 12,
            "hasSimilarProducts": false,
            "similarProducts": []
          }
        ]
      }
    ]
  }
}
```

Chỉ sản phẩm đang `ACTIVE` được đưa vào danh sách. `price` là giá hiện tại sau khuyến mãi tại chi nhánh; `originalPrice` là giá gốc. Sản phẩm thay thế được tìm trong cùng danh mục, đang còn hàng tại chi nhánh, tối đa 3 sản phẩm; `similarProducts` chỉ được trả khi nguyên liệu chính hết hàng. Không có API thêm nguyên liệu vào giỏ hàng hàng loạt trong phần đã triển khai.

## 2. Đánh giá đơn hàng và sản phẩm

### Khách hàng gửi đánh giá đơn hàng

`POST /api/v1/customer/orders/{orderId}/reviews` — yêu cầu vai trò `CUSTOMER`, chỉ chủ đơn hàng có trạng thái `COMPLETED` được đánh giá. Mỗi đơn chỉ có một đánh giá đơn hàng. Với đơn `DELIVERY`, `deliveryRating` là bắt buộc; với đơn `PICKUP`, trường này phải là `null` hoặc bỏ qua.

```json
{
  "deliveryRating": 5,
  "serviceRating": 4,
  "comment": "Giao hàng nhanh, đóng gói cẩn thận.",
  "images": ["https://cdn.urbanmart.vn/reviews/order-123-1.jpg"]
}
```

Các điểm đánh giá từ 1 đến 5; comment tối đa 2000 ký tự; tối đa 5 URL ảnh, mỗi URL tối đa 500 ký tự.

```json
{
  "status": "success",
  "message": "Đánh giá đơn hàng thành công",
  "data": {
    "reviewId": "00000000-0000-0000-0000-000000000501",
    "orderId": "00000000-0000-0000-0000-000000000601",
    "orderCode": "UM110AEA09A2",
    "productId": null,
    "isVerifiedPurchase": null,
    "createdAt": "2026-11-01T10:30:00Z"
  }
}
```

### Khách hàng gửi đánh giá sản phẩm

`POST /api/v1/customer/products/{productId}/reviews` — yêu cầu vai trò `CUSTOMER`. Đơn hàng phải thuộc khách hiện tại, ở trạng thái `COMPLETED`, và có chứa sản phẩm đó. Mỗi sản phẩm trong mỗi đơn chỉ được đánh giá một lần.

```json
{
  "orderId": "00000000-0000-0000-0000-000000000601",
  "rating": 5,
  "comment": "Sản phẩm tươi, chất lượng tốt.",
  "images": ["https://cdn.urbanmart.vn/reviews/product-456-1.jpg"]
}
```

Response `201 Created` có cùng cấu trúc `ReviewCreatedResponse`; `productId` là UUID sản phẩm và `isVerifiedPurchase` là `true`.

### Xem đánh giá công khai của sản phẩm

`GET /api/v1/products/{productId}/reviews?page=0&size=10&rating=5` là API công khai. `rating` tùy chọn từ 1 đến 5. Thống kê tổng quan và `ratingSpreads` tính trên tất cả đánh giá sản phẩm, không bị giới hạn bởi bộ lọc `rating`.

```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "00000000-0000-0000-0000-000000000501",
        "customerName": "Nguyen Khanh Duy",
        "customerAvatar": null,
        "rating": 5,
        "comment": "Sản phẩm tươi, chất lượng tốt.",
        "images": ["https://cdn.urbanmart.vn/reviews/product-456-1.jpg"],
        "isVerifiedPurchase": true,
        "createdAt": "2026-11-01T10:32:00Z",
        "branchName": "UrbanMart Cầu Giấy"
      }
    ],
    "averageRating": 4.8,
    "totalReviews": 25,
    "ratingSpreads": {"5": 20, "4": 3, "3": 2, "2": 0, "1": 0},
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

`customerAvatar` hiện là `null` vì hồ sơ user chưa có trường ảnh đại diện.

### Chi nhánh xem đánh giá

`GET /api/v1/branch/reviews?type=ALL&rating=5&startDate=2026-11-01T00:00:00Z&endDate=2026-11-02T00:00:00Z&page=0&size=10` — yêu cầu vai trò `BRANCH`. Chi nhánh được xác định từ tài khoản đăng nhập, không nhận `branchId` từ client. `type` là `ORDER`, `PRODUCT` hoặc `ALL`, mặc định `ALL`. `rating` lọc theo `serviceRating` của đánh giá đơn hoặc `rating` của đánh giá sản phẩm. `endDate` là mốc kết thúc không bao gồm (`created_at < endDate`).

Response `data` gồm:

- `content`: các trường `id`, `reviewType`, `orderId`, `orderCode`, `customerName`, `phone` (đã che), `productId`, `productName`, `rating`, `deliveryRating`, `serviceRating`, `comment`, `images`, `createdAt`.
- `page`, `size`, `totalElements`, `totalPages`.
- `branchSummary`: `averageOrderRating`, `averageProductRating`, `totalOrderReviews`, `totalProductReviews`. Thống kê này tính trên toàn bộ đánh giá của chi nhánh, không áp dụng bộ lọc ngày/số sao; trường của loại bị loại trừ sẽ là `null` và số lượng là `0`.

### Admin xem đánh giá

`GET /api/v1/admin/reviews?branchId={branchId}&type=PRODUCT&rating=5&keyword=su%20hao&startDate=2026-11-01T00:00:00Z&endDate=2026-11-02T00:00:00Z&page=0&size=10` — yêu cầu vai trò `ADMIN`. Có thể bỏ `branchId` để xem tất cả chi nhánh. Bộ lọc `keyword` tìm theo mã đơn, tên khách hàng và (với đánh giá sản phẩm) tên sản phẩm.

`data` là đối tượng phân trang gồm `content`, `page`, `size`, `totalElements`, `totalPages`. Mỗi phần tử có `id`, `branchId`, `branchName`, `reviewType`, `orderId`, `orderCode`, `targetName`, `productId`, `customerId`, `customerName`, `rating`, `deliveryRating`, `serviceRating`, `comment`, `images`, `createdAt`.

## 3. Tag và quy tắc thực phẩm không tương thích

Thông tin tương thích là cảnh báo tham khảo, không phải chẩn đoán hoặc khuyến cáo y tế. Backend không tạo sẵn dữ liệu tag/quy tắc; admin phải quản lý dữ liệu có căn cứ. Các mức `LOW`, `MEDIUM`, `HIGH`, `DANGEROUS` là mức được cấu hình cho quy tắc, không tự xác nhận tính đúng đắn y khoa.

### Admin quản lý tag

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/v1/admin/food-tags` | Danh sách tag |
| `POST` | `/api/v1/admin/food-tags` | Tạo tag |
| `PUT` | `/api/v1/admin/products/{productId}/food-tags` | Thay toàn bộ tag của sản phẩm |

Các endpoint trên yêu cầu vai trò `ADMIN`. Mã tag được chuẩn hóa chữ hoa; chỉ gồm chữ Latin, số và `_`.

Tạo tag:

```json
{"code": "HAI_SAN", "name": "Hải sản"}
```

Gán tag sản phẩm:

```json
{"tagCodes": ["HAI_SAN", "TOM"]}
```

Gửi `tagCodes: []` để gỡ toàn bộ tag. Response gán tag có `productId`, `productName`, `tags` (mỗi tag gồm `code`, `name`).

### Admin CRUD quy tắc

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/admin/food-incompatibilities` | Tạo quy tắc |
| `GET` | `/api/v1/admin/food-incompatibilities` | Danh sách, phân trang/lọc |
| `GET` | `/api/v1/admin/food-incompatibilities/{id}` | Chi tiết |
| `PUT` | `/api/v1/admin/food-incompatibilities/{id}` | Cập nhật |
| `DELETE` | `/api/v1/admin/food-incompatibilities/{id}` | Xóa |

Tất cả yêu cầu vai trò `ADMIN`. Quy tắc không phụ thuộc thứ tự hai tag; backend sắp xếp cặp tag và không cho phép trùng cặp.

Request tạo/cập nhật:

```json
{
  "tagA": "HAI_SAN",
  "tagB": "VITAMIN_C",
  "severity": "MEDIUM",
  "title": "Thông tin cần lưu ý khi kết hợp thực phẩm",
  "effect": "Nội dung cảnh báo đã được kiểm chứng và cần nêu rõ nguồn ở nội dung quản trị.",
  "recommendation": "Nếu có dị ứng hoặc chế độ ăn đặc biệt, hãy kiểm tra thành phần và hỏi chuyên gia.",
  "isActive": true
}
```

`severity` nhận `LOW`, `MEDIUM`, `HIGH` hoặc `DANGEROUS`; `isActive` mặc định `true`. `tagA` và `tagB` phải khác nhau và đã tồn tại. `GET` danh sách nhận `page`, `size`, `search`, `severity`, `isActive`; `search` tìm trong mã tag và tiêu đề. Response quy tắc có `id`, `tagA`, `tagB`, `severity`, `title`, `effect`, `recommendation`, `isActive`, `createdAt`, `updatedAt`. Xóa trả `data` là UUID quy tắc đã xóa.

### Kiểm tra danh sách sản phẩm

`POST /api/v1/customer/cart/check-incompatibilities` yêu cầu vai trò `CUSTOMER`.

```json
{
  "productIds": [
    "00000000-0000-0000-0000-000000000101",
    "00000000-0000-0000-0000-000000000102"
  ]
}
```

Request phải có từ 1 đến 200 UUID sản phẩm. Backend xác thực sản phẩm tồn tại và đang hoạt động, rồi đối chiếu tag sản phẩm với các quy tắc đang hoạt động.

```json
{
  "status": "success",
  "message": "Kiểm tra kết hợp thực phẩm thành công",
  "data": {
    "hasConflict": true,
    "warnings": [
      {
        "id": "00000000-0000-0000-0000-000000000701",
        "severity": "MEDIUM",
        "title": "Thông tin cần lưu ý khi kết hợp thực phẩm",
        "effect": "Nội dung cảnh báo đã được kiểm chứng.",
        "recommendation": "Kiểm tra thành phần nếu có dị ứng hoặc chế độ ăn đặc biệt.",
        "conflictingProducts": [
          {
            "id": "00000000-0000-0000-0000-000000000101",
            "name": "Sản phẩm A",
            "imageUrl": "/images/product-a.jpg",
            "matchedTag": "HAI_SAN"
          },
          {
            "id": "00000000-0000-0000-0000-000000000102",
            "name": "Sản phẩm B",
            "imageUrl": "/images/product-b.jpg",
            "matchedTag": "VITAMIN_C"
          }
        ]
      }
    ]
  }
}
```

API kiểm tra các `productIds` được gửi lên; hiện tại backend chưa tự đọc giỏ hàng của khách để tạo danh sách này. Vì vậy client cần gửi ID sản phẩm trong giỏ. Sản phẩm bị ngừng kinh doanh hoặc ID không tồn tại sẽ trả lỗi thay vì bị bỏ qua.

### Xem quy tắc liên quan đến một sản phẩm

`GET /api/v1/customer/products/{productId}/incompatibilities` là API công khai.

```json
{
  "status": "success",
  "message": "Lấy thông tin thực phẩm có thể không tương thích thành công",
  "data": {
    "productId": "00000000-0000-0000-0000-000000000101",
    "productName": "Sản phẩm A",
    "tags": ["HAI_SAN", "TOM"],
    "incompatibleRules": [
      {
        "id": "00000000-0000-0000-0000-000000000701",
        "matchedTag": "HAI_SAN",
        "incompatibleTag": "VITAMIN_C",
        "incompatibleTagName": "Thực phẩm giàu vitamin C",
        "severity": "MEDIUM",
        "title": "Thông tin cần lưu ý khi kết hợp thực phẩm",
        "effect": "Nội dung cảnh báo đã được kiểm chứng.",
        "recommendation": "Kiểm tra thành phần nếu có dị ứng hoặc chế độ ăn đặc biệt."
      }
    ]
  }
}
```
