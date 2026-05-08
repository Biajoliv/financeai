package com.financeai.infrastructure.persistence;

import com.financeai.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // Todas as transações ativas do usuário (excluindo soft-deleted)
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findActiveByUserId(@Param("userId") UUID userId);

    // Transações ativas por intervalo de datas (para comparação de períodos)
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
          AND t.deletedAt IS NULL
          AND t.date >= :startDate
          AND t.date <= :endDate
        ORDER BY t.date DESC
        """)
    List<Transaction> findActiveByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Transação específica do usuário (para soft delete verificar ownership)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.user.id = :userId AND t.deletedAt IS NULL")
    Optional<Transaction> findActiveByIdAndUserId(@Param("id") String id, @Param("userId") UUID userId);

    // Contagem de transações ativas do usuário (para limitar geração)
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId AND t.deletedAt IS NULL AND t.date = :date")
    long countActiveByUserIdAndDate(@Param("userId") UUID userId, @Param("date") LocalDate date);
}
