package com.turfbook.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerSettingsDto {

    private Boolean pushNotificationsEnabled;
    private Boolean emailNotificationsEnabled;
}
