package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.PayoutDto;
import com.turfbook.backend.entity.PayoutEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface PayoutMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    PayoutDto toDto(PayoutEntity entity);
}
