package com.financeai.api.dto;

import com.financeai.domain.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDto(
    String id,
    String description,
    BigDecimal amount,
    String type,
    String category,
    LocalDate date,
    boolean isOpenFinance
) {
    public static TransactionDto from(Transaction t) {
        return new TransactionDto(
            t.getId(),
            t.getDescription(),
            t.getAmount(),
            t.getType().name(),
            t.getCategory().name(),
            t.getDate(),
            t.isOpenFinance()
        );
    }
}
