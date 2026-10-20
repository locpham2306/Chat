# Hợp đồng API chat

Tóm tắt các quyết định đã thống nhất ngày 20/10/2026.
Đây là thiết kế, chưa phải API đã triển khai.

[Bảng API HTTP, WebSocket, model và mã lỗi](https://docs.google.com/spreadsheets/d/1DTv-iWD6VOK5e8GZwOaJ7J8uR1vmnhGx3EfQp_qGDjk/edit)

Khi thay đổi hợp đồng, cập nhật tài liệu liên quan và thông báo FE.
Nếu docs và sheet khác nhau, làm rõ trước khi triển khai phần có mâu thuẫn.

## Xác thực

- `POST /api/auth/login`: nhận username/password, tạo một session hạn 365 ngày
  (31.536.000 giây), đặt cookie `token` bằng `Set-Cookie`.
- Cookie HttpOnly; Secure trên HTTPS; Path=/; SameSite=Lax cho cùng site.
  Cross-site cần SameSite=None; Secure và bảo vệ CSRF phù hợp.
- JSON login chỉ trả thông báo thành công, không trả token.
- Không có access token, refresh token hoặc endpoint refresh.
- Phiên hết hạn trả 401; FE chuyển tới login để người dùng đăng nhập lại,
  không tự xác thực lại bằng token hết hạn.
- `POST /api/auth/logout`: lấy token trong cookie hoặc body `{ "token": "..." }`.
  Thiếu cả hai hoặc hai giá trị khác nhau trả 400.
  Thu hồi phiên, xóa cookie, đóng WS; token đã hết hạn/thu hồi vẫn trả 204.

## Hội thoại DIRECT và tin đầu

1. FE gọi `GET /api/conversations/direct?recipientId=u2`.
2. Đã tồn tại: 200 + Conversation; chưa có: 204 không body.
   API này tuyệt đối không tạo hội thoại.
3. Chưa có hội thoại: gửi `SEND_MESSAGE` với recipientId.
4. Server tìm hoặc tạo DIRECT, thêm thành viên và lưu tin trong cùng transaction.
   Constraint cặp user không phân biệt thứ tự ngăn hai hội thoại khi gửi đồng thời.
5. Sau commit, `MESSAGE_ACK` trả conversationId để FE cập nhật URL.
6. Đã có hội thoại: gửi `SEND_MESSAGE` bằng conversationId.

```json
{
  "event": "SEND_MESSAGE",
  "data": {
    "recipientId": "u2",
    "clientMessageId": "client-001",
    "content": "Xin chào",
    "attachmentIds": []
  }
}
```

Gửi đúng một trong recipientId/conversationId. content hoặc attachmentIds phải có dữ liệu.
ACK luôn có clientMessageId, conversationId, messageId và createdAt.
Retry giữ nguyên clientMessageId và payload; deduplicate theo senderId + clientMessageId.
Không tạo hội thoại từ lookup hoặc TYPING. GROUP vẫn có thể tồn tại khi chưa có tin.

## Enum dùng trên wire

```ts
type ConversationType = 'DIRECT' | 'GROUP';
type UserStatus = 'ONLINE' | 'OFFLINE';
type MemberRole = 'OWNER' | 'MEMBER';
type ClientEvent = 'SEND_MESSAGE' | 'TYPING';
type ServerEvent =
  | 'MESSAGE_ACK' | 'NEW_MESSAGE' | 'TYPING'
  | 'READ_RECEIPT' | 'PRESENCE_CHANGED' | 'ERROR';
type HttpErrorCode =
  | 'VALIDATION_ERROR' | 'UNAUTHENTICATED' | 'FORBIDDEN'
  | 'NOT_FOUND' | 'CONFLICT' | 'PAYLOAD_TOO_LARGE'
  | 'UNSUPPORTED_MEDIA_TYPE' | 'INTERNAL_ERROR';
type WsErrorCode =
  | 'VALIDATION_ERROR' | 'UNAUTHENTICATED' | 'FORBIDDEN'
  | 'NOT_FOUND' | 'CONFLICT' | 'INTERNAL_ERROR';
```

Đây là ký pháp mô tả contract, không quyết định framework FE.
Chuỗi phân biệt hoa/thường. Không tự thêm giá trị khi chưa cập nhật contract.

## WebSocket

Raw WebSocket JSON tại `/ws`, dùng chung cookie session với HTTP; không dùng STOMP.
Kiểm tra phiên và Origin khi handshake; đóng kết nối khi phiên hết hạn/thu hồi.
Không truyền token trong query/frame.
NEW_MESSAGE mang conversationId; nếu FE chưa biết hội thoại, tải chi tiết hoặc danh sách.
Chỉ thành viên hiện tại được truy cập hội thoại.
