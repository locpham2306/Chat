# Hợp đồng API chat

Tóm tắt các quyết định đã thống nhất ngày 20/10/2026.
HTTP-01 đăng ký, HTTP-02 đăng nhập, HTTP-04 đăng xuất,
HTTP-05 xem hồ sơ, HTTP-06 cập nhật hồ sơ, HTTP-07 tìm kiếm user, HTTP-08 lookup DIRECT,
HTTP-09 tạo GROUP, HTTP-10 danh sách, HTTP-11 chi tiết, HTTP-12 cập nhật hội thoại,
HTTP-13 danh sách thành viên, HTTP-14 thêm thành viên, HTTP-15 xóa thành viên/rời nhóm,
HTTP-20 tìm ứng viên thêm vào nhóm,
HTTP-18 upload file và HTTP-19 download file đã được triển khai.
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
  Các kết nối WebSocket thuộc đúng session bị thu hồi được đóng ngay; session khác của cùng
  user vẫn hoạt động.
- `GET /api/users/me`: yêu cầu cookie session hợp lệ, trả User của người đang đăng nhập.
  Thiếu/sai/hết hạn/đã thu hồi session trả 401 với `strCode: 'UNAUTHENTICATED'`.
  Status phản ánh presence WebSocket runtime; không có kết nối thì `'OFFLINE'`.
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
  `totalPages`, `totalElements`. Status phản ánh presence WebSocket runtime.

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

## File

- `POST /api/files`: yêu cầu cookie session hợp lệ, nhận `multipart/form-data` với part
  tên `file`, tối đa 20 MiB mặc định và chấp nhận mọi loại file. Content-Type hợp lệ
  được chuẩn hóa và lưu làm metadata; nếu thiếu hoặc không hợp lệ thì lưu
  `application/octet-stream`. Payload được lưu tại storage, database chỉ lưu metadata. Response 201 là
  `BaseResponse<File>`; không trả `storageKey` hoặc đường dẫn filesystem.
- `GET /api/files/{id}`: yêu cầu cookie session hợp lệ và trả binary với
  `Content-Disposition: attachment`, `Cache-Control: private, no-store` và
  `X-Content-Type-Options: nosniff`. Được tải khi user là người upload, file đang là
  avatar user, hoặc user là thành viên hiện tại của hội thoại chứa file/nhóm có avatar đó.
- File không tồn tại trả 404; không có quyền trả 403; file rỗng/tên sai trả 400;
  quá dung lượng trả 413. API upload không kiểm tra whitelist MIME hoặc phần mở rộng.
  Quy tắc dùng file làm avatar vẫn chỉ chấp nhận `image/*`.

Ví dụ response upload:

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "anh-dai-dien.png",
    "contentType": "image/png",
    "size": 102400,
    "downloadUrl": "/api/files/550e8400-e29b-41d4-a716-446655440000"
  }
}
```

## Hội thoại DIRECT và tin đầu

1. FE gọi `GET /api/conversations/direct?recipientId=u2`.
2. API luôn trả 200 nếu recipient tồn tại. `peer` luôn có; `conversation` là thông tin
   DIRECT đã tồn tại hoặc `null`. API này tuyệt đối không tạo hội thoại.
3. Chưa có hội thoại: gửi `SEND_MESSAGE` với recipientId.
4. Server tìm hoặc tạo DIRECT, thêm thành viên và lưu tin trong cùng transaction.
   Constraint cặp user không phân biệt thứ tự ngăn hai hội thoại khi gửi đồng thời.
5. Sau commit, `MESSAGE_ACK` trả conversationId để FE cập nhật URL.
6. Đã có hội thoại: gửi `SEND_MESSAGE` bằng conversationId.

HTTP-08 đã triển khai với `recipientId` là UUID bắt buộc. Thiếu/sai UUID hoặc chọn
chính mình trả 400; recipient không tồn tại trả 404. Response 200 dùng `BaseResponse`;
`content.peer` luôn có để FE hiển thị người đang nhắn, kể cả khi hai người chưa từng
chat. `content.conversation` là `null` khi chưa có DIRECT, ngược lại gồm `id`,
`type: 'DIRECT'` và `createdAt`.

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "conversation": null,
    "peer": {
      "id": "00000000-0000-0000-0000-000000000103",
      "username": "vana",
      "fullName": "Nguyễn Văn A",
      "avatarFileId": null,
      "status": "OFFLINE"
    }
  }
}
```

HTTP-09 đã triển khai tại `POST /api/conversations/group`. `name` bắt buộc, tối đa
100 ký tự; `memberIds` phải chứa ít nhất một user khác người tạo. ID trùng và ID của
người tạo được loại khỏi danh sách trước khi lưu; nếu còn ID không tồn tại thì toàn bộ
request trả 404 và không tạo group. Người tạo được gán `role: 'OWNER'`, các user còn
lại có `role: 'MEMBER'`. Avatar tùy chọn phải tồn tại, là `image/*`, do người tạo upload
và chưa gắn vào hội thoại; sai quyền trả 403. Conversation và membership được tạo trong
cùng transaction. Response 201 có `type: 'GROUP'` và `memberCount` đã gồm OWNER.

HTTP-10 đã triển khai tại `GET /api/conversations`. `page` mặc định 0 và phải từ 0;
`size` mặc định 20 và phải từ 1 đến 100. Chỉ trả hội thoại mà user có membership đang
hoạt động, sắp xếp theo hoạt động mới nhất rồi theo id để ổn định. Response 200 là
`BaseResponse<PageResponse<List<Conversation>>>`; dữ liệu nằm trong `content.data`,
kèm `totalPages` và `totalElements`.

Mỗi phần tử có `id`, `type: 'DIRECT' | 'GROUP'`, `name`, `avatarFileId`, `ownerId`,
`memberCount`, `createdAt`, `peer`, `lastMessage` và `unreadCount`. Với DIRECT, `peer`
là người còn lại trong hội thoại; với GROUP, `peer: null`. Peer gồm `id`, `username`,
`fullName`, `avatarFileId`, `status`. GROUP chưa có tin vẫn xuất hiện với
`lastMessage: null`. Nếu có tin cuối, `lastMessage` gồm `id`, `conversationId`,
`sender`, `clientMessageId`, `content`, `attachments`, `createdAt`; `sender` gồm `id` và
`fullName` để FE hiển thị người gửi tin cuối, không trả riêng `senderId`. Mỗi attachment
dùng model File của API upload/download. `unreadCount` được tính sau mốc đọc theo cặp
`createdAt`/`id`; chưa có mốc đọc thì đếm toàn bộ tin trong hội thoại. Query phân trang
không hợp lệ trả 400, thiếu hoặc sai session trả 401.

```json
{
  "lastMessage": {
    "id": "00000000-0000-0000-0000-000000000404",
    "conversationId": "00000000-0000-0000-0000-000000000202",
    "sender": {
      "id": "00000000-0000-0000-0000-000000000103",
      "fullName": "Bùi Huy Hoàng"
    },
    "clientMessageId": "seed-group-002",
    "content": "Đã xem, response ổn rồi",
    "attachments": [],
    "createdAt": "2026-10-26T03:54:26.449720Z"
  }
}
```

HTTP-11 đã triển khai tại `GET /api/conversations/{id}`. `id` phải là UUID hợp lệ;
hội thoại không tồn tại trả 404. Chỉ membership có `leftAt: null` được xem; người ngoài
hoặc thành viên đã rời trả 403. Response 200 gồm `id`, `type: 'DIRECT' | 'GROUP'`,
`name`, `avatarFileId`, `ownerId`, `memberCount`, `createdAt`, `peer`; `memberCount` chỉ
đếm thành viên đang hoạt động. Với DIRECT, `peer` là người còn lại và các trường riêng
của GROUP là null. Với GROUP, `peer: null`. Thiếu hoặc sai session trả 401.

HTTP-12 đã triển khai tại `PUT /api/conversations/{id}`. Body phải có đủ `name` và
`avatarFileId`; `name` bắt buộc, được trim và tối đa 100 ký tự; `avatarFileId: null`
dùng để xóa avatar. Chỉ OWNER có membership đang hoạt động được sửa GROUP. Hội thoại
không tồn tại trả 404; DIRECT trả 409; người không phải OWNER trả 403.

Avatar khác null phải tồn tại, là `image/*`, do OWNER upload và chưa gắn vào hội thoại;
file không tồn tại trả 404, file sai quyền hoặc sai loại trả 403. Việc kiểm tra quyền,
file và cập nhật chạy trong cùng transaction. Response 200 có `type: 'GROUP'` cùng
`id`, `name`, `avatarFileId`, `ownerId`, `memberCount`, `createdAt`.

HTTP-13 đã triển khai tại `GET /api/conversations/{id}/members`. `page` mặc định 0 và
phải từ 0; `size` mặc định 20 và phải từ 1 đến 100. Hội thoại không tồn tại trả 404;
người gọi không có membership đang hoạt động trả 403. Chỉ trả thành viên có
`leftAt: null`, sắp xếp ổn định theo `joinedAt` rồi `id`.

Response 200 là `BaseResponse<PageResponse<List<Member>>>`; `content` có `data`,
`totalPages`, `totalElements`. Mỗi phần tử gồm public `user` với
`status: 'ONLINE' | 'OFFLINE'`, `role: 'OWNER' | 'MEMBER'` và `joinedAt`.
Status phản ánh presence WebSocket runtime. Query phân
trang sai trả 400; thiếu hoặc sai session trả 401.

HTTP-14 đã triển khai tại `POST /api/conversations/{id}/members`. Body có `memberIds`
là mảng UUID bắt buộc và không rỗng. Chỉ OWNER có membership đang hoạt động của GROUP
được thêm thành viên; thành viên mới luôn có `role: 'MEMBER'`. ID trùng trong request
được loại theo thứ tự xuất hiện. User đang là thành viên được bỏ qua và trả trong
`existingUserIds`; user từng rời nhóm được kích hoạt lại membership cũ với `joinedAt`
mới và trả trong `addedUserIds`.

Nếu có bất kỳ user ID nào không tồn tại thì trả 404 và rollback toàn bộ request. Hội
thoại không tồn tại trả 404; DIRECT trả 409; người không phải OWNER trả 403; body sai
trả 400. Response 200 dùng `BaseResponse<AddConversationMembersResponse>`. Thao tác
khóa dòng hội thoại trong transaction để hai request thêm thành viên đồng thời không
tạo membership trùng.

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "addedUserIds": [
      "550e8400-e29b-41d4-a716-446655440004"
    ],
    "existingUserIds": [
      "550e8400-e29b-41d4-a716-446655440005"
    ]
  }
}
```

HTTP-15 đã triển khai tại `DELETE /api/conversations/{id}/members/{userId}` và trả
`204 No Content`, không có response body. API chỉ áp dụng cho GROUP và khóa dòng hội
thoại trong transaction để tuần tự hóa với thao tác thêm thành viên.

MEMBER chỉ được truyền chính `userId` của mình để rời nhóm. OWNER có membership đang
hoạt động được xóa MEMBER khác; OWNER không thể rời hoặc bị xóa và trường hợp này trả
409. Người gọi không có membership đang hoạt động hoặc MEMBER cố xóa người khác trả
403. Hội thoại không tồn tại trả 404; DIRECT trả 409; UUID sai trả 400.

Khi OWNER xóa một user đã rời hoặc chưa từng tham gia nhóm, API vẫn trả 204 và không
thay đổi dữ liệu. Điều này giúp thao tác xóa có tính idempotent khi người gọi vẫn còn
quyền. MEMBER đã rời không còn quyền gọi lại nên lần gọi tiếp theo trả 403. Membership
không bị xóa vật lý; backend đặt `leftAt` theo thời gian hiện tại để HTTP-14 có thể kích
hoạt lại đúng dòng membership cũ.

HTTP-20 đã triển khai tại `GET /api/conversations/{id}/available-members` để FE lấy
danh sách user có thể thêm vào một GROUP đã tồn tại. Chỉ OWNER có membership đang hoạt
động được gọi API. `keyword` tùy chọn, tìm không phân biệt hoa thường theo `username`
hoặc `fullName`; `page` mặc định 0 và `size` mặc định 20, tối đa 100. Kết quả sắp xếp
theo `username` rồi `id` và dùng `BaseResponse<PageResponse<List<User>>>`.

API loại người gọi và tất cả user có membership đang hoạt động (`leftAt: null`) trong
nhóm. User từng rời nhóm (`leftAt` khác null) được trả lại để OWNER có thể thêm lại.
Hội thoại không tồn tại trả 404; DIRECT trả 409; người không phải OWNER trả 403; query
phân trang sai trả 400. Status phản ánh presence WebSocket runtime.

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "data": [
      {
        "id": "00000000-0000-0000-0000-000000000104",
        "username": "linh",
        "fullName": "Nguyễn Mỹ Linh",
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

Khi đang tạo GROUP và chưa có conversation id, FE tiếp tục dùng `GET /api/users` rồi
loại các user đã chọn ở state phía client. Sau khi GROUP đã được tạo, màn hình thêm
thành viên dùng HTTP-20 để server loại thành viên hiện tại.

HTTP-16 đã triển khai tại `GET /api/conversations/{id}/messages`. Query `before` là
message UUID tùy chọn và `limit` mặc định 30, hợp lệ từ 1 đến 100. Kết quả sắp xếp từ
mới đến cũ theo `createdAt`, sau đó `id`; cursor là mốc loại trừ và phải thuộc đúng hội
thoại. Backend lấy thêm một dòng để tính `hasMore`, nên khi hết dữ liệu `nextCursor` là
`null` và `hasMore` là `false`. Hội thoại không tồn tại trả 404, người không còn là thành
viên trả 403, cursor/limit sai trả 400.

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "data": [
      {
        "id": "770e8400-e29b-41d4-a716-446655440001",
        "conversationId": "660e8400-e29b-41d4-a716-446655440001",
        "sender": {
          "id": "550e8400-e29b-41d4-a716-446655440001",
          "fullName": "Nguyễn Văn An"
        },
        "clientMessageId": "client-001",
        "content": "Xin chào",
        "attachments": [],
        "createdAt": "2026-10-26T08:05:00Z"
      }
    ],
    "nextCursor": "770e8400-e29b-41d4-a716-446655440001",
    "hasMore": true
  }
}
```

HTTP-17 đã triển khai tại `PUT /api/conversations/{id}/read` với body bắt buộc
`lastReadMessageId`. Tin phải tồn tại và thuộc đúng hội thoại. Backend khóa membership
của người gọi và chỉ cho mốc tiến theo `createdAt`, sau đó `id`; request cũ không làm
lùi mốc. Response luôn trả mốc thực tế cùng unreadCount hiện tại. Sau khi transaction
commit thành công, server phát READ_RECEIPT với đúng mốc thực tế cho các thành viên đang
hoạt động. Hội thoại/tin không tồn tại trả 404, người ngoài trả 403, tin thuộc hội thoại
khác hoặc body sai trả 400.

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "conversationId": "660e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "lastReadMessageId": "770e8400-e29b-41d4-a716-446655440001",
    "unreadCount": 0
  }
}
```

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

Đã triển khai SEND_MESSAGE, MESSAGE_ACK, NEW_MESSAGE, TYPING, PRESENCE_CHANGED và ERROR.
Tin đầu DIRECT khóa cặp user theo thứ tự ổn định, tạo conversation + membership + message
trong cùng transaction; retry cùng senderId/clientMessageId và payload trả ACK cũ, payload
khác trả CONFLICT. File chỉ được gắn bởi uploader và không chuyển sang hội thoại khác.
ACK/broadcast chỉ chạy sau khi transaction lưu tin thành công. Presence chỉ chuyển OFFLINE
khi user không còn kết nối; server heartbeat bằng ping/pong. READ_RECEIPT được phát sau
khi HTTP-17 commit mốc đọc và luôn phản ánh mốc thực tế không bị lùi.
