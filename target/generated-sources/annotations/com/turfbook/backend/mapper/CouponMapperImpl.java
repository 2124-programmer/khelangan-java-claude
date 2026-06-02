package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CouponDto;
import com.turfbook.backend.entity.CouponEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T00:59:15+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
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
