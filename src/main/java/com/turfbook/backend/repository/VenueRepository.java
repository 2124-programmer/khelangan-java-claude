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

    /** All of an owner's venues (used by the owner-moderation cascade). */
    List<VenueEntity> findByOwner(UserEntity owner);

    Page<VenueEntity> findByStatus(VenueEntity.VenueStatus status, Pageable pageable);

    /**
     * Batch per-owner venue aggregate for the Admin Owners list. One row per owner:
     * [ownerId, totalVenues, liveVenues, avgRating]. ARCHIVED venues (deleted owners) are excluded
     * from the live count but still counted in totalVenues for the record.
     */
    @Query("SELECT v.owner.id, COUNT(v), "
            + "SUM(CASE WHEN v.status = 'LIVE' THEN 1 ELSE 0 END), "
            + "AVG(v.ratingAverage) "
            + "FROM VenueEntity v WHERE v.owner.id IN :ownerIds GROUP BY v.owner.id")
    List<Object[]> aggregateByOwnerIds(@Param("ownerIds") java.util.Collection<Long> ownerIds);

    // A venue is shown to players only when approved (status LIVE) AND it holds an
    // active/trialing subscription (denormalized subscriptionActive flag) AND that subscription
    // covers at least one active court (bookableCourtCount > 0) — a venue with no bookable court
    // is omitted from discovery entirely. The sport filter matches the denormalized
    // bookableSportIds (sports with a covered/bookable court), NOT the static venue_sports list,
    // so a venue whose court for that sport is locked/uncovered is excluded. Ordering applies the
    // active plan's placementWeight (0 when no PRIORITY_PLACEMENT) on top of rating.
    @Query("SELECT v FROM VenueEntity v WHERE " +
           "v.status = :liveStatus AND v.subscriptionActive = true AND v.bookableCourtCount > 0 AND " +
           "(:city IS NULL OR LOWER(v.city) = LOWER(:city)) AND " +
           "(:sportId IS NULL OR :sportId MEMBER OF v.bookableSportIds) AND " +
           "(:search IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.city) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY v.placementWeight DESC, COALESCE(v.ratingAverage, 0) DESC, v.id DESC")
    Page<VenueEntity> findLiveVenues(
            @Param("liveStatus") VenueEntity.VenueStatus liveStatus,
            @Param("city") String city,
            @Param("sportId") Long sportId,
            @Param("search") String search,
            Pageable pageable
    );

    // Discovery with optional price/rating filters. Ordering is supplied via the Pageable's Sort
    // (so the service can switch between default placement, price, rating, and newest) rather than
    // a hardcoded ORDER BY. The bookable-court + live + active-subscription gate is unchanged. The
    // sport filter matches the denormalized bookableSportIds (sports with a covered/bookable court),
    // not the static venue_sports list, so a venue whose court for that sport is locked is excluded.
    @Query("SELECT v FROM VenueEntity v WHERE " +
           "v.status = :liveStatus AND v.subscriptionActive = true AND v.bookableCourtCount > 0 AND " +
           "(:city IS NULL OR LOWER(v.city) = LOWER(:city)) AND " +
           "(:sportId IS NULL OR :sportId MEMBER OF v.bookableSportIds) AND " +
           "(:search IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.city) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:minPrice IS NULL OR v.pricePerHour >= :minPrice) AND " +
           "(:maxPrice IS NULL OR v.pricePerHour <= :maxPrice) AND " +
           "(:minRating IS NULL OR v.ratingAverage >= :minRating)")
    Page<VenueEntity> findLiveVenuesFiltered(
            @Param("liveStatus") VenueEntity.VenueStatus liveStatus,
            @Param("city") String city,
            @Param("sportId") Long sportId,
            @Param("search") String search,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    /**
     * Admin subscription table: every venue that has entered the pipeline (anything but DRAFT),
     * matched against name, city, owner name, or owner phone. Subscription-status filtering and
     * pagination are applied in the service after the current subscription is resolved per row.
     */
    @Query("SELECT v FROM VenueEntity v WHERE v.status <> :excludeStatus AND " +
           "(:q IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(v.city) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(v.owner.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR v.owner.phone LIKE CONCAT('%', :q, '%')) " +
           "ORDER BY v.name ASC")
    List<VenueEntity> searchForSubscriptionTable(
            @Param("q") String q,
            @Param("excludeStatus") VenueEntity.VenueStatus excludeStatus);

    /**
     * Admin Venues screen: filter by a set of statuses (a tab maps to one or more) and search
     * across venue name, owner name, address, and city. Newest-updated first.
     */
    @Query("SELECT v FROM VenueEntity v WHERE v.status IN :statuses AND " +
           "(:q IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(v.owner.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(v.address) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(v.city) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY v.createdAt DESC")
    Page<VenueEntity> adminSearch(
            @Param("statuses") List<VenueEntity.VenueStatus> statuses,
            @Param("q") String q,
            Pageable pageable);

    /** Owners with at least one LIVE venue (Admin Owners KPI: "Active"). */
    @Query("SELECT COUNT(DISTINCT v.owner.id) FROM VenueEntity v WHERE v.status = 'LIVE'")
    long countDistinctOwnersWithLiveVenue();

    long countByStatus(VenueEntity.VenueStatus status);

    @Query("SELECT COUNT(v) FROM VenueEntity v WHERE v.owner = :owner AND v.status = :liveStatus")
    long countLiveByOwner(@Param("owner") UserEntity owner, @Param("liveStatus") VenueEntity.VenueStatus liveStatus);

    // For admin stats: pending approvals
    long countByStatusIn(List<VenueEntity.VenueStatus> statuses);

    long countByOwner(UserEntity owner);

    boolean existsByOwnerAndName(UserEntity owner, String name);

    boolean existsByOwnerAndNameAndIdNot(UserEntity owner, String name, Long id);
}
