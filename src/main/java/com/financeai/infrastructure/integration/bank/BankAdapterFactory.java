package com.financeai.infrastructure.integration.bank;

import com.financeai.domain.model.Transaction;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for bank data adapters
 * 
 * Allows pluggable adapters for different banks.
 * Falls back to Caixa (default) if bank code is not recognized.
 */
@Service
public class BankAdapterFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(BankAdapterFactory.class);
    private static final String DEFAULT_BANK = "CAIXA";
    
    private final Map<String, BankDataAdapter> adapters;

    public BankAdapterFactory(List<BankDataAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(BankDataAdapter::getSupportedBankCode, Function.identity()));
        logger.info("Registered {} bank adapters: {}", adapters.size(), adapters.keySet());
    }

    /**
     * Adapt raw bank data to Transaction format
     * 
     * @param rawData Raw data from bank API/import
     * @param bankCode Bank identifier (e.g., "CAIXA", "BRADESCO", "ITAU")
     * @return Standardized Transaction record
     */
    public Transaction adapt(Map<String, Object> rawData, String bankCode) {
        String normalizedCode = (bankCode != null ? bankCode.toUpperCase() : DEFAULT_BANK);
        
        BankDataAdapter adapter = adapters.getOrDefault(normalizedCode, adapters.get(DEFAULT_BANK));
        
        if (adapter == null) {
            logger.warn("No adapter found for bank {} or default {}, using Caixa", normalizedCode, DEFAULT_BANK);
            adapter = adapters.get(DEFAULT_BANK);
        }
        
        if (!normalizedCode.equals(DEFAULT_BANK) && adapter != null) {
            logger.debug("Using {} adapter for bank {}", adapter.getSupportedBankCode(), normalizedCode);
        }
        
        return adapter.map(rawData, normalizedCode);
    }

    /**
     * Get list of supported bank codes
     */
    public List<String> getSupportedBanks() {
        return adapters.keySet().stream().sorted().toList();
    }

    /**
     * Check if a bank code is supported
     */
    public boolean supports(String bankCode) {
        return adapters.containsKey((bankCode != null ? bankCode.toUpperCase() : DEFAULT_BANK));
    }
}
