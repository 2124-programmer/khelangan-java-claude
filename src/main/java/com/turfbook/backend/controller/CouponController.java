package com.turfbook.backend.controller;

import com.turfbook.backend.api.CouponsApi;
import com.turfbook.backend.dto.*;
import com.turfbook.backend.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponsApi {

    private final CouponService couponService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CouponDto>> listCoupons() {
        return ResponseEntity.ok(couponService.listActiveCoupons());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CouponValidationResponse> validateCoupon(ValidateCouponRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(request));
    }
}
