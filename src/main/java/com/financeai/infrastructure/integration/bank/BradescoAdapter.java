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
 * Adapter for Bradesco bank data format
 * 
 * Expected format:
 * {
 *   "transaction_id": "456",
 *   "memo": "Compra no supermercado",
 *   "value": 150.50,
 *   "transaction_date": "01/05/2024",
 *   "type_code": "SHP",
 *   "mandatory": false
 * }
 */
@Component
public class BradescoAdapter implements BankDataAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(BradescoAdapter.class);
    private static final String BANK_CODE = "BRADESCO";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public Transaction map(Map<String, Object> rawData, String bankCode) {
        try {
            String description = (String) rawData.getOrDefault("memo", "Transação Bradesco");
            BigDecimal amount = new BigDecimal(rawData.getOrDefault("value", "0").toString());
            
            String dateStr = (String) rawData.getOrDefault("transaction_date", LocalDate.now().format(DATE_FORMATTER));
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            
            String typeCode = (String) rawData.getOrDefault("type_code", "OTHER");
            String category = mapTypeCodeToCategory(typeCode);
            
            boolean mandatory = Boolean.parseBoolean(rawData.getOrDefault("mandatory", "false").toString());
            int priority = mandatory ? 5 : 2; // Mandatory = higher priority
            
            logger.debug("Mapped Bradesco transaction: {} - R$ {} on {}", description, amount, date);
            
            return new Transaction(description, amount, date, category, mandatory, priority);
        } catch (Exception e) {
            logger.error("Error mapping Bradesco data: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Bradesco transaction format: " + e.getMessage());
        }
    }

    @Override
    public String getSupportedBankCode() {
        return BANK_CODE;
    }

    /**
     * Map Bradesco type codes to categories
     */
    private String mapTypeCodeToCategory(String typeCode) {
        return switch (typeCode.toUpperCase()) {
            case "SHP" -> "FOOD";           // Shopping
            case "RST" -> "FOOD";           // Restaurant
            case "GAS" -> "TRANSPORT";      // Gas station
            case "TRN" -> "TRANSPORT";      // Transport
            case "TXI" -> "TRANSPORT";      // Taxi
            case "MED" -> "HEALTH";         // Medical
            case "FAR" -> "HEALTH";         // Pharmacy
            case "EDU" -> "EDUCATION";      // Education
            case "ENT" -> "LEISURE";        // Entertainment
            case "HOM" -> "HOUSING";        // Housing
            case "UTL" -> "HOUSING";        // Utilities
            case "SAL" -> "SALARY";         // Salary
            default -> "OTHER";
        };
    }
}
