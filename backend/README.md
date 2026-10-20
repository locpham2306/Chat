# Backend

Thư mục dành cho toàn bộ source, build, cấu hình và test của backend Spring Boot.
Đã dựng entity/schema cho toàn bộ phạm vi chat; chưa có API auth hoặc API chat.
Xem [schema và quan hệ DB](docs/06-database-schema.md).
Java 21, Spring Boot 3.5.16, Maven Wrapper 3.9.5; root package `com.chat`.

Phát triển trên nhánh `backend`, giữ toàn bộ code trong thư mục `backend/`.
Khi hoàn thiện và kiểm tra xong mới gộp vào `main`.

## Chạy local

Cần JDK 21, MySQL 8.4 và kết nối mạng để tải dependency lần đầu.
Tạo database `chat` rỗng với charset `utf8mb4`; chuẩn bị tài khoản riêng có quyền trên database này.
Xem [hướng dẫn database](docs/05-database-va-migration.md).
Từ `backend/`, đặt biến môi trường bằng giá trị local của bạn:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/chat?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8'
$env:DB_USERNAME='chat_app'
$env:DB_PASSWORD='<mat-khau-local>'
.\mvnw.cmd spring-boot:run
```

Linux/macOS: `sh mvnw spring-boot:run`.
Có thể copy `.env.example` thành `backend/.env` và điền thông tin local.
App import `.env` khi chạy từ `Chat/` hoặc `Chat/backend/`; dùng `KEY=value`,
không bọc giá trị trong dấu nháy hoặc thêm `export`. Biến môi trường có độ ưu tiên cao hơn file.
Trong VS Code chọn cấu hình `Chat Backend (8086)` rồi F5/Ctrl+F5.
App yêu cầu database hoạt động; không tự tạo database hay âm thầm chuyển sang database in-memory.

Kiểm tra: `GET http://localhost:8086/actuator/health` trả `{"status":"UP"}`.
Chỉ expose health, không expose env hoặc chi tiết health.
Đổi port bằng biến môi trường `SERVER_PORT` (mặc định 8086).
Ứng dụng chưa có session/authentication hay API chat; chưa dùng để triển khai production.

## Build và kiểm tra

Cần Docker Engine/Docker Desktop đang chạy. Testcontainers tạo MySQL riêng trên port ngẫu nhiên,
không dùng `DB_URL`/tài khoản database local; container được dọn sau test.
Lần đầu cần tải image `mysql:8.4`.

```powershell
.\mvnw.cmd clean verify
java -jar target/chat-backend-0.0.1-SNAPSHOT.jar
```

Lệnh chạy jar vẫn cần các biến DB và MySQL như phần chạy local.
Test khởi động HTTP server ở port ngẫu nhiên, kiểm tra health và endpoint env không được expose.
Test persistence chạy migration fixture trên MySQL, kiểm tra không chạy lặp migration,
lưu/đọc tiếng Việt và emoji, rollback transaction.
Các migration production V1–V4 cũng chạy trong test; Hibernate validate entity khớp schema.
Schema 3NF có 8 bảng nghiệp vụ; test nâng cấp V3 có dữ liệu xác nhận giữ liên kết và mốc đọc.
Test bổ sung kiểm tra unique username, UUID/audit, foreign key, unique token hash,
phiên hết hạn đúng tại expiresAt và thu hồi idempotent.
Test MVC kiểm tra validation Việt/Anh, JSON sai, lỗi nghiệp vụ, lỗi giao thức HTTP,
ẩn thông tin lỗi nội bộ và giữ nguyên response thành công.

## Xử lý lỗi và validation

- `common.exception.BusinessException`: lỗi nghiệp vụ với HTTP status 4xx và message key.
- `common.exception.GlobalExceptionHandler`: chuẩn hóa lỗi trong Spring MVC.
- `common.dto.response.ErrorResponse`: timestamp UTC, status, code, message, path, fieldErrors.
- `common.constant.ErrorConstants`: các code ổn định theo hợp đồng.
- `common.config.ValidationConfig`: Bean Validation dùng chung message bundle.
- `messages.properties` / `messages_en.properties`: tiếng Việt mặc định; dùng
  `Accept-Language: en` để nhận tiếng Anh.

Ví dụ trong service khi đã có nghiệp vụ:

```java
throw new BusinessException(HttpStatus.NOT_FOUND, ErrorConstants.NOT_FOUND);
```

Message riêng module khai báo trong bundle, code trả FE vẫn theo loại lỗi HTTP.
Key không tồn tại sẽ dùng thông báo chung, không trả nguyên key.
Request DTO dùng `@NotBlank(message = "{validation.notBlank}")` cùng `@Valid`.
Không đưa password, token hoặc chi tiết SQL vào message/arguments.
`fieldErrors` luôn là array; lỗi DTO có tên field và message, không chứa rejectedValue.

Các lỗi giao thức vẫn giữ HTTP status/header gốc, ví dụ 405 với header Allow;
405/406 dùng code `VALIDATION_ERROR`, không chuyển thành 500.
Phần này xử lý lỗi Spring MVC; lỗi filter/security và WS sẽ được tích hợp riêng ở bước tương ứng.
Các endpoint `/test/...` chỉ nằm trong test, không được thêm vào ứng dụng.

## Các bước review tiếp theo

1. Service/API đăng ký, đăng nhập/đăng xuất, mã hóa mật khẩu và session cookie.
2. Module nghiệp vụ, phân quyền hội thoại và WebSocket.

Mỗi bước dừng để review; chỉ commit/push khi được yêu cầu.

Quy tắc BE được lưu trong `docs/` của thư mục backend.

Đọc [convention](docs/01-kien-truc-va-convention.md),
[quy tắc code](docs/02-quy-tac-trien-khai-code.md),
[quy trình Git](docs/03-git-workflow.md) và
[hợp đồng API](docs/04-hop-dong-api.md) trước khi code.
