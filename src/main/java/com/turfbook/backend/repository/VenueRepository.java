package com.turfbook.backend.repository;

import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<VenueEntity, Long> {

    Page<VenueEntity> findByOwner(UserEntity owner, Pageable pageable);

    Page<VenueEntity> findByStatus(VenueEntity.VenueStatus status, Pageable pageable);

    @Query("SELECT DISTINCT v FROM VenueEntity v LEFT JOIN v.sports s WHERE " +
           "v.status = :liveStatus AND " +
           "(:city IS NULL OR LOWER(v.city) = LOWER(:city)) AND " +
           "(:sport IS NULL OR LOWER(s.name) = LOWER(:sport)) AND " +
           "(:search IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<VenueEntity> findLiveVenues(
            @Param("liveStatus") VenueEntity.VenueStatus liveStatus,
            @Param("city") String city,
            @Param("sport") String sport,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(VenueEntity.VenueStatus status);

    @Query("SELECT COUNT(v) FROM VenueEntity v WHERE v.owner = :owner AND v.status = :liveStatus")
    long countLiveByOwner(@Param("owner") UserEntity owner, @Param("liveStatus") VenueEntity.VenueStatus liveStatus);

    // For admin stats: pending approvals
    long countByStatusIn(List<VenueEntity.VenueStatus> statuses);

    boolean existsByOwnerAndName(UserEntity owner, String name);

    boolean existsByOwnerAndNameAndIdNot(UserEntity owner, String name, Long id);
}
