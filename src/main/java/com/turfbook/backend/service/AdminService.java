package com.turfbook.backend.service;

import com.turfbook.backend.dto.AdminStatsDto;
import com.turfbook.backend.dto.PlatformSettingsDto;
import com.turfbook.backend.dto.UpdateSettingsRequest;

public interface AdminService {

    AdminStatsDto getAdminStats();

    PlatformSettingsDto getSettings();

    PlatformSettingsDto updateSettings(UpdateSettingsRequest request);
}
