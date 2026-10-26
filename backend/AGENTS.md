# Quy tắc làm việc backend

- Đọc `README.md` trong thư mục này và các tài liệu trong `docs/` trước khi sửa BE.
- Quy tắc trong `docs/` áp dụng cho backend, không áp đặt kiến trúc hoặc convention code cho frontend.
- Phát triển trên nhánh `backend`; giữ toàn bộ source, cấu hình, build và test BE trong thư mục `backend/` của repo.
- Tổ chức code theo feature; business logic ở service, truy cập DB ở repository.
- Tuân thủ `docs/01-kien-truc-va-convention.md` và `docs/02-quy-tac-trien-khai-code.md`.
- Theo `docs/03-git-workflow.md`; khi hoàn thiện mới gộp vào `main`.
- Khi đổi API, cập nhật `docs/04-hop-dong-api.md` và tài liệu liên quan để FE đối chiếu.
- HTTP-08 lookup DIRECT, HTTP-09 tạo GROUP, HTTP-10 danh sách, HTTP-11 chi tiết hội thoại
  và HTTP-18/HTTP-19 file đã triển khai; bước tiếp theo theo thứ tự trong hợp đồng là
  HTTP-12 cập nhật nhóm. Làm từng bước;
  chỉ commit/push khi người dùng yêu cầu.
- MySQL 8.4; schema thay đổi qua Flyway, Hibernate chỉ validate. Không chạy migration lên database ngoài dự án.
