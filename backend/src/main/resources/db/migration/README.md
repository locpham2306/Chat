# Database migrations

V1 tạo bảng users; V2 tạo bảng sessions.
Entity/repository nằm ở modules.user và modules.auth.

- Đặt SQL tại đây: `V1__create_users.sql`, `V2__create_sessions.sql`, ...
- Chỉ tạo migration khi có thay đổi schema thực sự, không thêm migration rỗng.
- Không sửa migration đã chạy trên môi trường dùng chung; thêm version mới.
- Không thêm `schema.sql` hoặc dùng Hibernate `update/create`.
- Database đích phải được tạo trước; cấu hình URL/user/password bằng biến môi trường.
- Không baseline tự động một database cũ; kiểm tra schema trước khi lập kế hoạch chuyển đổi.
- Fixture kiểm thử nằm trong `src/test/resources/db/test-migration`, không đưa vào production.
