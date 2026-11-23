package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Nhận điều kiện tìm kiếm và phân trang khi FE lấy các user có thể thêm vào nhóm.
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AvailableMembersRequest {
    private String keyword;

    @Min(value = 0, message = "{validation.min}")
    private Integer page;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 100, message = "{validation.max}")
    private Integer size;

    public AvailableMembersRequest() {
        this(null, null, null);
    }

    public AvailableMembersRequest(String keyword, Integer page, Integer size) {
        setKeyword(keyword);
        setPage(page);
        setSize(size);
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword;
    }

    public void setPage(Integer page) {
        this.page = page == null ? 0 : page;
    }

    public void setSize(Integer size) {
        this.size = size == null ? 20 : size;
    }
}
