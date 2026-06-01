package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.DisputeDto;
import com.turfbook.backend.entity.DisputeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "playerId", source = "player.id")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    DisputeDto toDto(DisputeEntity entity);
}
