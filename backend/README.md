# Backend

Thư mục dành cho toàn bộ source, build, cấu hình và test của backend Spring Boot.
Chưa dựng base; hiện chưa có lệnh chạy hoặc test.

Phát triển trên nhánh `backend`, giữ toàn bộ code trong thư mục `backend/`.
Khi hoàn thiện và kiểm tra xong mới gộp vào `main`.

Khi bắt đầu triển khai:

1. Chốt Java, Spring Boot, build tool, database và root package.
2. Đặt project và build wrapper ngay trong `backend/`, không tạo thêm tầng `backend/backend`.
3. Tạo cấu hình mẫu không chứa secret và hướng dẫn chạy local.
4. Thêm migration, test và CI tương ứng với tính năng thực sự triển khai.

Đọc [convention](../docs/01-kien-truc-va-convention.md),
[quy tắc code](../docs/02-quy-tac-trien-khai-code.md) và
[hợp đồng API](../docs/04-hop-dong-api.md) trước khi code.
