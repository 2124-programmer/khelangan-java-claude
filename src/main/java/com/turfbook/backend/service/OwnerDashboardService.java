package com.turfbook.backend.service;

import com.turfbook.backend.dto.OwnerDashboardSummaryDto;

public interface OwnerDashboardService {

    OwnerDashboardSummaryDto getSummary(Long ownerId);
}
