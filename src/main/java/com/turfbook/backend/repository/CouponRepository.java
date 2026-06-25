package com.turfbook.backend.repository;

import com.turfbook.backend.entity.CouponEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    Optional<CouponEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT c FROM CouponEntity c WHERE c.isActive = true AND c.validUntil >= :today AND c.usedCount < c.maxUses")
    List<CouponEntity> findActiveCoupons(LocalDate today);

    @Query("SELECT COUNT(c) FROM CouponEntity c WHERE c.isActive = true AND c.validUntil >= :today AND c.usedCount < c.maxUses")
    long countActiveCoupons(LocalDate today);

    /** Active, in-window, non-exhausted venue-scoped coupons for a page of venues (offer badges). */
    @Query("SELECT c FROM CouponEntity c WHERE c.venue.id IN :venueIds AND c.isActive = true " +
           "AND c.validUntil >= :today AND c.usedCount < c.maxUses")
    List<CouponEntity> findActiveByVenueIds(@Param("venueIds") Collection<Long> venueIds,
                                            @Param("today") LocalDate today);
}
