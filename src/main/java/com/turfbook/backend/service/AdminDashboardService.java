package com.turfbook.backend.service;

import com.turfbook.backend.dto.DashboardPeriod;
import com.turfbook.backend.dto.DashboardSummary;

public interface AdminDashboardService {

    /** Single aggregated payload powering the admin home dashboard. */
    DashboardSummary getSummary(DashboardPeriod period);
}
