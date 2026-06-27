package com.turfbook.backend.service;

import com.turfbook.backend.dto.PlayerSettingsDto;
import com.turfbook.backend.dto.UpdatePlayerSettingsRequest;

public interface PlayerSettingsService {

    PlayerSettingsDto getSettings(Long playerId);

    PlayerSettingsDto updateSettings(Long playerId, UpdatePlayerSettingsRequest request);
}
