package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.entity.SportEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T00:59:15+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
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
