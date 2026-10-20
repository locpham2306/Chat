# Database và migration

## Phạm vi bước 3

- MySQL 8.4, Spring Data JPA, HikariCP, Flyway.
- Dependency version do Spring Boot BOM quản lý.
- Chưa tạo bảng nghiệp vụ hoặc BaseEntity; schema user/session sẽ review trong bước auth.
- Chưa thêm Redis, auditing user hoặc soft delete khi chưa có nghiệp vụ dùng đến.

## Kết nối local

Chuẩn bị MySQL riêng cho Chat, không dùng database của QLCC hoặc dự án khác.
Với tài khoản quản trị MySQL, tạo database rỗng:

```sql
CREATE DATABASE chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Tạo tài khoản riêng và cấp quyền DDL/DML cần thiết trong database `chat`
để Flyway quản lý schema và app truy xuất dữ liệu. Không dùng root để chạy app.
Credential không đưa vào Git; tham khảo `backend/.env.example`.

| Biến | Ý nghĩa |
| --- | --- |
| `DB_URL` | JDBC URL MySQL, bắt buộc; database phải tồn tại |
| `DB_USERNAME` | Tài khoản MySQL, bắt buộc |
| `DB_PASSWORD` | Mật khẩu MySQL, bắt buộc |
| `DB_POOL_SIZE` | Số connection tối đa, mặc định 10 |
| `SERVER_PORT` | Port HTTP, mặc định 8080 |

URL mẫu dùng `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8`.
Không tắt TLS mặc định trong cấu hình chung; TLS/certificate cấu hình theo môi trường.
Spring Boot không tự load file `.env`; đặt biến qua shell, IDE hoặc hệ thống triển khai.

## Quản lý schema

- Hibernate `ddl-auto=validate`: kiểm tra entity khớp schema, không tự tạo/sửa bảng.
- `open-in-view=false`: truy xuất DB trong service/transaction, không lazy load trong lúc serialize response.
- Thời gian JDBC dùng UTC.
- Flyway chạy trước khởi tạo EntityManagerFactory; SQL nằm ở `src/main/resources/db/migration`.
- Dùng `V1__description.sql`, `V2__description.sql`, ... khi có schema thực tế.
- Không sửa migration đã dùng chung; thêm version mới. Flyway validate checksum khi khởi động.
- `baseline-on-migrate=false`: không tự chấp nhận một schema cũ chưa được quản lý.
- `clean-disabled=true`: không cho Flyway clean xóa schema.
- `spring.sql.init.mode=never`: không trộn thêm `schema.sql`/`data.sql` với Flyway.

Ở bước này thư mục migration production chỉ có hướng dẫn; chưa có SQL tạo bảng nghiệp vụ.
Flyway có thể tạo bảng lịch sử của nó khi khởi động trên database rỗng.

## Kiểm thử

`ChatApplicationTests` dùng Testcontainers MySQL 8.4, `@ServiceConnection` cấp connection cho test.
Không fallback sang H2 và không skip âm thầm khi Docker chưa chạy.
Fixture `src/test/resources/db/test-migration/V1__create_persistence_probe.sql`
chỉ tạo bảng kiểm thử, không được đóng gói trong jar.

Kiểm tra migration chạy thành công và không chạy lại, UTF-8/emoji, rollback,
health và endpoint env không expose. Khi có entity sẽ bổ sung test mapping/schema validation.
Đây chưa phải test schema auth/chat vì schema đó chưa được triển khai.

## Tham khảo

- [Spring Boot: database initialization](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)
- [Testcontainers: MySQL](https://java.testcontainers.org/modules/databases/mysql/)
