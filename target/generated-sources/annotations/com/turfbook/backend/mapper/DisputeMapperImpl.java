package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.DisputeDto;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.DisputeEntity;
import com.turfbook.backend.entity.UserEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T13:20:38+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class DisputeMapperImpl implements DisputeMapper {

    @Override
    public DisputeDto toDto(DisputeEntity entity) {
        if ( entity == null ) {
            return null;
        }

        DisputeDto disputeDto = new DisputeDto();

        disputeDto.setBookingId( entityBookingId( entity ) );
        disputeDto.setPlayerId( entityPlayerId( entity ) );
        disputeDto.setOwnerId( entityOwnerId( entity ) );
        disputeDto.setId( entity.getId() );
        disputeDto.setPlayerName( entity.getPlayerName() );
        disputeDto.setOwnerName( entity.getOwnerName() );
        disputeDto.setVenueName( entity.getVenueName() );
        disputeDto.setIssue( entity.getIssue() );
        disputeDto.setResolvedNote( entity.getResolvedNote() );
        disputeDto.setCreatedAt( entity.getCreatedAt() );

        disputeDto.setStatus( entity.getStatus().name() );

        return disputeDto;
    }

    private Long entityBookingId(DisputeEntity disputeEntity) {
        BookingEntity booking = disputeEntity.getBooking();
        if ( booking == null ) {
            return null;
        }
        return booking.getId();
    }

    private Long entityPlayerId(DisputeEntity disputeEntity) {
        UserEntity player = disputeEntity.getPlayer();
        if ( player == null ) {
            return null;
        }
        return player.getId();
    }

    private Long entityOwnerId(DisputeEntity disputeEntity) {
        UserEntity owner = disputeEntity.getOwner();
        if ( owner == null ) {
            return null;
        }
        return owner.getId();
    }
}
