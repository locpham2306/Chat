package com.chat.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Nhận thông tin tài khoản FE gửi xuống khi đăng ký user mới.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RegisterRequest {
    @NotBlank(message = "{validation.notBlank}")
    @Size(max = 50, message = "{validation.maxLength}")
    private String username;

    @NotBlank(message = "{validation.notBlank}")
    private String password;

    @NotBlank(message = "{validation.notBlank}")
    @Size(max = 100, message = "{validation.maxLength}")
    private String fullName;

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=<redacted>, fullName=" + fullName + "]";
    }
}
