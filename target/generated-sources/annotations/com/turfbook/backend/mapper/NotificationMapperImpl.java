package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.NotificationDto;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.UserEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T00:59:15+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationDto toDto(NotificationEntity entity) {
        if ( entity == null ) {
            return null;
        }

        NotificationDto notificationDto = new NotificationDto();

        notificationDto.setUserId( entityUserId( entity ) );
        notificationDto.setId( entity.getId() );
        notificationDto.setTitle( entity.getTitle() );
        notificationDto.setBody( entity.getBody() );

        notificationDto.setType( entity.getType().name() );
        notificationDto.setIsRead( entity.isRead() );
        notificationDto.setCreatedAt( entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null );

        return notificationDto;
    }

    private Long entityUserId(NotificationEntity notificationEntity) {
        UserEntity user = notificationEntity.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
