package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.dto.SportDto;
import com.turfbook.backend.dto.VenueDetailDto;
import com.turfbook.backend.dto.VenueSummaryDto;
import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SportEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-01T23:04:16+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class VenueMapperImpl implements VenueMapper {

    @Autowired
    private SportMapper sportMapper;
    @Autowired
    private CourtMapper courtMapper;

    @Override
    public VenueSummaryDto toSummaryDto(VenueEntity entity) {
        if ( entity == null ) {
            return null;
        }

        VenueSummaryDto venueSummaryDto = new VenueSummaryDto();

        venueSummaryDto.setOwnerId( entityOwnerId( entity ) );
        venueSummaryDto.setId( entity.getId() );
        venueSummaryDto.setName( entity.getName() );
        venueSummaryDto.setAddress( entity.getAddress() );
        venueSummaryDto.setCity( entity.getCity() );
        venueSummaryDto.setRating( entity.getRating() );
        venueSummaryDto.setReviewCount( entity.getReviewCount() );
        venueSummaryDto.setPricePerSlot( entity.getPricePerSlot() );
        venueSummaryDto.setCoverPhoto( entity.getCoverPhoto() );
        venueSummaryDto.setLat( entity.getLat() );
        venueSummaryDto.setLng( entity.getLng() );

        venueSummaryDto.setStatus( entity.getStatus().name() );

        return venueSummaryDto;
    }

    @Override
    public VenueDetailDto toDetailDto(VenueEntity entity) {
        if ( entity == null ) {
            return null;
        }

        VenueDetailDto venueDetailDto = new VenueDetailDto();

        venueDetailDto.setOwnerId( entityOwnerId( entity ) );
        venueDetailDto.setSports( sportEntitySetToSportDtoList( entity.getSports() ) );
        venueDetailDto.setCourts( courtEntityListToCourtDtoList( entity.getCourts() ) );
        venueDetailDto.setId( entity.getId() );
        venueDetailDto.setName( entity.getName() );
        venueDetailDto.setAddress( entity.getAddress() );
        venueDetailDto.setCity( entity.getCity() );
        venueDetailDto.setDescription( entity.getDescription() );
        venueDetailDto.setRating( entity.getRating() );
        venueDetailDto.setReviewCount( entity.getReviewCount() );
        venueDetailDto.setPricePerSlot( entity.getPricePerSlot() );
        venueDetailDto.setCoverPhoto( entity.getCoverPhoto() );
        List<String> list2 = entity.getPhotos();
        if ( list2 != null ) {
            venueDetailDto.setPhotos( new ArrayList<String>( list2 ) );
        }
        List<String> list3 = entity.getAmenities();
        if ( list3 != null ) {
            venueDetailDto.setAmenities( new ArrayList<String>( list3 ) );
        }
        venueDetailDto.setLat( entity.getLat() );
        venueDetailDto.setLng( entity.getLng() );

        venueDetailDto.setStatus( entity.getStatus().name() );
        venueDetailDto.setCreatedAt( entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null );

        return venueDetailDto;
    }

    @Override
    public List<VenueSummaryDto> toSummaryDtoList(List<VenueEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<VenueSummaryDto> list = new ArrayList<VenueSummaryDto>( entities.size() );
        for ( VenueEntity venueEntity : entities ) {
            list.add( toSummaryDto( venueEntity ) );
        }

        return list;
    }

    private Long entityOwnerId(VenueEntity venueEntity) {
        UserEntity owner = venueEntity.getOwner();
        if ( owner == null ) {
            return null;
        }
        return owner.getId();
    }

    protected List<SportDto> sportEntitySetToSportDtoList(Set<SportEntity> set) {
        if ( set == null ) {
            return null;
        }

        List<SportDto> list = new ArrayList<SportDto>( set.size() );
        for ( SportEntity sportEntity : set ) {
            list.add( sportMapper.toDto( sportEntity ) );
        }

        return list;
    }

    protected List<CourtDto> courtEntityListToCourtDtoList(List<CourtEntity> list) {
        if ( list == null ) {
            return null;
        }

        List<CourtDto> list1 = new ArrayList<CourtDto>( list.size() );
        for ( CourtEntity courtEntity : list ) {
            list1.add( courtMapper.toDto( courtEntity ) );
        }

        return list1;
    }
}
