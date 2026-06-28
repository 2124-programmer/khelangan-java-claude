package com.turfbook.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Promote an existing (non-admin) user to ADMIN by email (SUPER_ADMIN only).
 * {@code adminRole} is the effective sub-role to grant.
 */
@Data
public class PromoteAdminRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String adminRole; // SUPER_ADMIN | SUPPORT | READ_ONLY
}
