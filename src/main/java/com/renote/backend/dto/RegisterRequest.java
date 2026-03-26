package com.renote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username不能为空")
    @Size(min = 3, max = 64, message = "username长度为3-64")
    private String username;

    @NotBlank(message = "password不能为空")
    @Size(min = 6, max = 128, message = "password长度为6-128")
    private String password;
}
