package com.chat.common;

import com.chat.config.CorsProperties;
import com.chat.common.wrapper.BaseResponse;
import com.chat.utils.PageableUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonBaseTests {

    @Test
    void successResponseUsesQlccCompatibleEnvelope() {
        var response = BaseResponse.success("data");

        assertThat(response.code()).isEqualTo("0");
        assertThat(response.strCode()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("SUCCESSFUL");
        assertThat(response.content()).isEqualTo("data");
    }

    @Test
    void pageableUtilityAppliesDefaultsAndMaximumSize() {
        assertThat(PageableUtils.of(null, null))
                .isEqualTo(PageRequest.of(PageableUtils.DEFAULT_PAGE, PageableUtils.DEFAULT_SIZE));
        assertThat(PageableUtils.of(-1, 1_000))
                .isEqualTo(PageRequest.of(PageableUtils.DEFAULT_PAGE, PageableUtils.MAX_SIZE));
    }

    @Test
    void pageableUtilityMapsPageResponse() {
        var page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 5);

        var response = PageableUtils.toResponse(page, value -> "user-" + value);

        assertThat(response.data()).containsExactly("user-1", "user-2");
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(5);
    }

    @Test
    void corsPropertiesRequireExplicitOriginsWhenUsingCredentials() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*"), 3600))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
