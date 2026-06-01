package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CouponDto;
import com.turfbook.backend.entity.CouponEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-01T23:04:16+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CouponMapperImpl implements CouponMapper {

    @Override
    public CouponDto toDto(CouponEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CouponDto couponDto = new CouponDto();

        couponDto.setId( entity.getId() );
        couponDto.setCode( entity.getCode() );
        couponDto.setDiscountValue( entity.getDiscountValue() );
        couponDto.setMinBooking( entity.getMinBooking() );
        couponDto.setMaxDiscount( entity.getMaxDiscount() );
        couponDto.setValidUntil( entity.getValidUntil() );
        couponDto.setUsedCount( entity.getUsedCount() );
        couponDto.setMaxUses( entity.getMaxUses() );

        couponDto.setDiscountType( entity.getDiscountType().name() );
        couponDto.setIsActive( entity.isActive() );

        return couponDto;
    }
}
