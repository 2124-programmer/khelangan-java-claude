package com.turfbook.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailChangeCreateRequest {

    @NotBlank
    @Email
    private String newEmail;
}
