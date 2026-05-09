package com.financeai.infrastructure.persistence;

import com.financeai.domain.entity.ReportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRecordRepository extends JpaRepository<ReportRecord, String> {

    @Query("SELECT r FROM ReportRecord r WHERE r.userId = :userId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<ReportRecord> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM ReportRecord r WHERE r.userId = :userId AND r.intervalType = :interval AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<ReportRecord> findActiveByUserIdAndInterval(@Param("userId") UUID userId, @Param("interval") String interval);

    @Query("SELECT r FROM ReportRecord r WHERE r.id = :id AND r.userId = :userId AND r.deletedAt IS NULL")
    Optional<ReportRecord> findActiveByIdAndUserId(@Param("id") String id, @Param("userId") UUID userId);
}
