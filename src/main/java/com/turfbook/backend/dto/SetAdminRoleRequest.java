package com.turfbook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Assign an admin sub-role (SUPER_ADMIN | SUPPORT | READ_ONLY) to an ADMIN user. */
@Data
public class SetAdminRoleRequest {

    @NotBlank
    private String adminRole;
}
