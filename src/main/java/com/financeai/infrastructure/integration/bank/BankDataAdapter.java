package com.financeai.infrastructure.integration.bank;

import com.financeai.domain.model.Transaction;
import java.util.Map;

/**
 * Interface for bank-specific data adapters.
 * Each bank may have different field names and formats,
 * but they all need to be converted to our standard Transaction format.
 */
public interface BankDataAdapter {
    
    /**
     * Map raw bank data to our standard Transaction format
     * 
     * @param rawData Map of field names to values from bank API/import
     * @param bankCode Identifier for which bank this data came from
     * @return Standardized Transaction record
     */
    Transaction map(Map<String, Object> rawData, String bankCode);
    
    /**
     * Get the bank code this adapter handles
     * @return Bank identifier (e.g., "CAIXA", "BRADESCO", "ITAU")
     */
    String getSupportedBankCode();
}
