package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.VenueDetailDto;
import com.turfbook.backend.dto.VenueSummaryDto;
import com.turfbook.backend.entity.VenueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {SportMapper.class, CourtMapper.class, DateTimeMapper.class})
public interface VenueMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "sports", source = "sports")
    @Mapping(target = "amenities", source = "amenities")
    @Mapping(target = "isActive", expression = "java(entity.isActive())")
    @Mapping(target = "courtCount", expression = "java((long)(entity.getCourts() != null ? entity.getCourts().size() : 0))")
    VenueSummaryDto toSummaryDto(VenueEntity entity);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "sports", source = "sports")
    @Mapping(target = "courts", source = "courts")
    @Mapping(target = "isActive", expression = "java(entity.isActive())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    VenueDetailDto toDetailDto(VenueEntity entity);

    List<VenueSummaryDto> toSummaryDtoList(List<VenueEntity> entities);
}
