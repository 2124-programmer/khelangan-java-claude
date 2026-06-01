package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.NotificationDto;
import com.turfbook.backend.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface NotificationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "type", expression = "java(entity.getType().name())")
    @Mapping(target = "isRead", expression = "java(entity.isRead())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    NotificationDto toDto(NotificationEntity entity);
}
