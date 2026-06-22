package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PlanCode;
import com.turfbook.backend.entity.SubscriptionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, Long> {

    Optional<SubscriptionPlanEntity> findByCode(PlanCode code);

    boolean existsByCode(PlanCode code);

    List<SubscriptionPlanEntity> findAllByOrderByDisplayOrderAscIdAsc();

    List<SubscriptionPlanEntity> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
