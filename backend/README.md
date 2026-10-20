# Backend

Thư mục dành cho toàn bộ source, build, cấu hình và test của backend Spring Boot.
Bước 1: khung Spring Boot chạy độc lập, chưa có database hoặc API nghiệp vụ.
Java 21, Spring Boot 3.5.16, Maven Wrapper 3.9.5; root package `com.chat`.

Phát triển trên nhánh `backend`, giữ toàn bộ code trong thư mục `backend/`.
Khi hoàn thiện và kiểm tra xong mới gộp vào `main`.

## Chạy local

Cần JDK 21 và kết nối mạng để tải dependency lần đầu. Từ `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS: `sh mvnw spring-boot:run`.

Kiểm tra: `GET http://localhost:8080/actuator/health` trả `{"status":"UP"}`.
Chỉ expose health, không expose env hoặc chi tiết health.
Đổi port bằng biến môi trường `SERVER_PORT` (mặc định 8080).
Ứng dụng chưa có session/authentication hay API chat; chưa dùng để triển khai production.

## Build và kiểm tra

```powershell
.\mvnw.cmd clean verify
java -jar target/chat-backend-0.0.1-SNAPSHOT.jar
```

Test khởi động HTTP server ở port ngẫu nhiên, kiểm tra health và endpoint env không được expose.

## Các bước review tiếp theo

1. Xử lý lỗi và validation thống nhất theo contract.
2. Chốt database, thêm migration và cấu hình persistence.
3. Session cookie, đăng nhập/đăng xuất và phân quyền.
4. Module nghiệp vụ và WebSocket.

Mỗi bước dừng để review; chỉ commit/push khi được yêu cầu.

Quy tắc BE được lưu trong `docs/` của thư mục backend.

Đọc [convention](docs/01-kien-truc-va-convention.md),
[quy tắc code](docs/02-quy-tac-trien-khai-code.md),
[quy trình Git](docs/03-git-workflow.md) và
[hợp đồng API](docs/04-hop-dong-api.md) trước khi code.
