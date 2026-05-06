package com.financeai.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =========================================================
    // ENUMS com comportamento — Strategy Pattern implícito
    // =========================================================

    /**
     * Tipo da transação.
     * Clean Code: enum com método que encapsula regra de negócio.
     */
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

    /**
     * Categoria da transação.
     * Open/Closed: novas categorias sem alterar lógica existente.
     */
    public enum Category {
        FOOD, TRANSPORT, HEALTH, LEISURE,
        EDUCATION, HOUSING, SALARY, OTHER
    }

    // =========================================================
    // MÉTODOS DE DOMÍNIO — Clean Code: nomes expressivos
    // =========================================================

    /**
     * Java 21 — switch expression para label legível em português.
     * Clean Code: método com responsabilidade única e nome expressivo.
     */
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

    /**
     * Retorna o valor com sinal correto para cálculo de saldo.
     * Java 21 — switch expression.
     * Clean Code: nome expressivo que revela a intenção.
     */
    public BigDecimal signedAmount() {
        return switch (type) {
            case INCOME  -> amount;
            case EXPENSE -> amount.negate();
        };
    }

    // =========================================================
    // equals, hashCode e toString — SOLID + Clean Code
    // =========================================================

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