package com.financeai.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions",
       indexes = {
           @Index(name = "idx_transaction_user", columnList = "user_id"),
           @Index(name = "idx_transaction_date", columnList = "date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    @NotBlank(message = "Descrição é obrigatória")
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Tipo é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Builder.Default
    private LocalDate date = LocalDate.now();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum TransactionType {
        INCOME, EXPENSE;

        public boolean isPositive() {
            return this == INCOME;
        }

        // Java 21 — switch expression
        public String label() {
            return switch (this) {
                case INCOME  -> "Receita";
                case EXPENSE -> "Despesa";
            };
        }
    }

    public enum Category {
        FOOD, TRANSPORT, HEALTH, LEISURE,
        EDUCATION, HOUSING, SALARY, OTHER
    }

    
    public String categoryLabel() {
        return switch (category) {
            case FOOD       -> "Alimentação";
            case TRANSPORT  -> "Transporte";
            case HEALTH     -> "Saúde";
            case LEISURE    -> "Lazer";
            case EDUCATION  -> "Educação";
            case HOUSING    -> "Moradia";
            case SALARY     -> "Salário";
            case OTHER      -> "Outros";
        };
    }

    public BigDecimal signedAmount() {
        return switch (type) {
            case INCOME  -> amount;
            case EXPENSE -> amount.negate();
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", category=" + category +
                ", date=" + date +
                '}';
    }
}