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
