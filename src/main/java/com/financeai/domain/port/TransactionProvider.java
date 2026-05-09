package com.financeai.domain.port;

import com.financeai.domain.model.Transaction;
import java.util.List;

public interface TransactionProvider {
    List<Transaction> getTransactionsByUserId(String userId);
}
