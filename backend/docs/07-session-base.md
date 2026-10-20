# Base session

Tham khảo QLCC: tách bộ sinh token, service quản lý phiên, repository và cookie service.
Chat dùng một session token; thời hạn cố định 365 ngày, không có rotation/access/refresh.
Đây là phần nền tảng nội bộ, chưa có controller login/logout hoặc filter xác thực request.

## Thành phần

- `SessionTokenGenerator`: SecureRandom sinh 32 byte, encode Base64 URL không padding;
  chỉ chấp nhận dạng canonical 43 ký tự. SHA-256 chuyển token thành hash trước khi truy vấn/lưu DB.
- `SessionService` / `SessionServiceImpl`: transaction tạo phiên cho user tồn tại,
  tìm phiên còn hạn/chưa thu hồi và thu hồi idempotent.
- `IssuedSession`: kết quả nội bộ lúc cấp phiên; không dùng làm response DTO.
  rawToken được loại khỏi JSON bằng `@JsonIgnore`, không đưa vào toString.
- `SessionIdentity`: sessionId, userId, expiresAt; không chứa token/hash.
- `SessionCookieService` / `SessionCookieServiceImpl`: ghi/đọc/xóa cookie và chọn token
  logout từ cookie hoặc body.
- `SessionCookieProperties`: cấu hình cookie được kiểm tra khi app khởi động.
- `TimeConfig`: cung cấp Clock UTC, cho phép test cố định thời gian.

DB chỉ lưu hash. Token thô chỉ tồn tại trong bộ nhớ khi cấp phiên và ở cookie trình duyệt.
Đọc phiên không cập nhật expiresAt. Tại đúng expiresAt, phiên không còn hợp lệ.
Thời điểm lưu DB có độ chính xác microsecond tương ứng DATETIME(6).

## Cookie và logout

- Tên `token`, Path `/`, HttpOnly luôn bật, không đặt Domain.
- `SESSION_COOKIE_SECURE` mặc định `true`; local HTTP đặt `false` trong `.env`.
- `SESSION_COOKIE_SAME_SITE`: `Lax` (mặc định), `Strict` hoặc `None`.
  `None` bắt buộc Secure=true; cấu hình không hợp lệ khiến app không khởi động.
- Max-Age tính theo thời gian còn lại của phiên; không đặt lại thành 365 ngày khi đọc phiên.
- Xóa cookie dùng cùng tên/path/thuộc tính và Max-Age=0.
- Logout nhận cookie hoặc body; nếu có cả hai phải trùng. Thiếu, sai định dạng,
  khác nhau hoặc nhiều cookie cùng tên thì lỗi 400.
- Token đúng định dạng nhưng không tồn tại/đã thu hồi/hết hạn vẫn có thể logout idempotent.
  Service kiểm tra phiên trả empty cho token sai định dạng, không truy vấn DB.

## Tích hợp ở bước kế tiếp

1. Auth service kiểm tra username/password trước khi gọi `SessionService.create`.
   Hàm create không tự xác minh mật khẩu và không được expose trực tiếp.
2. Controller ghi cookie sau khi transaction cấp phiên commit thành công;
   JSON chỉ trả thông báo theo hợp đồng, không trả IssuedSession.
3. Logout resolve token, thu hồi trong transaction, sau commit xóa cookie.
4. Bổ sung filter xác thực, response 401/403, CORS và CSRF trước khi expose API auth.
   Cookie HttpOnly/SameSite không thay thế kiểm tra CSRF.
5. Khi có WebSocket, đóng các kết nối của phiên sau khi thu hồi thành công.

Chưa có endpoint auth hoặc WS trong bước này, nên chưa thực hiện đóng WS hoặc áp dụng
phân quyền HTTP. Không đổi schema và không thêm dependency.

## Kiểm thử

Unit test kiểm tra định dạng/hash, hạn cố định, lookup không gia hạn, cấu hình/thuộc tính
cookie, logout thiếu/mâu thuẫn token và không serialize token thô.
Integration test MySQL kiểm tra cấp hai phiên, lưu hash, truy xuất và thu hồi một phiên
không ảnh hưởng phiên còn lại. Test repository kiểm tra hạn đúng tại expiresAt.
