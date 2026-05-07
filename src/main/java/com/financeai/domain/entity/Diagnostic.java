package com.financeai.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "diagnosticos",
       indexes = {
           @Index(name = "idx_diagnostico_user", columnList = "user_id"),
           @Index(name = "idx_diagnostico_goal", columnList = "goal_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnostic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    // Resumo do diagnóstico em texto simples
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Cenário atual — saldo projetado sem mudanças
    @Column(precision = 10, scale = 2)
    private BigDecimal currentBalance;

    // Cenário recomendado — saldo projetado com ajustes sugeridos
    @Column(precision = 10, scale = 2)
    private BigDecimal projectedBalance;

    // Cenário pessimista — saldo com imprevistos
    @Column(precision = 10, scale = 2)
    private BigDecimal pessimisticBalance;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.WARNING;

    // Insight gerado pelo LLM — texto longo
    @Column(columnDefinition = "TEXT")
    private String aiInsight;

    @Column(updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private Goal goal;

    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL;

        // Java 21 — switch expression
        public String label() {
            return switch (this) {
                case HEALTHY  -> "Saudável";
                case WARNING  -> "Atenção";
                case CRITICAL -> "Crítico";
            };
        }

        public String emoji() {
            return switch (this) {
                case HEALTHY  -> "✅";
                case WARNING  -> "⚠️";
                case CRITICAL -> "🚨";
            };
        }

        public boolean needsAction() {
            return this != HEALTHY;
        }
    }

    public BigDecimal potentialImprovement() {
        if (projectedBalance == null || currentBalance == null) {
            return BigDecimal.ZERO;
        }
        return projectedBalance.subtract(currentBalance);
    }

    public boolean canImprove() {
        return potentialImprovement().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Diagnostic other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Diagnostico{" +
                "id='" + id + '\'' +
                ", healthStatus=" + healthStatus +
                ", currentBalance=" + currentBalance +
                ", projectedBalance=" + projectedBalance +
                ", generatedAt=" + generatedAt +
                '}';
    }
}