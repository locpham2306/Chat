# Quy tắc triển khai code

## API và DTO

- Endpoint dùng `/api/...` và đúng GET/POST/PUT/DELETE trong tài liệu Chat.
- Chọn một nơi quản lý prefix `/api`, tránh khai báo lặp trong context-path và controller.
- Input/output dùng DTO; không trả entity, password/hash hoặc session token trong JSON.
- Response thành công trả object/array trực tiếp; 204 không có body.
  Không tự thêm `BaseResponse<T>`, `/api/v1`, Core/CMS hay chuyển mọi API sang POST.
- Validate request bằng Bean Validation; validation nghiệp vụ nằm trong service.
- Dùng PUT theo schema đã công bố, phân biệt field thiếu và null.
- Enum ghi rõ dạng `type: 'DIRECT' | 'GROUP'` trong docs; JSON ví dụ chỉ chứa một giá trị hợp lệ.
- Không nhận senderId từ client để xác định người gửi; lấy user từ session.

## Repository và transaction

- CRUD/query đơn giản: Spring Data repository và derived query.
- JPQL cố định dài: constant trong class `XxxQueries` final, gọi bằng `@Query`.
- Filter động vừa phải: Specification; query động phức tạp: custom QueryRepository.
- Bind tham số, không ghép trực tiếp input vào SQL.
- Có phân trang và giới hạn size cho danh sách; tránh N+1.
- Transaction đặt tại service cho đơn vị nghiệp vụ cần atomic.
- Tạo DIRECT + thành viên + tin đầu trong cùng transaction; lỗi phải rollback toàn bộ.
- Duy nhất cặp người dùng và khóa chống trùng tin phải được bảo đảm bằng constraint DB,
  không chỉ kiểm tra trước bằng query.
- ACK/broadcast chỉ sau commit thành công, không phát sự kiện cho dữ liệu đã rollback.

## Lỗi

- Dùng exception nghiệp vụ có code rõ ràng, xử lý tập trung tại exception handler.
- Không throw `RuntimeException` chung chung cho tình huống nghiệp vụ dự kiến.
- HTTP error theo model Error trong hợp đồng; WS error dùng event `ERROR`.
- Không trả stack trace, SQL, credential hoặc nội dung nội bộ ra client.
- Message có thể quản lý bằng resource bundle; code lỗi là key ổn định để FE xử lý.
- Không tạo enum lỗi chỉ để chứa message; có thể dùng `*ErrorConstants` cho key.

## Session và phân quyền

- Một session token ngẫu nhiên; server lưu hash, userId, expiresAt và trạng thái thu hồi.
- Hạn cố định 365 ngày; cookie `token` do backend đặt, HttpOnly,
  Secure trên HTTPS, Path=/; SameSite theo deployment đã thống nhất.
- Không access/refresh token; không trả token trong JSON hoặc URL.
- Request kiểm tra phiên còn hạn/chưa thu hồi và quyền trên tài nguyên.
- Logout hỗ trợ cookie hoặc body; nếu cả hai khác nhau trả 400.
  Thu hồi server-side, xóa cookie và đóng WS của phiên.
- Không ghi token, password hoặc cookie header vào log.
- CORS credentials chỉ cho origin cụ thể; bảo vệ CSRF cho request thay đổi dữ liệu.
  Handshake WS kiểm tra Origin và session.

## Database, cấu hình và kiểm thử

- Mỗi thay đổi schema phải có migration; không dùng JPA tự sửa schema production.
- Secret lấy từ env hoặc secret store; cấu hình mẫu chỉ có placeholder.
- Không thêm dependency/base abstraction khi chưa có nhu cầu.
- Test nghiệp vụ quan trọng: phân quyền, session hết hạn/thu hồi, logout,
  lookup không tạo hội thoại, tin đầu rollback, gửi đồng thời và retry không tạo trùng.
- Khi đã có build tool, chạy compile/test phù hợp và ghi kết quả thực tế trong PR.
  Hiện chưa dựng base nên chưa có lệnh kiểm thử ứng dụng.
