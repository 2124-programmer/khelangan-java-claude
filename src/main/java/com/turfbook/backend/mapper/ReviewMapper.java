package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.entity.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "playerId", source = "player.id")
    ReviewDto toDto(ReviewEntity entity);
}
