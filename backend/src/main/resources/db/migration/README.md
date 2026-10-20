# Database migrations

Chưa có bảng nghiệp vụ ở bước 3. Flyway quản lý lịch sử migration;
schema user/session sẽ được thêm ở bước auth sau khi review.

- Đặt SQL tại đây: `V1__create_users.sql`, `V2__create_sessions.sql`, ...
- Chỉ tạo migration khi có thay đổi schema thực sự, không thêm migration rỗng.
- Không sửa migration đã chạy trên môi trường dùng chung; thêm version mới.
- Không thêm `schema.sql` hoặc dùng Hibernate `update/create`.
- Database đích phải được tạo trước; cấu hình URL/user/password bằng biến môi trường.
- Không baseline tự động một database cũ; kiểm tra schema trước khi lập kế hoạch chuyển đổi.
- Fixture kiểm thử nằm trong `src/test/resources/db/test-migration`, không đưa vào production.
