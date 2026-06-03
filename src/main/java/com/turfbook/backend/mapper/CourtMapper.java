package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.entity.CourtEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourtMapper {

    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "sportId", source = "sport.id")
    @Mapping(target = "effectivePricePerHour", expression = "java(entity.effectivePricePerHour())")
    @Mapping(target = "effectiveOpenTime",     expression = "java(entity.effectiveOpenTime())")
    @Mapping(target = "effectiveCloseTime",    expression = "java(entity.effectiveCloseTime())")
    CourtDto toDto(CourtEntity entity);
}
