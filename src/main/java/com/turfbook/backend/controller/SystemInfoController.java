package com.turfbook.backend.controller;

import com.turfbook.backend.dto.SystemInfoDto;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Super-admin "App Configuration" diagnostics. The hard gate (SUPER_ADMIN only) is enforced in the
 * service; {@code @PreAuthorize} here only ensures the caller is an authenticated admin.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SystemInfoController {

    private final SystemInfoService systemInfoService;

    @GetMapping("/api/v1/admin/system-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemInfoDto> getSystemInfo() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        log.info("SystemInfoController.getSystemInfo() called - actorId={}", principal.getId());
        return ResponseEntity.ok(systemInfoService.getSystemInfo(principal.getId()));
    }
}
