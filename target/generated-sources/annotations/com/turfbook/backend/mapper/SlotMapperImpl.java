package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.SlotDto;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SlotEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-02T13:20:38+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class SlotMapperImpl implements SlotMapper {

    @Override
    public SlotDto toDto(SlotEntity entity) {
        if ( entity == null ) {
            return null;
        }

        SlotDto slotDto = new SlotDto();

        slotDto.setCourtId( entityCourtId( entity ) );
        slotDto.setId( entity.getId() );
        slotDto.setDate( entity.getDate() );
        slotDto.setPrice( entity.getPrice() );

        slotDto.setStatus( entity.getStatus().name() );
        slotDto.setStartTime( entity.getStartTime().toString() );
        slotDto.setEndTime( entity.getEndTime().toString() );

        return slotDto;
    }

    @Override
    public List<SlotDto> toDtoList(List<SlotEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<SlotDto> list = new ArrayList<SlotDto>( entities.size() );
        for ( SlotEntity slotEntity : entities ) {
            list.add( toDto( slotEntity ) );
        }

        return list;
    }

    private Long entityCourtId(SlotEntity slotEntity) {
        CourtEntity court = slotEntity.getCourt();
        if ( court == null ) {
            return null;
        }
        return court.getId();
    }
}
