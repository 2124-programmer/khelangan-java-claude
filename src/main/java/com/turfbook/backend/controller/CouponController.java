package com.turfbook.backend.controller;

import com.turfbook.backend.api.CouponsApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CouponController implements CouponsApi {

    private final CouponService couponService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CouponDto>> listCoupons() {
        log.info("CouponController.listCoupons() called");
        return ResponseEntity.ok(couponService.listActiveCoupons());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CouponValidationResponse> validateCoupon(ValidateCouponRequest request) {
        log.info("CouponController.validateCoupon() called - code={}", request.getCode());
        return ResponseEntity.ok(couponService.validateCoupon(request));
    }
}
