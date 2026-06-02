package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T13:20:38+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class BookingMapperImpl implements BookingMapper {

    @Override
    public BookingDto toDto(BookingEntity entity) {
        if ( entity == null ) {
            return null;
        }

        BookingDto bookingDto = new BookingDto();

        bookingDto.setPlayerId( entityPlayerId( entity ) );
        bookingDto.setPlayerName( entityPlayerName( entity ) );
        bookingDto.setVenueId( entityVenueId( entity ) );
        bookingDto.setVenueName( entityVenueName( entity ) );
        bookingDto.setCourtId( entityCourtId( entity ) );
        bookingDto.setCourtName( entityCourtName( entity ) );
        bookingDto.setSlotId( entitySlotId( entity ) );
        bookingDto.setId( entity.getId() );
        bookingDto.setSport( entity.getSport() );
        bookingDto.setDate( entity.getDate() );
        bookingDto.setAmount( entity.getAmount() );
        bookingDto.setConvenienceFee( entity.getConvenienceFee() );
        bookingDto.setDiscount( entity.getDiscount() );
        bookingDto.setCommission( entity.getCommission() );
        bookingDto.setCouponCode( entity.getCouponCode() );

        bookingDto.setStatus( entity.getStatus().name() );
        bookingDto.setPaymentStatus( entity.getPaymentStatus().name() );
        bookingDto.setStartTime( entity.getStartTime().toString() );
        bookingDto.setEndTime( entity.getEndTime().toString() );
        bookingDto.setHasReview( entity.isHasReview() );
        bookingDto.setCreatedAt( entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null );

        return bookingDto;
    }

    private Long entityPlayerId(BookingEntity bookingEntity) {
        UserEntity player = bookingEntity.getPlayer();
        if ( player == null ) {
            return null;
        }
        return player.getId();
    }

    private String entityPlayerName(BookingEntity bookingEntity) {
        UserEntity player = bookingEntity.getPlayer();
        if ( player == null ) {
            return null;
        }
        return player.getName();
    }

    private Long entityVenueId(BookingEntity bookingEntity) {
        VenueEntity venue = bookingEntity.getVenue();
        if ( venue == null ) {
            return null;
        }
        return venue.getId();
    }

    private String entityVenueName(BookingEntity bookingEntity) {
        VenueEntity venue = bookingEntity.getVenue();
        if ( venue == null ) {
            return null;
        }
        return venue.getName();
    }

    private Long entityCourtId(BookingEntity bookingEntity) {
        CourtEntity court = bookingEntity.getCourt();
        if ( court == null ) {
            return null;
        }
        return court.getId();
    }

    private String entityCourtName(BookingEntity bookingEntity) {
        CourtEntity court = bookingEntity.getCourt();
        if ( court == null ) {
            return null;
        }
        return court.getName();
    }

    private Long entitySlotId(BookingEntity bookingEntity) {
        SlotEntity slot = bookingEntity.getSlot();
        if ( slot == null ) {
            return null;
        }
        return slot.getId();
    }
}
