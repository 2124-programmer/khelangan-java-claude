package com.turfbook.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create a brand-new ADMIN account (SUPER_ADMIN only). The new admin can sign in immediately
 * with the supplied password. {@code adminRole} is the effective sub-role to grant.
 */
@Data
public class CreateAdminRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    private String adminRole; // SUPER_ADMIN | SUPPORT | READ_ONLY
}
