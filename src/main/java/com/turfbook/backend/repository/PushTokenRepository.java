package com.turfbook.backend.repository;

import com.turfbook.backend.entity.PushTokenEntity;
import com.turfbook.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushTokenEntity, Long> {

    Optional<PushTokenEntity> findByToken(String token);

    List<PushTokenEntity> findByUser(UserEntity user);

    @Modifying
    @Query("DELETE FROM PushTokenEntity p WHERE p.token = :token")
    void deleteByToken(@Param("token") String token);
}
