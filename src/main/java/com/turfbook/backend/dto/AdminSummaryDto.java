package com.turfbook.backend.dto;

import lombok.Data;

/**
 * One admin in the super-admin "Admin Roles" management list.
 * {@code adminRole} is the EFFECTIVE sub-role (legacy NULL resolves to SUPER_ADMIN);
 * {@code self} marks the currently logged-in admin so the UI can prevent self-demotion.
 */
@Data
public class AdminSummaryDto {
    private Long id;
    private String name;
    private String email;
    private String avatarUrl;
    private String adminRole; // SUPER_ADMIN | SUPPORT | READ_ONLY
    private boolean self;
}
