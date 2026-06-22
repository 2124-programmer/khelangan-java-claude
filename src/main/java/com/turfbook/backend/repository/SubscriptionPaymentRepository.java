package com.turfbook.backend.repository;

import com.turfbook.backend.entity.SubscriptionPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentEntity, Long> {

    List<SubscriptionPaymentEntity> findBySubscription_IdOrderByIdDesc(Long subscriptionId);
}
