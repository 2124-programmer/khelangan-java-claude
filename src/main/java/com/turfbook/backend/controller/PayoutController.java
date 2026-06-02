package com.turfbook.backend.controller;

import com.turfbook.backend.api.PayoutsApi;
import com.turfbook.backend.dto.PayoutPage;
import com.turfbook.backend.security.UserPrincipal;
import com.turfbook.backend.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PayoutController implements PayoutsApi {

    private final PayoutService payoutService;

    @Override
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PayoutPage> listOwnerPayouts(Integer page, Integer size) {
        UserPrincipal principal = getPrincipal();
        log.info("PayoutController.listOwnerPayouts() called - ownerId={}", principal.getId());
        return ResponseEntity.ok(payoutService.listOwnerPayouts(principal.getId(),
                page != null ? page : 0, size != null ? size : 20));
    }

    private UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
