# Base session

Luồng xác thực tham khảo cách tách lớp của QLCC và được thu gọn cho Chat.
Hệ thống dùng một session token ngẫu nhiên hạn 365 ngày, không có access token,
refresh token hoặc cơ chế rotation. Token thô chỉ nằm trong cookie HttpOnly;
database chỉ lưu SHA-256 hash.

## Luồng xác thực

1. Register chuẩn hóa username/fullName, băm password bằng BCrypt và tạo user.
2. Login kiểm tra username/password, tạo session hạn 365 ngày và đặt cookie `token`.
3. Response đăng nhập chỉ trả thông báo thành công, không trả token trong JSON.
4. Trình duyệt tự gửi cookie trong các request sau. Filter kiểm tra token hash,
   hạn và trạng thái thu hồi rồi đưa danh tính phiên vào request.
5. Logout nhận token từ cookie hoặc body, thu hồi session trong DB và xóa cookie.
6. Session hết hạn thì client chuyển người dùng về màn hình đăng nhập.

## Thành phần

- `SessionTokenGenerator`: sinh token ngẫu nhiên 32 byte dạng Base64 URL và tạo SHA-256 hash.
- `SessionService`: tạo session, tìm session còn hiệu lực và thu hồi session.
- `SessionCookieService`: ghi/đọc/xóa cookie; logout hỗ trợ cookie hoặc body.
- `SessionAuthenticationFilter`: bảo vệ `/api/**`, trừ register/login/logout,
  và trả Error model 401 khi session không hợp lệ.
- `AuthenticationService`: đăng ký user, kiểm tra BCrypt password và tạo session.
- `AuthenticationController`: cung cấp register, login và logout.
- `UserController`/`UserService`: xem và cập nhật hồ sơ tại `GET/PUT /api/users/me`.

Bảng `sessions` lưu `token_hash`, `user_id`, `created_at`, `expires_at`
và `revoked_at`. Không lưu token thô trong database hoặc log.

## Cookie

- Tên cookie: `token`.
- Path `/`, HttpOnly, không đặt Domain.
- `SESSION_COOKIE_SECURE` mặc định `true`; local HTTP đặt `false` trong `.env`.
- `SESSION_COOKIE_SAME_SITE` mặc định `Lax`; `None` bắt buộc Secure=true.
- Max-Age theo thời gian còn lại của session, tối đa 31.536.000 giây.

## Phạm vi hiện tại

Đã có register, login, logout, filter xác thực session và API xem/cập nhật hồ sơ,
tìm kiếm user, upload/download file có phân quyền.
CORS cho HTTP API dùng danh sách origin cụ thể từ cấu hình, cho phép credentials và
chạy trước filter session để xử lý preflight. Raw WebSocket `/ws` dùng cùng cookie, kiểm tra
Origin + session khi handshake, ping heartbeat, đóng theo hạn phiên và đóng ngay khi logout.
CSRF cho các request HTTP thay đổi dữ liệu vẫn là bước tiếp theo.

Test kiểm tra BCrypt, session hạn 365 ngày, hash token trong DB, cookie HttpOnly,
401 cho session thiếu/sai/đã thu hồi, `GET/PUT /api/users/me`, logout idempotent,
phân quyền file avatar và không lộ token trong JSON.
Test WebSocket kiểm tra handshake cookie/Origin và session được dùng để lưu tin trước ACK.
