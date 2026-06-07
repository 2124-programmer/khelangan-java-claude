package com.turfbook.backend.repository;

import com.turfbook.backend.entity.CourtEntity;
import com.turfbook.backend.entity.SlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<SlotEntity, Long> {

    List<SlotEntity> findByCourtAndDateOrderByStartTime(CourtEntity court, LocalDate date);

    List<SlotEntity> findByCourtAndDate(CourtEntity court, LocalDate date);

    Optional<SlotEntity> findByCourtAndDateAndStartTime(CourtEntity court, LocalDate date, LocalTime startTime);

    @Modifying
    @Query("UPDATE SlotEntity s SET s.status = :newStatus WHERE s.court = :court AND s.date = :date AND s.status = :availableStatus")
    int bulkUpdateStatusByCourtAndDate(
            @Param("court") CourtEntity court,
            @Param("date") LocalDate date,
            @Param("newStatus") SlotEntity.SlotStatus newStatus,
            @Param("availableStatus") SlotEntity.SlotStatus availableStatus
    );
}
