package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.entity.VenueEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T13:20:38+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class CourtMapperImpl implements CourtMapper {

    @Override
    public CourtDto toDto(CourtEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CourtDto courtDto = new CourtDto();

        courtDto.setVenueId( entityVenueId( entity ) );
        courtDto.setSportId( entitySportId( entity ) );
        courtDto.setId( entity.getId() );
        courtDto.setName( entity.getName() );
        courtDto.setType( entity.getType() );
        courtDto.setPricePerSlot( entity.getPricePerSlot() );
        courtDto.setPeakPrice( entity.getPeakPrice() );

        return courtDto;
    }

    private Long entityVenueId(CourtEntity courtEntity) {
        VenueEntity venue = courtEntity.getVenue();
        if ( venue == null ) {
            return null;
        }
        return venue.getId();
    }

    private Long entitySportId(CourtEntity courtEntity) {
        SportEntity sport = courtEntity.getSport();
        if ( sport == null ) {
            return null;
        }
        return sport.getId();
    }
}
