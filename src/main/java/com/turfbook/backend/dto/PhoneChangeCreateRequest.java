package com.turfbook.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneChangeCreateRequest {

    /** 10–15 digit phone number, optional leading '+'. */
    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter a valid phone number.")
    private String newPhone;
}
