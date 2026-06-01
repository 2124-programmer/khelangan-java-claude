package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.entity.SportEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-01T23:04:16+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class SportMapperImpl implements SportMapper {

    @Override
    public SportDto toDto(SportEntity entity) {
        if ( entity == null ) {
            return null;
        }

        SportDto sportDto = new SportDto();

        sportDto.setId( entity.getId() );
        sportDto.setName( entity.getName() );
        sportDto.setIcon( entity.getIcon() );

        return sportDto;
    }
}
