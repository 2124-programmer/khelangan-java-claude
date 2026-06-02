package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.PayoutDto;
import com.turfbook.backend.entity.PayoutEntity;
import com.turfbook.backend.entity.UserEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T00:59:15+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class PayoutMapperImpl implements PayoutMapper {

    @Override
    public PayoutDto toDto(PayoutEntity entity) {
        if ( entity == null ) {
            return null;
        }

        PayoutDto payoutDto = new PayoutDto();

        payoutDto.setOwnerId( entityOwnerId( entity ) );
        payoutDto.setId( entity.getId() );
        payoutDto.setOwnerName( entity.getOwnerName() );
        payoutDto.setAmount( entity.getAmount() );
        payoutDto.setCommissionDeducted( entity.getCommissionDeducted() );
        payoutDto.setNetAmount( entity.getNetAmount() );
        payoutDto.setDate( entity.getDate() );

        payoutDto.setStatus( entity.getStatus().name() );
        payoutDto.setCreatedAt( entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null );

        return payoutDto;
    }

    private Long entityOwnerId(PayoutEntity payoutEntity) {
        UserEntity owner = payoutEntity.getOwner();
        if ( owner == null ) {
            return null;
        }
        return owner.getId();
    }
}
