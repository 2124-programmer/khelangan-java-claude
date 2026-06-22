package com.turfbook.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyResponse {

    /** Short-lived, single-use token for setting the new password. */
    private String resetToken;
}
