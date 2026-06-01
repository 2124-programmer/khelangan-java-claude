package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.ReviewEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T00:59:15+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewDto toDto(ReviewEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ReviewDto reviewDto = new ReviewDto();

        reviewDto.setBookingId( entityBookingId( entity ) );
        reviewDto.setVenueId( entityVenueId( entity ) );
        reviewDto.setPlayerId( entityPlayerId( entity ) );
        reviewDto.setId( entity.getId() );
        reviewDto.setPlayerName( entity.getPlayerName() );
        reviewDto.setRating( entity.getRating() );
        reviewDto.setComment( entity.getComment() );
        reviewDto.setCleanliness( entity.getCleanliness() );
        reviewDto.setGround( entity.getGround() );
        reviewDto.setStaff( entity.getStaff() );
        reviewDto.setOwnerReply( entity.getOwnerReply() );
        reviewDto.setCreatedAt( entity.getCreatedAt() );

        return reviewDto;
    }

    private Long entityBookingId(ReviewEntity reviewEntity) {
        BookingEntity booking = reviewEntity.getBooking();
        if ( booking == null ) {
            return null;
        }
        return booking.getId();
    }

    private Long entityVenueId(ReviewEntity reviewEntity) {
        VenueEntity venue = reviewEntity.getVenue();
        if ( venue == null ) {
            return null;
        }
        return venue.getId();
    }

    private Long entityPlayerId(ReviewEntity reviewEntity) {
        UserEntity player = reviewEntity.getPlayer();
        if ( player == null ) {
            return null;
        }
        return player.getId();
    }
}
