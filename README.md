# Chat

Repository chung cho ứng dụng chat: backend và frontend được phát triển độc lập.

## Cấu trúc

```text
Chat/
├── backend/       # Spring Boot REST API, WebSocket và persistence
└── frontend/      # React client cho chat cá nhân/nhóm
```

Backend dùng Java 21, Spring Boot 3.5.16, Maven, MySQL, JPA và Flyway. Module cung cấp
xác thực bằng session cookie, hồ sơ người dùng, hội thoại/thành viên, tệp đính kèm,
lịch sử tin nhắn theo cursor, mốc đọc và raw WebSocket `/ws` cho tin nhắn, typing,
read receipt và presence.

Frontend dùng TypeScript, React 19, Vite và pnpm. Client tích hợp REST/WebSocket của
backend và cung cấp các luồng đăng ký/đăng nhập, chat cá nhân/nhóm, quản lý nhóm,
hồ sơ, tệp đính kèm và đồng bộ thời gian thực.

BE phát triển trên nhánh `backend`, code nằm trong `backend/`.
`main` dành cho bản tích hợp hoàn thiện; khi BE sẵn sàng mới gộp từ `backend` vào `main`.

## Tài liệu

- [Hướng dẫn backend](backend/README.md)
- [Hướng dẫn frontend](frontend/README.md)
