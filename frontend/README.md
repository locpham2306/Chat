<<<<<<< HEAD
# Frontend

Thư mục dành cho toàn bộ source, build, cấu hình và test của frontend.
Chưa dựng base; hiện chưa có lệnh chạy hoặc test.

Khi bắt đầu triển khai:

1. Chốt framework, phiên bản runtime và package manager.
2. Đặt project ngay trong `frontend/`, không tạo thêm tầng `frontend/frontend`.
3. Tạo cấu hình mẫu không chứa secret và hướng dẫn chạy local.
4. Thêm test và CI tương ứng với tính năng thực sự triển khai.

Đọc [hợp đồng API](../backend/docs/04-hop-dong-api.md) trước khi tích hợp với BE.
Quy tắc code frontend sẽ được nhóm FE bổ sung riêng.
HTTP và WebSocket phải khớp endpoint, enum và luồng session đã thống nhất.
=======
# MODULE CLIENT

> Frontend React cho hệ thống nhắn tin trực tuyến. Client giao tiếp với backend bằng REST API và WebSocket raw.

---

## 🎯 MỤC TIÊU

Client chịu trách nhiệm:

- Gửi request HTTP đến backend cho đăng ký, đăng nhập, quản lý hồ sơ, hội thoại, nhóm, tin nhắn và tệp đính kèm.
- Kết nối WebSocket để nhận tin nhắn mới, đồng bộ trạng thái online/offline, đang nhập và đã đọc theo thời gian thực.
- Hiển thị giao diện chat cá nhân/nhóm, lịch sử tin nhắn, tìm kiếm người dùng và các thông báo lỗi/trạng thái cho người dùng.

---

## ⚙️ CÔNG NGHỆ SỬ DỤNG

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | TypeScript |
| Runtime và build tool | Node.js, pnpm, Vite |
| Framework giao diện | React 19 |
| Thư viện chính | Axios, TanStack Query, React Hook Form, Zustand, Zod |
| Giao thức | HTTP/REST, WebSocket raw JSON |
| Giao diện | Tailwind CSS, shadcn/ui (Radix UI), Lucide React |

---

## 🚀 HƯỚNG DẪN CHẠY

### Cài đặt

Yêu cầu Node.js `20.19+`, `22.12+` hoặc `24+` để tương thích Vite 8. Nhóm đã kiểm tra dự án với pnpm 12.

```powershell
pnpm install
```

Tạo file môi trường theo hệ điều hành:

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```

```bash
# macOS hoặc Linux
cp .env.example .env
```

### Chạy chương trình

```bash
pnpm dev
```

Mở client tại `http://localhost:3001`.

Để chạy bản production đã build:

```bash
pnpm build
pnpm preview
```

### Cấu hình

Cấu hình trong file `.env`, được tạo từ `.env.example`:

```env
# HTTP API của backend Chat
VITE_API_URL=http://localhost:8086

# Raw WebSocket của backend Chat
VITE_WS_URL=ws://localhost:8086/ws

# Thời gian chờ HTTP, đơn vị mili giây
VITE_TIMEOUT=10000
```

- `VITE_API_URL`: base URL cho các request HTTP/REST.
- `VITE_WS_URL`: endpoint WebSocket raw JSON tại `/ws`.
- `VITE_TIMEOUT`: thời gian chờ request HTTP tính bằng mili giây.

Không đặt token, mật khẩu, khóa API hoặc secret khác vào `.env` frontend vì biến `VITE_*` được đưa vào mã chạy trên trình duyệt.

---

## 📦 CẤU TRÚC

```text
frontend/
├── README.md                     # Tài liệu module client
├── .env.example                  # Mẫu cấu hình môi trường
├── package.json                  # Scripts và dependencies
├── vite.config.ts                # Cấu hình Vite và proxy backend
├── src/
│   ├── main.tsx                  # Điểm khởi động React
│   ├── app.tsx                   # Component ứng dụng gốc
│   ├── apis/                     # Module gọi HTTP API
│   ├── components/               # Component UI, chat, nhóm và hồ sơ
│   ├── hooks/                    # Hooks HTTP, WebSocket và TanStack Query
│   ├── lib/                      # Axios, cấu hình môi trường, WebSocket client
│   ├── pages/                    # Các màn hình login, đăng ký và chat
│   ├── routes/                   # Router và protected route
│   ├── stores/                   # Zustand stores cho state cục bộ
│   └── types/                    # Kiểu dữ liệu API, chat, user, file, WebSocket
└── public/                       # Tài nguyên tĩnh
```

---

## 💡 SỬ DỤNG

1. Khởi động backend Chat tại `http://localhost:8086`.
2. Chạy `pnpm dev` trong thư mục `frontend`.
3. Mở `http://localhost:3001`, đăng ký hoặc đăng nhập.
4. Dùng giao diện để tìm người dùng, tạo hội thoại, gửi tin nhắn hoặc tệp đính kèm.

Với cấu hình mẫu, client kết nối trực tiếp đến các URL backend đã khai báo. Nếu để trống `VITE_API_URL` và `VITE_WS_URL`, client sẽ dùng đường dẫn tương đối `/api` và `/ws`; Vite proxy các đường dẫn này về backend trong development. HTTP dùng cookie session; WebSocket dùng cùng session cookie và không sử dụng STOMP.

Các lệnh kiểm tra:

```bash
pnpm test:run
pnpm test:typecheck
pnpm lint
pnpm prettier
pnpm build
```

---

## 📝 GHI CHÚ

- Đảm bảo backend đã chạy trước khi khởi động client.
- Backend local mặc định: `http://localhost:8086`.
- Client local mặc định: `http://localhost:3001`.
- Backend cần cho phép origin `http://localhost:3001` và khởi động endpoint WebSocket `/ws`.
- Khi `VITE_WS_URL` không được cấu hình, client tự suy ra `ws(s)://<host-client>/ws`; Vite sẽ proxy endpoint này trong development.
>>>>>>> frontend
