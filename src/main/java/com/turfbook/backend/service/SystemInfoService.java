package com.turfbook.backend.service;

import com.turfbook.backend.dto.SystemInfoDto;

public interface SystemInfoService {

    /** Build the non-sensitive system/config snapshot. SUPER_ADMIN only (enforced in impl). */
    SystemInfoDto getSystemInfo(Long actorId);
}
