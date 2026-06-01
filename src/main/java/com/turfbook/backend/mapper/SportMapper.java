package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.entity.SportEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SportMapper {

    SportDto toDto(SportEntity entity);
}
