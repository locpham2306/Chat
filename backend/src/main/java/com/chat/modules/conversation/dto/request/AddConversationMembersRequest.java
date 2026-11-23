package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

// Nhận danh sách user FE muốn thêm vào một cuộc hội thoại nhóm.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AddConversationMembersRequest {
    @NotEmpty(message = "{validation.notEmpty}")
    private List<@NotNull(message = "{validation.notNull}") UUID> memberIds;
}
