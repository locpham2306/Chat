# Chat

Repository chung cho ứng dụng chat: backend và frontend được phát triển độc lập.

## Cấu trúc

```text
Chat/
├── backend/       # Backend Spring Boot (chưa dựng base)
│   └── docs/      # Quy tắc BE và hợp đồng API
└── frontend/      # Frontend (chưa dựng base)
```

Hiện repo chỉ có cấu trúc và tài liệu; chưa có ứng dụng chạy được, build, database hoặc dependency.
Framework frontend, phiên bản Java/Spring Boot, build tool và database sẽ được chốt khi dựng base.

BE phát triển trên nhánh `backend`, code nằm trong `backend/`.
`main` dành cho bản tích hợp hoàn thiện; khi BE sẵn sàng mới gộp từ `backend` vào `main`.

## Tài liệu

- [Kiến trúc và convention BE](backend/docs/01-kien-truc-va-convention.md)
- [Quy tắc triển khai code BE](backend/docs/02-quy-tac-trien-khai-code.md)
- [Quy trình Git BE](backend/docs/03-git-workflow.md)
- [Hợp đồng API chat](backend/docs/04-hop-dong-api.md)
- [Hướng dẫn backend](backend/README.md)
- [Hướng dẫn frontend](frontend/README.md)

Convention tổ chức code tham khảo QLCC, được điều chỉnh cho ứng dụng chat.
Hợp đồng API của Chat là nguồn quyết định về endpoint, method, response và enum.
