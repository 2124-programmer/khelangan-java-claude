package com.turfbook.backend.repository;

import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllReadByUser(@Param("user") UserEntity user);

    List<NotificationEntity> findByUserAndIsReadFalse(UserEntity user);
}
