package com.turfbook.backend.controller;

import com.turfbook.backend.dto.OwnerDashboardSummaryDto;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerDashboardSummaryDto> getDashboardSummary() {
        UserPrincipal principal = getPrincipal();
        log.info("OwnerDashboardController.getDashboardSummary() - ownerId={}", principal.getId());
        return ResponseEntity.ok(ownerDashboardService.getSummary(principal.getId()));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
