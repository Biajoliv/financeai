package com.financeai.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "goals",
       indexes = @Index(name = "idx_goal_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    @NotBlank(message = "Descrição é obrigatória")
    @Column(nullable = false)
    private String description;

    @Positive(message = "Valor alvo deve ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal targetAmount;

    @Positive(message = "Prazo deve ser positivo")
    @Column(nullable = false)
    private int deadlineMonths;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GoalStatus status = GoalStatus.NOT_VIABLE;

    @Column(updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum GoalStatus {
        VIABLE,
        NOT_VIABLE,
        NEEDS_ADJUSTMENT;

        // Java 21 — switch expression
        public String label() {
            return switch (this) {
                case VIABLE           -> "Viável";
                case NOT_VIABLE       -> "Inviável";
                case NEEDS_ADJUSTMENT -> "Precisa de ajuste";
            };
        }

        public boolean isPositive() {
            return this == VIABLE;
        }
    }


    public BigDecimal requiredMonthlySaving() {
        if (deadlineMonths <= 0) return targetAmount;
        return targetAmount.divide(
            BigDecimal.valueOf(deadlineMonths),
            2,
            java.math.RoundingMode.HALF_UP
        );
    }

    public boolean isAchievable(BigDecimal monthlySaving) {
        return monthlySaving.compareTo(requiredMonthlySaving()) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Goal other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Goal{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", targetAmount=" + targetAmount +
                ", deadlineMonths=" + deadlineMonths +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}