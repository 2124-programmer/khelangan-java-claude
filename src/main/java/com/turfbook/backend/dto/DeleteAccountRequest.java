package com.turfbook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Self-service account deletion. Password re-authentication is required because the action is
 * terminal and frees the account's email/phone for reuse.
 */
@Data
public class DeleteAccountRequest {

    @NotBlank
    private String password;

    /** Optional free-text reason captured for audit. */
    private String reason;
}
