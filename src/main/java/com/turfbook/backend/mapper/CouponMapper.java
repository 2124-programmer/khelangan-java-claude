package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CouponDto;
import com.turfbook.backend.entity.CouponEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    @Mapping(target = "discountType", expression = "java(entity.getDiscountType().name())")
    @Mapping(target = "isActive", expression = "java(entity.isActive())")
    CouponDto toDto(CouponEntity entity);
}
