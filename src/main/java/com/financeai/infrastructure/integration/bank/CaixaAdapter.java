package com.financeai.infrastructure.integration.bank;

import com.financeai.domain.model.Transaction;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Adapter for Caixa bank data format (default/standard format)
 * 
 * Expected format:
 * {
 *   "id": "123",
 *   "description": "Compra no supermercado",
 *   "amount": 150.50,
 *   "date": "2024-05-01",
 *   "category": "FOOD",
 *   "essential": false,
 *   "priority": 2
 * }
 */
@Component
public class CaixaAdapter implements BankDataAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CaixaAdapter.class);
    private static final String BANK_CODE = "CAIXA";

    @Override
    public Transaction map(Map<String, Object> rawData, String bankCode) {
        try {
            String description = (String) rawData.getOrDefault("description", "Transação");
            BigDecimal amount = new BigDecimal(rawData.getOrDefault("amount", "0").toString());
            
            String dateStr = (String) rawData.getOrDefault("date", LocalDate.now().toString());
            LocalDate date = LocalDate.parse(dateStr);
            
            String categoryStr = (String) rawData.getOrDefault("category", "OTHER");
            boolean essential = Boolean.parseBoolean(rawData.getOrDefault("essential", "false").toString());
            int priority = Integer.parseInt(rawData.getOrDefault("priority", "1").toString());
            
            logger.debug("Mapped Caixa transaction: {} - R$ {} on {}", description, amount, date);
            
            return new Transaction(description, amount, date, categoryStr, essential, priority);
        } catch (Exception e) {
            logger.error("Error mapping Caixa data: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Caixa transaction format: " + e.getMessage());
        }
    }

    @Override
    public String getSupportedBankCode() {
        return BANK_CODE;
    }
}
