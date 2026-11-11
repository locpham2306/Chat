# Quy tắc làm việc backend

- Đọc `README.md` trong thư mục này và các tài liệu trong `docs/` trước khi sửa BE.
- Quy tắc trong `docs/` áp dụng cho backend, không áp đặt kiến trúc hoặc convention code cho frontend.
- Phát triển trên nhánh `backend`; giữ toàn bộ source, cấu hình, build và test BE trong thư mục `backend/` của repo.
- Tổ chức code theo feature; business logic ở service, truy cập DB ở repository.
- Tuân thủ `docs/01-kien-truc-va-convention.md` và `docs/02-quy-tac-trien-khai-code.md`.
- Theo `docs/03-git-workflow.md`; khi hoàn thiện mới gộp vào `main`.
- Khi đổi API, cập nhật `docs/04-hop-dong-api.md` và tài liệu liên quan để FE đối chiếu.
- HTTP-08 lookup DIRECT, HTTP-09 tạo GROUP, HTTP-10 danh sách, HTTP-11 chi tiết, HTTP-12
  cập nhật hội thoại, HTTP-13 danh sách thành viên, HTTP-14 thêm thành viên,
  HTTP-15 xóa thành viên/rời nhóm, HTTP-20 tìm ứng viên thêm vào nhóm
  và HTTP-18/HTTP-19 file đã triển khai. HTTP-16 lịch sử theo cursor và HTTP-17 mốc đọc
  chỉ tiến đã triển khai. Raw WebSocket `/ws` đã có SEND_MESSAGE, MESSAGE_ACK, NEW_MESSAGE,
  TYPING, READ_RECEIPT, PRESENCE_CHANGED và ERROR. Làm từng bước theo hợp đồng;
  chỉ commit/push khi người dùng yêu cầu.
- MySQL 8.4; schema thay đổi qua Flyway, Hibernate chỉ validate. Không chạy migration lên database ngoài dự án.
