package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.SlotDto;
import com.turfbook.backend.entity.SlotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SlotMapper {

    @Mapping(target = "courtId", source = "court.id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    SlotDto toDto(SlotEntity entity);

    List<SlotDto> toDtoList(List<SlotEntity> entities);
}
