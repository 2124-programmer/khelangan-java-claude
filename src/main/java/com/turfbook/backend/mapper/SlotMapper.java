package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.SlotDto;
import com.turfbook.backend.entity.SlotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SlotMapper {

    @Mapping(target = "courtId", source = "court.id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "startTime", expression = "java(entity.getStartTime().toString())")
    @Mapping(target = "endTime", expression = "java(entity.getEndTime().toString())")
    SlotDto toDto(SlotEntity entity);

    List<SlotDto> toDtoList(List<SlotEntity> entities);
}
