package com.financeai.api.dto;

import com.financeai.domain.model.FinancialGoal;
import com.financeai.domain.model.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AnalysisRequest {
    @NotEmpty(message = "A lista de transações não pode estar vazia")
    @Valid
    private List<Transaction> transactions;

    @NotNull(message = "O objetivo financeiro é obrigatório")
    @Valid
    private FinancialGoal goal;

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
    public FinancialGoal getGoal() { return goal; }
    public void setGoal(FinancialGoal goal) { this.goal = goal; }
}