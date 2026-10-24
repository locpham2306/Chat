# Hợp đồng API chat

Tóm tắt các quyết định đã thống nhất ngày 20/10/2026.
HTTP-01 đăng ký, HTTP-02 đăng nhập, HTTP-04 đăng xuất,
HTTP-05 xem hồ sơ, HTTP-06 cập nhật hồ sơ và HTTP-07 tìm kiếm user đã được triển khai.
Các API còn lại vẫn là thiết kế.

[Bảng API HTTP, WebSocket, model và mã lỗi](https://docs.google.com/spreadsheets/d/1DTv-iWD6VOK5e8GZwOaJ7J8uR1vmnhGx3EfQp_qGDjk/edit)

Khi thay đổi hợp đồng, cập nhật tài liệu liên quan và thông báo FE.
Nếu docs và sheet khác nhau, làm rõ trước khi triển khai phần có mâu thuẫn.

## Response chung

Các API có body dùng envelope theo base QLCC:

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {}
}
```

Danh sách phân trang dùng `PageResponse<T>` trong `content` với ba trường
`data`, `totalPages`, `totalElements`. Response 204 không có body.
Khi lỗi, `strCode` là một `HttpErrorCode`; `content` chứa
`timestamp`, HTTP `status`, `path` và `fieldErrors`.

## Xác thực

- `POST /api/auth/register`: nhận username/password/fullName, trả User với status
  `'OFFLINE'` và không trả password/hash. Username trùng trả 409.
- `POST /api/auth/login`: nhận username/password, tạo session ngẫu nhiên hạn 365 ngày
  (31.536.000 giây), chỉ lưu hash + userId + expiresAt và đặt cookie `token`.
- Response đăng nhập không chứa token; `content` là
  `{ "message": "Đăng nhập thành công" }` trong `BaseResponse`.
- Cookie session HttpOnly; Secure trên HTTPS; Path=/; SameSite=Lax cho cùng site.
  Cross-site cần SameSite=None; Secure và bảo vệ CSRF phù hợp.
- Không có access token hoặc refresh token. Trình duyệt tự gửi cookie session;
  session hết hạn thì FE chuyển người dùng tới đăng nhập.
- `POST /api/auth/logout`: lấy session token trong cookie hoặc body `{ "token": "..." }`.
  Thiếu cả hai hoặc hai giá trị khác nhau trả 400.
  Thu hồi phiên, xóa cookie; token đã hết hạn/thu hồi vẫn trả 204.
  Phần đóng WS sẽ nối vào logout khi module WebSocket được triển khai.
- `GET /api/users/me`: yêu cầu cookie session hợp lệ, trả User của người đang đăng nhập.
  Thiếu/sai/hết hạn/đã thu hồi session trả 401 với `strCode: 'UNAUTHENTICATED'`.
  Status hiện là `'OFFLINE'` cho tới khi có presence WebSocket.
- `PUT /api/users/me`: yêu cầu cookie session hợp lệ và nhận đầy đủ hai trường
  `{ "fullName": "Nguyễn Văn An", "avatarFileId": null }`.
  `fullName` không được rỗng, tối đa 100 ký tự và được trim trước khi lưu.
  `avatarFileId` phải có trong JSON; giá trị `null` dùng để xóa avatar.
  Khi khác null, file phải tồn tại, do chính user upload, chưa gắn vào hội thoại
  và có MIME `image/*`. File không tồn tại trả 404; file không được phép trả 403.
  Response 200 là User đã cập nhật. Không cho sửa `id`, `username` hoặc `status`.
- `GET /api/users`: yêu cầu cookie session hợp lệ; nhận query `keyword` tùy chọn,
  `page` mặc định 0 và `size` mặc định 20. `page` phải từ 0; `size` từ 1 đến 100.
  Tìm không phân biệt hoa/thường theo username hoặc fullName, không trả chính người
  đang đăng nhập và sắp xếp ổn định theo username rồi id. Query sai trả 400.
  Response 200 là `BaseResponse<PageResponse<List<User>>>`; `content` có `data`,
  `totalPages`, `totalElements`. Status hiện là `'OFFLINE'` tới khi có presence WebSocket.

Ví dụ `GET /api/users?keyword=an&page=0&size=20`:

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "data": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "username": "an",
        "fullName": "Nguyễn Văn An",
        "avatarFileId": null,
        "status": "OFFLINE",
        "lastSeen": null
      }
    ],
    "totalPages": 1,
    "totalElements": 1
  }
}
```

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
