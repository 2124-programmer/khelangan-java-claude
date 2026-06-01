package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.entity.CourtEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourtMapper {

    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "sportId", source = "sport.id")
    CourtDto toDto(CourtEntity entity);
}
