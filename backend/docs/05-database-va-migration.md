# Database và migration

## Phạm vi hiện tại

- MySQL 8.4, Spring Data JPA, HikariCP, Flyway.
- Dependency version do Spring Boot BOM quản lý.
- V1 thêm users, V2 thêm sessions; service/API auth và session đã được triển khai.
- V3 thêm conversations, conversation_members, messages, files, message_attachments
  và FK avatar. Xem [schema đầy đủ](06-database-schema.md).
- V4 chuẩn hóa 3NF: tách read state, bỏ conversation_id lặp ở attachment;
  chuyển dữ liệu cũ và giữ kiểm tra cùng hội thoại bằng trigger.
- `common.base.BaseEntity` gom createdAt/updatedAt, tự ghi qua JPA Auditing.
  Các entity user/conversation/member/message/file/attachment kế thừa base;
  SessionEntity giữ các mốc thời gian vòng đời phiên riêng.
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
V4 tạo trigger. Khi server bật binary logging, chạy migration bằng tài khoản quản trị
có quyền phù hợp hoặc để DBA cấu hình `log_bin_trust_function_creators` theo chính sách
môi trường. Không cấp SUPER cho tài khoản runtime. Container test bật tùy chọn này
riêng trong container; migration không thay đổi cấu hình global của MySQL local.

| Biến | Ý nghĩa |
| --- | --- |
| `DB_URL` | JDBC URL MySQL, bắt buộc; database phải tồn tại |
| `DB_USERNAME` | Tài khoản MySQL, bắt buộc |
| `DB_PASSWORD` | Mật khẩu MySQL, bắt buộc |
| `DB_POOL_SIZE` | Số connection tối đa, mặc định 10 |
| `SERVER_PORT` | Port HTTP, mặc định 8086 |
| `FILE_STORAGE_ROOT` | Thư mục lưu payload file, mặc định `./uploads` |
| `FILE_MAX_SIZE` | Dung lượng tối đa mỗi file, mặc định `20MB` |
| `FILE_MAX_REQUEST_SIZE` | Dung lượng tối đa multipart request, mặc định `21MB` |

URL mẫu dùng `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8`.
Không tắt TLS mặc định trong cấu hình chung; TLS/certificate cấu hình theo môi trường.
App đã khai báo import `.env` tại thư mục làm việc hoặc `backend/.env`.
Chạy từ `Chat/` hoặc `Chat/backend/`; file dùng cú pháp properties `KEY=value`,
không thêm quote/export. Biến môi trường vẫn ghi đè giá trị trong file.

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

Dữ liệu giả local nằm riêng tại `scripts/seed-dev.sql`, không phải migration và không
được đóng gói để tự chạy cùng ứng dụng. Chỉ chạy thủ công sau khi Flyway đã tạo schema;
xem lệnh và tài khoản mẫu trong `backend/README.md`.

Khi chạy app trên database Chat, Flyway tạo users/sessions và bảng lịch sử migration.
Kiểm thử dùng MySQL 8.4 trong Docker; máy local hiện là MySQL 8.0.37.
Chỉ chạy migration local vào schema `chat` khi được yêu cầu.

## Schema user/session

### users — V1

- `id`: UUID do Hibernate sinh, DB lưu BINARY(16); API serialize thành UUID string.
- `username`: tối đa 50 ký tự, unique tại DB. Collation `utf8mb4_0900_as_ci`
  không phân biệt hoa/thường nhưng phân biệt dấu; `An` và `an` là cùng username.
  Quy tắc ký tự/trim được validate tại API đăng ký, không silently normalize ở entity.
- `password_hash`: tối đa 255 ký tự; chỉ nhận mật khẩu đã được BCrypt encode bởi auth service.
  Không lưu mật khẩu thật.
- `full_name`: tối đa 100 ký tự, hỗ trợ tiếng Việt/emoji.
- `avatar_file_id`, `last_seen`: nullable. V3 bổ sung FK avatar tới files.
- `created_at`, `updated_at`: JPA Auditing ghi thời gian UTC qua BaseEntity.
  Không nhận từ API; không có public setter cho các trường audit.
- Không lưu ONLINE/OFFLINE cố định; presence sẽ tính theo kết nối WS.

### sessions — V2

- `id`: UUID/BINARY(16); `user_id`: FK tới users, không cascade delete.
- `token_hash`: SHA-256 lowercase hex 64 ký tự, unique, so sánh binary.
  Không có cột chứa token thô; auth service sinh token và tính digest.
- `created_at`, `expires_at`, `revoked_at`: UTC, DATETIME(6); revoked_at nullable.
- DB check `expires_at > created_at`; index user_id và expires_at.
- Thời hạn 365 ngày được auth service tính khi cấp phiên, không tự gia hạn trong repository.
- Lookup phiên hợp lệ: đúng hash, revoked_at null, expires_at > thời điểm hiện tại.
  Tại đúng expires_at phiên đã hết hạn.
- Thu hồi dùng một UPDATE có điều kiện revoked_at null; gọi lần nữa không đổi thời điểm thu hồi.
- Dùng FK ID thuần, không serialize entity trực tiếp thành response.

## Kiểm thử

`ChatApplicationTests` dùng Testcontainers MySQL 8.4, `@ServiceConnection` cấp connection cho test.
Không fallback sang H2 và không skip âm thầm khi Docker chưa chạy.
Fixture `src/test/resources/db/test-migration/V9000__create_persistence_probe.sql`
chỉ tạo bảng kiểm thử, không được đóng gói trong jar.
Version fixture được chuyển sang 9000 để không trùng V1 production;
chỉ áp dụng trên container test mới, không sửa lịch sử migration của database dùng chung.

Kiểm tra migration chạy thành công và không chạy lại, UTF-8/emoji, rollback,
health và endpoint env không expose. Có test mapping UUID/audit, unique username/hash,
FK session, expiry boundary, revocation idempotent và Hibernate validate schema.
Đã kiểm thử luồng register/login/logout, cookie session, hồ sơ, tìm kiếm user và
upload/download file có phân quyền.
`ChatSchemaMigrationTests` nâng cấp V3 có dữ liệu lên V4, kiểm tra giữ mốc đọc/audit,
liên kết tin/file và chặn cập nhật sai hội thoại cả ở dòng con lẫn dòng cha.

## Tham khảo

- [Spring Boot: database initialization](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)
- [Testcontainers: MySQL](https://java.testcontainers.org/modules/databases/mysql/)
