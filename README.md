# Chat

Repository chung cho ứng dụng chat: backend và frontend được phát triển độc lập.

## Cấu trúc

```text
Chat/
├── backend/       # Spring Boot REST API, WebSocket và persistence
│   └── docs/      # Quy tắc BE và hợp đồng API
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

- [Kiến trúc và convention BE](backend/docs/01-kien-truc-va-convention.md)
- [Quy tắc triển khai code BE](backend/docs/02-quy-tac-trien-khai-code.md)
- [Quy trình Git BE](backend/docs/03-git-workflow.md)
- [Hợp đồng API chat](backend/docs/04-hop-dong-api.md)
- [Database và migration](backend/docs/05-database-va-migration.md)
- [Schema và quan hệ DB](backend/docs/06-database-schema.md)
- [Session và cookie](backend/docs/07-session-base.md)
- [Hướng dẫn backend](backend/README.md)
- [Hướng dẫn frontend](frontend/README.md)

Convention tổ chức code tham khảo QLCC, được điều chỉnh cho ứng dụng chat.
Hợp đồng API của Chat là nguồn quyết định về endpoint, method, response và enum.
