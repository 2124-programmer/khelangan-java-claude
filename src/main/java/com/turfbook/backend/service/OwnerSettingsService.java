package com.turfbook.backend.service;

import com.turfbook.backend.dto.OwnerSettingsDto;
import com.turfbook.backend.dto.UpdateOwnerSettingsRequest;

public interface OwnerSettingsService {

    OwnerSettingsDto getSettings(Long ownerId);

    OwnerSettingsDto updateSettings(Long ownerId, UpdateOwnerSettingsRequest request);

    boolean isAutoAccept(Long ownerId);
}
