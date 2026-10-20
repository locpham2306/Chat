# Kiến trúc và convention

## Phạm vi

Hiện có `com.chat.ChatApplication`, phần common cho lỗi HTTP/validation,
entity trong modules user/auth/conversation/message/file, repository user/auth.
Service và API nghiệp vụ sẽ được thêm theo từng bước review.
Tham khảo cách tổ chức của QLCC; không mang nghiệp vụ chung cư, Core/CMS,
sync offline hoặc cấu hình nội bộ sang Chat.

## Cấu trúc backend

Dưới root package `com.chat`:

```text
common/
  base/
  config/
  exception/
  dto/
    response/
  constant/
  enums/
modules/
  <feature>/
    controller/
    service/
      impl/
    repository/
    mapper/
    entity/
    dto/
      request/
      response/
integration/
  <technology>/
```

Feature có thể là auth, user, conversation, message, file; chỉ tạo package khi có code.
WebSocket handler đặt trong module sở hữu nghiệp vụ và gọi service chung với HTTP.

## Tên và trách nhiệm

- Package viết thường; class dùng PascalCase; method/field dùng camelCase.
- Suffix: `Controller`, `Service`, `ServiceImpl`, `Repository`, `Queries`,
  `Mapper`, `Entity`, `Request`, `Response` theo vai trò.
- Service có interface; implementation nằm trong `service/impl`.
- Controller/WS handler nhận input, validate và gọi service; không chứa nghiệp vụ hoặc query DB.
- Service xử lý nghiệp vụ, phân quyền và transaction.
- Repository chỉ truy xuất DB. Mapper chuyển Entity/DTO; không query DB hoặc quyết định quyền.
- Dùng constructor injection; tránh field injection.
- Ưu tiên MapStruct khi cần mapping; chỉ tạo `BaseMapper` khi có nhu cầu dùng chung thực tế.
- `common` chỉ chứa thành phần dùng chung; không biến thành nơi chứa mọi nghiệp vụ.
- SDK storage/cache và client ngoài nằm sau adapter trong `integration`.
  Chưa cần Redis/MinIO hoặc công nghệ khác thì không thêm dependency.

## Entity và enum

- Entity khớp migration; kiểu ID khớp schema DB. ID trên API vẫn là string theo hợp đồng.
- Ưu tiên FK ID; dùng quan hệ JPA khi có lý do rõ ràng, tránh serialize entity trực tiếp.
- Không ép entity kế thừa base có audit/sync không phù hợp schema.
- `common.base.BaseEntity` chứa createdAt/updatedAt, dùng `@CreatedDate`,
  `@LastModifiedDate` và `AuditingEntityListener`; bật tại `common.config.JpaAuditingConfig`.
- Entity có audit tương ứng kế thừa BaseEntity.
  SessionEntity giữ createdAt/expiresAt/revokedAt riêng cho vòng đời phiên.
- Dùng Lombok `@Getter` thay getter viết tay. Không dùng `@Data` hoặc setter toàn bộ
  để tránh lộ thông tin trong toString và cho phép sửa ID/audit/token ngoài ý muốn.
- Enum dùng riêng đặt tại module; enum dùng chung đặt tại `common.enums`.
- Tên enum kết thúc bằng `Enum`; constant dùng UPPER_SNAKE_CASE.
- Giá trị serialize ra API phải khớp chính xác chuỗi đã công bố, phân biệt hoa/thường.
- Không lưu ordinal của enum trong DB. Nếu lưu chuỗi thì dùng mapping STRING;
  nếu dùng code riêng phải có converter và migration rõ ràng.
- Không tự đổi sang lowercase hoặc integer chỉ để giống dự án tham khảo.
- Constant lỗi chung đặt tại `common.constant`; lỗi riêng tại `<feature>.constant`.
