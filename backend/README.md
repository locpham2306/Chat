# Backend

Thư mục dành cho toàn bộ source, build, cấu hình và test của backend Spring Boot.
Đã dựng entity/schema, API register/login/logout, filter session, xem/cập nhật `/api/users/me`,
tìm kiếm user và upload/download file có phân quyền. Raw WebSocket `/ws` dùng cookie session
đã hỗ trợ gửi tin, retry idempotent, typing, read receipt, presence và lỗi frame.
HTTP lịch sử tin nhắn dùng cursor; HTTP mốc đọc chỉ tiến và phát receipt sau commit.
Lookup hội thoại DIRECT đã có và không tạo dữ liệu khi hai user chưa từng nhắn tin;
API tạo nhóm tạo OWNER/MEMBER trong cùng transaction. Danh sách hội thoại chỉ trả
membership đang hoạt động, sắp xếp theo hoạt động mới nhất và kèm tin cuối/số chưa đọc.
API chi tiết hội thoại chỉ cho thành viên đang hoạt động truy cập; OWNER có thể cập nhật
tên và avatar của GROUP. Danh sách thành viên có phân trang và không trả người đã rời.
MEMBER có thể rời nhóm, còn OWNER có thể xóa MEMBER nhưng không thể tự rời nhóm.
Tin đầu DIRECT tạo conversation + hai membership + message trong cùng transaction; ACK và
NEW_MESSAGE chỉ phát sau commit. Logout/phiên hết hạn đóng đúng kết nối của phiên đó.
Xem [session và cookie](docs/07-session-base.md).
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

### Nạp dữ liệu mẫu local

Khởi động backend một lần để Flyway tạo schema V1–V4, sau đó chạy script từ thư mục
`backend/`. Dữ liệu mẫu chỉ dành cho database local, không chạy trên môi trường dùng chung
hoặc production.

PowerShell/MySQL CLI:

```powershell
mysql --default-character-set=utf8mb4 -u root -p chat -e "source scripts/seed-dev.sql"
```

MySQL Workbench: mở `scripts/seed-dev.sql`, chọn schema `chat` rồi chạy toàn bộ script.
Script có thể chạy lại, không nằm trong Flyway và không tự chạy khi start ứng dụng.

Các tài khoản mẫu dùng chung mật khẩu `123456`:

- `seed_an`
- `seed_binh`
- `seed_chi`
- `seed_dung`

Seed tạo một hội thoại DIRECT, một GROUP có tin nhắn và một GROUP chưa có tin để kiểm
tra danh sách hội thoại, tin cuối và số chưa đọc. Không seed session; đăng nhập qua
`POST /api/auth/login` để backend tự tạo cookie session hợp lệ.

Kiểm tra: `GET http://localhost:8086/actuator/health` trả `{"status":"UP"}`.
Chỉ expose health, không expose env hoặc chi tiết health.
Đổi port bằng biến môi trường `SERVER_PORT` (mặc định 8086).
Session token nằm trong cookie HttpOnly và chỉ được lưu hash trong DB.
Cookie mặc định Secure=true; khi chạy HTTP local dùng `SESSION_COOKIE_SECURE=false`.
CORS mặc định chỉ cho `http://localhost:3001` và bật credentials để trình duyệt gửi
cookie session. Cấu hình nhiều origin bằng `CORS_ALLOWED_ORIGINS` phân tách bởi dấu phẩy;
không dùng wildcard `*`.
WebSocket dùng `ws://localhost:8086/ws` khi chạy local HTTP, raw JSON và không dùng STOMP.
Handshake bắt buộc cookie `token` hợp lệ và Origin nằm trong `CORS_ALLOWED_ORIGINS`; không
truyền token trong URL/frame. Server ping mỗi 30 giây, đổi bằng `WEBSOCKET_HEARTBEAT_INTERVAL`.
File được lưu ngoài database tại `FILE_STORAGE_ROOT` (mặc định `./uploads`), còn bảng
`files` chỉ lưu metadata. Upload chấp nhận mọi loại file; Content-Type thiếu hoặc sai
được lưu là `application/octet-stream`. Giới hạn mặc định là 20 MiB/file và có thể
cấu hình bằng `FILE_MAX_SIZE`, `FILE_MAX_REQUEST_SIZE`.
Chưa dùng để triển khai production.

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
ẩn thông tin lỗi nội bộ và response wrapper chung.
Test auth kiểm tra BCrypt, session hạn 365 ngày, cookie, xác thực `/api/users/me`
và không serialize token thô. Test hồ sơ kiểm tra request đầy đủ, trim tên,
xóa avatar và quyền sử dụng file ảnh. Test tìm kiếm user kiểm tra username/fullName,
loại người đang đăng nhập, phân trang và validation query.
Test file kiểm tra mọi loại MIME, giới hạn dung lượng, metadata an toàn và quyền tải của uploader,
avatar công khai cho user đã đăng nhập, thành viên hội thoại và người không có quyền.
Test HTTP-08 kiểm tra lookup hai chiều luôn trả 200 với peer, conversation có thể null,
recipient sai và bảo đảm không tạo hội thoại rỗng.
Test HTTP-09 kiểm tra role, member trùng, user/avatar không hợp lệ, rollback và quyền
xem avatar nhóm.
Test HTTP-10 kiểm tra phân trang, chỉ lấy membership đang hoạt động, thứ tự hoạt động,
peer của DIRECT, memberCount, lastMessage kèm thông tin người gửi/attachment và unreadCount.
Test HTTP-11 kiểm tra response chi tiết, peer của DIRECT, memberCount hiện tại, hội thoại
không tồn tại, người ngoài, thành viên đã rời, ID sai và session thiếu.
Test HTTP-12 kiểm tra quyền OWNER, trim tên, đặt/xóa avatar, file sai quyền/không tồn tại,
body thiếu/sai và từ chối cập nhật DIRECT.
Test HTTP-13 kiểm tra phân trang thành viên, public profile, role, thứ tự ổn định,
loại người đã rời và chặn người ngoài hội thoại.
Test HTTP-14 kiểm tra OWNER thêm thành viên, deduplicate ID, bỏ qua thành viên hiện tại,
kích hoạt lại người đã rời, rollback khi có user sai và từ chối MEMBER/DIRECT.
Test HTTP-15 kiểm tra MEMBER tự rời, OWNER xóa MEMBER, xóa idempotent, giữ membership
để có thể thêm lại và từ chối OWNER rời nhóm, MEMBER xóa người khác hoặc thao tác trên DIRECT.
Test HTTP-20 kiểm tra tìm ứng viên thêm vào nhóm, loại OWNER/thành viên hiện tại,
cho phép thêm lại người đã rời, phân trang và chỉ OWNER được gọi.
Test HTTP-16 kiểm tra cursor loại trừ, thứ tự mới đến cũ, attachment và phân quyền.
Test HTTP-17 kiểm tra mốc đọc chỉ tiến, unreadCount và READ_RECEIPT dùng mốc thực tế.
Test WebSocket kiểm tra cookie/Origin handshake, SEND_MESSAGE tạo dữ liệu trước ACK/broadcast,
retry không tạo trùng, attachment, TYPING, lỗi frame và presence theo số kết nối.

## Xử lý lỗi và validation

- `common.wrapper.BaseResponse`: envelope chung `code/strCode/message/content` theo QLCC.
- `common.dto.response.PageResponse`: dữ liệu và metadata phân trang.
- `exception.BusinessException` cùng các exception theo HTTP status: lỗi nghiệp vụ với message key.
- `exception.GlobalExceptionHandler`: chuẩn hóa lỗi Spring MVC vào `BaseResponse`.
- `common.dto.response.ErrorDetailResponse`: timestamp UTC, status, path, fieldErrors.
- `utils.MessageUtils`, `StringUtils`, `PageableUtils`: helper dùng chung đã có nhu cầu.
- `common.constant.ErrorConstants`: các code ổn định theo hợp đồng.
- `config.ValidationConfig`: Bean Validation dùng chung message bundle.
- `messages.properties` / `messages_en.properties`: tiếng Việt mặc định; dùng
  `Accept-Language: en` để nhận tiếng Anh.

Ví dụ trong service khi đã có nghiệp vụ:

```java
throw new NotFoundException(ErrorConstants.NOT_FOUND);
```

Message riêng module khai báo trong bundle, `strCode` trả FE vẫn theo loại lỗi HTTP.
Key không tồn tại sẽ dùng thông báo chung, không trả nguyên key.
Request DTO dùng `@NotBlank(message = "{validation.notBlank}")` cùng `@Valid`.
Không đưa password, token hoặc chi tiết SQL vào message/arguments.
`fieldErrors` trong `content` luôn là array; lỗi DTO có tên field và message,
không chứa rejectedValue.

Các lỗi giao thức vẫn giữ HTTP status/header gốc, ví dụ 405 với header Allow;
405/406 dùng code `VALIDATION_ERROR`, không chuyển thành 500.
Filter session chuyển lỗi xác thực qua cùng Error model; lỗi WS sẽ được tích hợp ở bước tương ứng.
Các endpoint `/test/...` chỉ nằm trong test, không được thêm vào ứng dụng.

## Các bước review tiếp theo

1. Review HTTP-16/HTTP-17 và READ_RECEIPT.
2. Tiếp tục các API/event còn lại theo hợp đồng.

Mỗi bước dừng để review; chỉ commit/push khi được yêu cầu.

Quy tắc BE được lưu trong `docs/` của thư mục backend.

Đọc [convention](docs/01-kien-truc-va-convention.md),
[quy tắc code](docs/02-quy-tac-trien-khai-code.md),
[quy trình Git](docs/03-git-workflow.md) và
[hợp đồng API](docs/04-hop-dong-api.md) trước khi code.
