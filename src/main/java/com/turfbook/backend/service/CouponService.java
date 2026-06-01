package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;

import java.util.List;

public interface CouponService {

    List<CouponDto> listActiveCoupons();

    CouponValidationResponse validateCoupon(ValidateCouponRequest request);

    CouponPage adminListCoupons(int page, int size);

    CouponDto createCoupon(CreateCouponRequest request);

    CouponDto updateCoupon(Long id, UpdateCouponRequest request);
}
