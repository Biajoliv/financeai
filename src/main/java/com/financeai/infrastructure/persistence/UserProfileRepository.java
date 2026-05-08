package com.financeai.infrastructure.persistence;

import com.financeai.domain.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query("SELECT p FROM UserProfile p WHERE p.userId = :userId AND p.deletedAt IS NULL")
    Optional<UserProfile> findActiveByUserId(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);
}
