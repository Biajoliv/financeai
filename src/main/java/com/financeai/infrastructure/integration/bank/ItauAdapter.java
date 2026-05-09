package com.financeai.infrastructure.integration.bank;

import com.financeai.domain.model.Transaction;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Adapter for Itaú bank data format
 * 
 * Expected format:
 * {
 *   "tid": "789",
 *   "description_pt": "Compra no supermercado",
 *   "amount_brl": 150.50,
 *   "posting_date": "2024-05-01T10:30:00Z",
 *   "classification": "GROCERIES",
 *   "required": false
 * }
 */
@Component
public class ItauAdapter implements BankDataAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(ItauAdapter.class);
    private static final String BANK_CODE = "ITAU";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public Transaction map(Map<String, Object> rawData, String bankCode) {
        try {
            String description = (String) rawData.getOrDefault("description_pt", "Transação Itaú");
            BigDecimal amount = new BigDecimal(rawData.getOrDefault("amount_brl", "0").toString());
            
            // Parse ISO datetime (2024-05-01T10:30:00Z) and extract date
            String dateStr = (String) rawData.getOrDefault("posting_date", LocalDate.now().toString());
            LocalDate date = parseItauDate(dateStr);
            
            String classification = (String) rawData.getOrDefault("classification", "OTHER");
            String category = mapClassificationToCategory(classification);
            
            boolean required = Boolean.parseBoolean(rawData.getOrDefault("required", "false").toString());
            int priority = required ? 5 : 3; // Required expenses = higher priority
            
            logger.debug("Mapped Itaú transaction: {} - R$ {} on {}", description, amount, date);
            
            return new Transaction(description, amount, date, category, required, priority);
        } catch (Exception e) {
            logger.error("Error mapping Itaú data: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Itaú transaction format: " + e.getMessage());
        }
    }

    @Override
    public String getSupportedBankCode() {
        return BANK_CODE;
    }

    /**
     * Parse Itaú date format (ISO 8601 with time, or simple date)
     */
    private LocalDate parseItauDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }
        
        // Handle ISO format with time: 2024-05-01T10:30:00Z
        if (dateStr.contains("T")) {
            dateStr = dateStr.substring(0, 10); // Extract YYYY-MM-DD part
        }
        
        return LocalDate.parse(dateStr, ISO_DATE_FORMATTER);
    }

    /**
     * Map Itaú classification to categories
     */
    private String mapClassificationToCategory(String classification) {
        return switch (classification.toUpperCase()) {
            case "GROCERIES" -> "FOOD";
            case "FUEL", "TRANSPORT" -> "TRANSPORT";
            case "PHARMACY", "HEALTHCARE", "DOCTOR" -> "HEALTH";
            case "ENTERTAINMENT", "CINEMA", "GAMES" -> "LEISURE";
            case "SCHOOL", "UNIVERSITY" -> "EDUCATION";
            case "RENT", "UTILITIES", "ELECTRICITY", "WATER" -> "HOUSING";
            case "SALARY", "DEPOSIT" -> "SALARY";
            default -> "OTHER";
        };
    }
}
