package com.financeai.infrastructure.persistence;

import com.financeai.domain.entity.SimulationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationRecordRepository extends JpaRepository<SimulationRecord, String> {

    @Query("SELECT s FROM SimulationRecord s WHERE s.userId = :userId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<SimulationRecord> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM SimulationRecord s WHERE s.id = :id AND s.userId = :userId AND s.deletedAt IS NULL")
    Optional<SimulationRecord> findActiveByIdAndUserId(@Param("id") String id, @Param("userId") UUID userId);
}
