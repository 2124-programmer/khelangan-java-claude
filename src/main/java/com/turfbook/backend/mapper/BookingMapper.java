package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.BookingDto;
import com.turfbook.backend.entity.BookingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface BookingMapper {

    @Mapping(target = "playerId", source = "player.id")
    @Mapping(target = "playerName", source = "player.name")
    @Mapping(target = "playerPhone", source = "player.phone")
    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "venuePhone", source = "venue.contactPhone")
    @Mapping(target = "courtId", source = "court.id")
    @Mapping(target = "courtName", source = "court.name")
    @Mapping(target = "slotId", source = "slot.id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "paymentStatus", expression = "java(entity.getPaymentStatus().name())")
    @Mapping(target = "startTime", expression = "java(entity.getStartTime().toString())")
    @Mapping(target = "endTime", expression = "java(entity.getEndTime().toString())")
    @Mapping(target = "hasReview", expression = "java(entity.isHasReview())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "groupId", source = "entity.groupId")
    @Mapping(target = "cancellationReason", expression = "java(entity.getCancellationReason() != null ? entity.getCancellationReason().name() : null)")
    BookingDto toDto(BookingEntity entity);
}
