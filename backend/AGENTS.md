# Quy tắc làm việc backend

- Đọc `README.md` trong thư mục này và các tài liệu trong `docs/` trước khi sửa BE.
- Quy tắc trong `docs/` áp dụng cho backend, không áp đặt kiến trúc hoặc convention code cho frontend.
- Phát triển trên nhánh `backend`; giữ toàn bộ source, cấu hình, build và test BE trong thư mục `backend/` của repo.
- Tổ chức code theo feature; business logic ở service, truy cập DB ở repository.
- Tuân thủ `docs/01-kien-truc-va-convention.md` và `docs/02-quy-tac-trien-khai-code.md`.
- Theo `docs/03-git-workflow.md`; khi hoàn thiện mới gộp vào `main`.
- Khi đổi API, cập nhật `docs/04-hop-dong-api.md` và tài liệu liên quan để FE đối chiếu.
- Đang review bước 2: lỗi HTTP và validation chung. Làm từng bước; chỉ commit/push khi người dùng yêu cầu.
