package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

// Nhận tên, thành viên và ảnh đại diện FE gửi xuống khi tạo nhóm mới.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class CreateGroupRequest {
    @NotBlank(message = "{validation.notBlank}")
    @Size(max = 100, message = "{validation.maxLength}")
    private String name;

    @NotEmpty(message = "{validation.notEmpty}")
    private List<@NotNull(message = "{validation.notNull}") UUID> memberIds;

    private UUID avatarFileId;
}
