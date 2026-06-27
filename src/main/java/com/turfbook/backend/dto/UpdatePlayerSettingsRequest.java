package com.turfbook.backend.dto;

import lombok.Data;

@Data
public class UpdatePlayerSettingsRequest {

    /** Null ⇒ leave unchanged (partial update). */
    private Boolean pushNotificationsEnabled;
    private Boolean emailNotificationsEnabled;
}
