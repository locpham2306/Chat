# Chat

Repository chung cho ứng dụng chat: backend và frontend được phát triển độc lập.

## Cấu trúc

```text
Chat/
├── backend/       # Backend Spring Boot (chưa dựng base)
├── docs/          # Quy tắc code, Git và hợp đồng API
└── frontend/      # Sẽ được thêm khi bắt đầu làm FE
```

Hiện repo chỉ có cấu trúc và tài liệu; chưa có ứng dụng chạy được, build, database hoặc dependency.
Framework frontend, phiên bản Java/Spring Boot, build tool và database sẽ được chốt khi dựng base.

## Tài liệu

- [Kiến trúc và convention](docs/01-kien-truc-va-convention.md)
- [Quy tắc triển khai code](docs/02-quy-tac-trien-khai-code.md)
- [Quy trình Git](docs/03-git-workflow.md)
- [Hợp đồng API chat](docs/04-hop-dong-api.md)
- [Hướng dẫn backend](backend/README.md)

Convention tổ chức code tham khảo QLCC, được điều chỉnh cho ứng dụng chat.
Hợp đồng API của Chat là nguồn quyết định về endpoint, method, response và enum.
