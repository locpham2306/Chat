# CHAT APPLICATION

> Ứng dụng nhắn tin trực tuyến gồm client React và server Spring Boot.

---

## 🎯 MỤC TIÊU

Hệ thống cung cấp:

- Đăng ký, đăng nhập và quản lý hồ sơ người dùng.
- Chat cá nhân và chat nhóm theo thời gian thực.
- Gửi tin nhắn, file đính kèm và xem lịch sử hội thoại.
- Hiển thị trạng thái online/offline, đang nhập, đã đọc và số tin chưa đọc.
- Quản lý thông tin nhóm và thành viên nhóm.

---

## ⚙️ CÔNG NGHỆ SỬ DỤNG

| Thành phần | Công nghệ |
|------------|-----------|
| Client | TypeScript, React 19, Vite, pnpm |
| Server | Java 21, Spring Boot 3.5.16, Maven |
| Database | MySQL 8.4, Spring Data JPA, Flyway |
| Realtime | WebSocket raw JSON |
| Xác thực | Session cookie HttpOnly, BCrypt |

---

## 🚀 HƯỚNG DẪN CHẠY

### Server

```powershell
cd source/server
Copy-Item .env.example .env
.\mvnw.cmd spring-boot:run
```

Server mặc định chạy tại `http://localhost:8086`.

### Client

```powershell
cd source/client
Copy-Item .env.example .env
corepack pnpm install
corepack pnpm dev
```

Client mặc định chạy tại `http://localhost:3001`.

---

## 🔗 GIAO TIẾP

| Thành phần | Địa chỉ | Mục đích |
|------------|---------|----------|
| REST API | `http://localhost:8086/api` | Tài khoản, người dùng, hội thoại, lịch sử và file |
| WebSocket | `ws://localhost:8086/ws` | Tin nhắn, typing, trạng thái đọc và presence realtime |
| Health check | `http://localhost:8086/actuator/health` | Kiểm tra server |

Danh sách endpoint chi tiết nằm trong [tài liệu server](source/server/README.md).

---

## 📦 CẤU TRÚC

```text
Chat/
├── README.md
└── source/
    ├── client/                 # Giao diện React
    │   ├── README.md
    │   ├── package.json
    │   ├── public/
    │   └── src/
    └── server/                 # API và WebSocket Spring Boot
        ├── README.md
        ├── pom.xml
        ├── scripts/
        └── src/
```

---

## 🧪 KIỂM TRA BUILD

```powershell
# Server
cd source/server
.\mvnw.cmd -DskipTests clean package

# Client
cd ../client
corepack pnpm lint
corepack pnpm build
```

---

## 📝 GHI CHÚ

- Server dùng file `source/server/.env`; client dùng `source/client/.env`.
- Cần tạo database MySQL trước khi khởi động server.
- Flyway tự tạo và cập nhật schema khi server chạy.
- File tải lên được lưu tại đường dẫn do `FILE_STORAGE_ROOT` cấu hình.
- Hướng dẫn riêng: [client](source/client/README.md) và [server](source/server/README.md).
