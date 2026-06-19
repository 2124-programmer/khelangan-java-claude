package com.turfbook.backend.mapper;

import com.turfbook.backend.dto.ReviewDto;
import com.turfbook.backend.entity.ReviewEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class ReviewMapper {

    public ReviewDto toDto(ReviewEntity entity, boolean isOwn, String venueName) {
        if (entity == null) return null;
        ReviewDto dto = new ReviewDto();
        dto.setId(entity.getId());
        dto.setVenueId(entity.getVenue().getId());
        dto.setAuthorName(entity.getAuthorName());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        dto.setIsOwn(isOwn);
        dto.setVenueName(venueName);
        return dto;
    }
}
