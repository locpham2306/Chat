# Chat

Repository chung cho ứng dụng chat: backend và frontend được phát triển độc lập.

## Cấu trúc

```text
Chat/
├── backend/       # Spring Boot, đang review entity/schema chat
│   └── docs/      # Quy tắc BE và hợp đồng API
└── frontend/      # Frontend (chưa dựng base)
```

Backend có khung Java 21 / Spring Boot 3.5.16 / Maven và health check.
Đã thêm xử lý lỗi HTTP và Bean Validation dùng message bundle.
Cấu hình persistence dùng MySQL, JPA và Flyway; có schema/entity chat và repository user/session,
chưa có API nghiệp vụ.
Framework frontend chưa chốt.

BE phát triển trên nhánh `backend`, code nằm trong `backend/`.
`main` dành cho bản tích hợp hoàn thiện; khi BE sẵn sàng mới gộp từ `backend` vào `main`.

## Tài liệu

- [Kiến trúc và convention BE](backend/docs/01-kien-truc-va-convention.md)
- [Quy tắc triển khai code BE](backend/docs/02-quy-tac-trien-khai-code.md)
- [Quy trình Git BE](backend/docs/03-git-workflow.md)
- [Hợp đồng API chat](backend/docs/04-hop-dong-api.md)
- [Database và migration](backend/docs/05-database-va-migration.md)
- [Schema và quan hệ DB](backend/docs/06-database-schema.md)
- [Hướng dẫn backend](backend/README.md)
- [Hướng dẫn frontend](frontend/README.md)

Convention tổ chức code tham khảo QLCC, được điều chỉnh cho ứng dụng chat.
Hợp đồng API của Chat là nguồn quyết định về endpoint, method, response và enum.
