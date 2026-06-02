package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.CouponEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.mapper.CouponMapper;
import com.turfbook.backend.repository.CouponRepository;
import com.turfbook.backend.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> listActiveCoupons() {
        log.info("CouponService.listActiveCoupons() called");
        return couponRepository.findActiveCoupons(LocalDate.now())
                .stream().map(couponMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(ValidateCouponRequest request) {
        log.info("CouponService.validateCoupon() called - code={}", request.getCode());
        CouponValidationResponse response = new CouponValidationResponse();

        var couponOpt = couponRepository.findByCode(request.getCode().trim().toUpperCase());
        if (couponOpt.isEmpty()) {
            response.setValid(false);
            response.setDiscount(0);
            response.setMessage("Invalid coupon code");
            return response;
        }

        CouponEntity coupon = couponOpt.get();

        if (!coupon.isActive()) {
            response.setValid(false);
            response.setDiscount(0);
            response.setMessage("Coupon is inactive");
            return response;
        }
        if (coupon.getValidUntil().isBefore(LocalDate.now())) {
            response.setValid(false);
            response.setDiscount(0);
            response.setMessage("Coupon has expired");
            return response;
        }
        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            response.setValid(false);
            response.setDiscount(0);
            response.setMessage("Coupon usage limit reached");
            return response;
        }
        if (request.getBookingAmount() < coupon.getMinBooking()) {
            response.setValid(false);
            response.setDiscount(0);
            response.setMessage(String.format("Minimum booking amount of ₹%d required", coupon.getMinBooking()));
            return response;
        }

        int discount;
        if (coupon.getDiscountType() == CouponEntity.DiscountType.PERCENT) {
            discount = (request.getBookingAmount() * coupon.getDiscountValue()) / 100;
            if (coupon.getMaxDiscount() != null && discount > coupon.getMaxDiscount()) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
            if (discount > request.getBookingAmount()) {
                discount = request.getBookingAmount();
            }
        }

        response.setValid(true);
        response.setDiscount(discount);
        response.setMessage("Coupon applied successfully! You save ₹" + discount);
        log.info("CouponService.validateCoupon() completed - code={}, valid=true, discount={}", request.getCode(), discount);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponPage adminListCoupons(int page, int size) {
        log.info("CouponService.adminListCoupons() called - page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<CouponEntity> entityPage = couponRepository.findAll(pageable);
        CouponPage dto = new CouponPage();
        dto.setContent(entityPage.getContent().stream().map(couponMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }

    @Override
    @Transactional
    public CouponDto createCoupon(CreateCouponRequest request) {
        String code = request.getCode().trim().toUpperCase();
        log.info("CouponService.createCoupon() called - code={}", code);
        if (couponRepository.existsByCode(code)) {
            throw new ConflictException("Coupon code already exists: " + code);
        }

        CouponEntity.DiscountType discountType;
        try {
            discountType = CouponEntity.DiscountType.valueOf(request.getDiscountType().toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid discount type: " + request.getDiscountType());
        }

        CouponEntity coupon = CouponEntity.builder()
                .code(code)
                .discountType(discountType)
                .discountValue(request.getDiscountValue())
                .minBooking(request.getMinBooking() != null ? request.getMinBooking() : 0)
                .maxDiscount(request.getMaxDiscount())
                .validUntil(request.getValidUntil())
                .maxUses(request.getMaxUses() != null ? request.getMaxUses() : 100)
                .isActive(true)
                .build();

        CouponDto result = couponMapper.toDto(couponRepository.save(coupon));
        log.info("CouponService.createCoupon() completed - id={}, code={}", result.getId(), code);
        return result;
    }

    @Override
    @Transactional
    public CouponDto updateCoupon(Long id, UpdateCouponRequest request) {
        log.info("CouponService.updateCoupon() called - id={}", id);
        CouponEntity coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        if (request.getIsActive() != null) {
            coupon.setActive(request.getIsActive());
        }
        return couponMapper.toDto(couponRepository.save(coupon));
    }
}
