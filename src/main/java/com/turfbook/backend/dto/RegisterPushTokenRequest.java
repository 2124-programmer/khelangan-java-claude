package com.turfbook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterPushTokenRequest {

    @NotBlank
    private String token;

    /** "ios" | "android" | "web" — optional. */
    private String platform;
}
