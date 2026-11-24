# MODULE SERVER

> 📘 Module server của ứng dụng Chat realtime, xử lý tài khoản, phiên đăng nhập, hội thoại, tin nhắn, trạng thái đọc, file đính kèm và WebSocket.

---

## 🎯 MỤC TIÊU

Server chịu trách nhiệm:

- Tiếp nhận yêu cầu HTTP và kết nối WebSocket từ client.
- Đăng ký, đăng nhập, đăng xuất và xác thực bằng session cookie.
- Quản lý người dùng, hội thoại DIRECT/GROUP và thành viên nhóm.
- Lưu tin nhắn, lịch sử chat, file đính kèm và mốc đọc.
- Phát realtime tin nhắn mới, trạng thái đang nhập, đã đọc và ONLINE/OFFLINE.
- Kiểm tra quyền truy cập trước khi trả dữ liệu hoặc file.

---

## ⚙️ CÔNG NGHỆ SỬ DỤNG

| Thành phần | Công nghệ |
|------------|-----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 3.5.16 |
| REST API | Spring Web MVC |
| Realtime | Spring WebSocket, raw JSON, không dùng STOMP |
| Database | MySQL 8.4 |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway |
| Authentication | Session cookie HttpOnly, BCrypt |
| File storage | Local filesystem, metadata lưu trong MySQL |
| Build | Maven Wrapper |
| Test | JUnit 5, Spring Boot Test, Testcontainers MySQL |

---

## 🚀 HƯỚNG DẪN CHẠY

### Yêu cầu

- JDK 21.
- MySQL 8.4.
- Docker Desktop nếu chạy test tích hợp.

### Cài đặt

Tạo database MySQL rỗng:

```sql
CREATE DATABASE chat
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_as_ci;
```

Từ thư mục `backend/`, sao chép cấu hình mẫu:

```bash
cp .env.example .env
```

Điền thông tin kết nối trong `.env`:

```properties
DB_URL=jdbc:mysql://localhost:3306/chat?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8
DB_USERNAME=chat_app
DB_PASSWORD=your_password
SERVER_PORT=8086
SESSION_COOKIE_SECURE=false
CORS_ALLOWED_ORIGINS=http://localhost:3001
FILE_STORAGE_ROOT=./uploads
```

Flyway tự tạo và cập nhật schema khi server khởi động.

### Khởi động server

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Server chạy tại: `http://localhost:8086`

WebSocket chạy tại: `ws://localhost:8086/ws`

Kiểm tra health:

```bash
curl http://localhost:8086/actuator/health
```

Kết quả:

```json
{"status":"UP"}
```

---

## 🔗 API

Các API yêu cầu đăng nhập sử dụng cookie session do endpoint login tạo.

| Endpoint | Method | Input chính | Output chính |
|----------|--------|------------|--------------|
| `/actuator/health` | GET | — | `{"status":"UP"}` |
| `/api/auth/register` | POST | `username, password, fullName` | Tạo tài khoản |
| `/api/auth/login` | POST | `username, password` | Cookie session và thông báo đăng nhập |
| `/api/auth/logout` | POST | Cookie session | Thu hồi phiên đăng nhập |
| `/api/users` | GET | `keyword, page, size` | Danh sách user công khai |
| `/api/users/me` | GET | Cookie session | User đang đăng nhập |
| `/api/users/me` | PUT | `fullName, avatarFileId` | User sau khi cập nhật |
| `/api/files` | POST | Multipart field `file` | Metadata và `fileId` |
| `/api/files/{id}` | GET | File ID | Nội dung file |
| `/api/conversations` | GET | `page, size` | Danh sách hội thoại, tin cuối và số chưa đọc |
| `/api/conversations/direct` | GET | `recipientId` | DIRECT đã có và thông tin người nhận |
| `/api/conversations/group` | POST | `name, avatarFileId, memberIds` | Nhóm vừa tạo |
| `/api/conversations/{id}` | GET | Conversation ID | Chi tiết hội thoại |
| `/api/conversations/{id}` | PUT | `name, avatarFileId` | Nhóm sau khi cập nhật |
| `/api/conversations/{id}/messages` | GET | `before, limit` | Lịch sử tin nhắn theo cursor |
| `/api/conversations/{id}/read` | PUT | `lastReadMessageId` | Mốc đọc và số chưa đọc |
| `/api/conversations/{id}/members` | GET | `page, size` | Thành viên đang hoạt động |
| `/api/conversations/{id}/members` | POST | `memberIds` | Kết quả thêm thành viên |
| `/api/conversations/{id}/members/{userId}` | DELETE | User ID | Rời nhóm hoặc xóa thành viên |
| `/api/conversations/{id}/available-members` | GET | `keyword, page, size` | User có thể thêm vào nhóm |

### WebSocket

Client kết nối `/ws` bằng cookie session hợp lệ. Frame chung:

```json
{
  "event": "EVENT_NAME",
  "data": {}
}
```

| Chiều | Event | Mục đích |
|-------|-------|----------|
| FE → BE | `SEND_MESSAGE` | Gửi tin bằng `conversationId` hoặc `recipientId` |
| FE → BE | `TYPING` | Báo đang nhập hoặc đã dừng nhập |
| BE → FE | `MESSAGE_ACK` | Xác nhận tin đã được lưu |
| BE → FE | `NEW_MESSAGE` | Phát tin mới tới thành viên |
| BE → FE | `READ_RECEIPT` | Báo user đã đọc tới tin nào |
| BE → FE | `PRESENCE_CHANGED` | Báo ONLINE/OFFLINE |
| BE → FE | `ERROR` | Trả lỗi của frame WebSocket |

---

## 📦 CẤU TRÚC

```text
backend/
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .env.example
├── config/
│   └── application-local.yml
├── scripts/
│   └── seed-dev.sql
├── src/
│   ├── main/
│   │   ├── java/com/chat/
│   │   │   ├── common/        # Response chung, constant và base entity
│   │   │   ├── config/        # CORS, cookie, WebSocket và validation
│   │   │   ├── exception/     # Chuẩn hóa lỗi toàn hệ thống
│   │   │   ├── integration/   # Storage và WebSocket
│   │   │   ├── modules/       # auth, user, conversation, message, file
│   │   │   └── utils/         # Helper dùng chung
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/
│   │       └── messages*.properties
│   └── test/
└── uploads/                    # File local khi chạy development
```

---

## 🧪 TEST

Chạy toàn bộ test:

```powershell
.\mvnw.cmd clean verify
```

Test tích hợp sử dụng Testcontainers nên cần Docker đang chạy.

Chỉ compile source:

```powershell
.\mvnw.cmd -DskipTests compile
```

Test nhanh health API:

```bash
curl http://localhost:8086/actuator/health
```

---

## 📝 GHI CHÚ

- Port mặc định: **8086**.
- Có thể thay đổi bằng `SERVER_PORT` trong file `.env`.
- Frontend local mặc định: `http://localhost:3001`.
- Cookie local cần `SESSION_COOKIE_SECURE=false`; production HTTPS nên dùng `true`.
- File thật được lưu tại `FILE_STORAGE_ROOT`; bảng `files` chỉ lưu metadata và `storageKey`.
- Giới hạn mặc định: **20 MiB/file**.
- Gửi tin nhắn dùng WebSocket; tải lịch sử và file dùng HTTP.
- Flyway quản lý schema; không dùng Hibernate để tự tạo hoặc sửa bảng.
