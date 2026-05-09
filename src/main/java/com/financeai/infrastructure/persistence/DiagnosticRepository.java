package com.financeai.infrastructure.persistence;

import com.financeai.domain.entity.Diagnostic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosticRepository extends JpaRepository<Diagnostic, String> {
    @Query("SELECT d FROM Diagnostic d WHERE d.user.id = :userId AND d.deletedAt IS NULL ORDER BY d.generatedAt DESC")
    List<Diagnostic> findByUserIdAndNotDeleted(@Param("userId") UUID userId);

    List<Diagnostic> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    @Query("SELECT d FROM Diagnostic d WHERE d.id = :id AND d.user.id = :userId AND d.deletedAt IS NULL")
    Optional<Diagnostic> findByIdAndUserIdAndNotDeleted(@Param("id") String id, @Param("userId") String userId);
}
