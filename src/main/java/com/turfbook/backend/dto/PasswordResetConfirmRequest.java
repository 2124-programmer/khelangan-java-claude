package com.turfbook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}
