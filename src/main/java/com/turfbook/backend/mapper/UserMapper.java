package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface UserMapper {

    @Mapping(target = "role", expression = "java(entity.getRole().name())")
    @Mapping(target = "isPremium", expression = "java(entity.isPremium())")
    @Mapping(target = "isBlocked", expression = "java(entity.isBlocked())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    UserDto toDto(UserEntity entity);
}
