package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.entity.UserEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T13:20:39+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(UserEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setId( entity.getId() );
        userDto.setName( entity.getName() );
        userDto.setEmail( entity.getEmail() );
        userDto.setPhone( entity.getPhone() );
        userDto.setAvatarUrl( entity.getAvatarUrl() );
        List<String> list = entity.getPreferredSports();
        if ( list != null ) {
            userDto.setPreferredSports( new ArrayList<String>( list ) );
        }
        userDto.setTotalBookings( entity.getTotalBookings() );

        userDto.setRole( entity.getRole().name() );
        userDto.setIsPremium( entity.isPremium() );
        userDto.setIsBlocked( entity.isBlocked() );
        userDto.setCreatedAt( entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null );

        return userDto;
    }
}
