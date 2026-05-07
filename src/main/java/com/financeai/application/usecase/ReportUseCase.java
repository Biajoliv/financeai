package com.financeai.application.usecase;

import com.financeai.application.service.RecommendationService; 
import com.financeai.api.dto.FinancialReportResponse;
import com.financeai.domain.model.FinancialDiagnostic;
import com.financeai.domain.model.FinancialGoal;
import com.financeai.domain.model.ReportInterval;
import com.financeai.domain.model.Transaction;
import com.financeai.domain.model.UserProfile;
import com.financeai.domain.service.CalculationEngine;
import com.financeai.infrastructure.simulation.MockIngestionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportUseCase {

    private final MockIngestionService ingestionService;
    private final CalculationEngine calculationEngine;
    private final RecommendationService recommendationService;

    public ReportUseCase(MockIngestionService ingestionService, 
                         CalculationEngine calculationEngine, 
                         RecommendationService recommendationService) {
        this.ingestionService = ingestionService;
        this.calculationEngine = calculationEngine;
        this.recommendationService = recommendationService;
    }

    /**
     * Gera um relatório proativo baseado no intervalo escolhido pelo cliente.
     * Requisito Tema 4: Análise e Insights Financeiros.
     */
    public FinancialReportResponse generatePeriodReport(String userId, ReportInterval interval) {
        List<Transaction> transactions = ingestionService.loadTransactions();
        
        FinancialGoal reportGoal = new FinancialGoal(
            "Check-up " + interval.name(),
            BigDecimal.ZERO, 
            LocalDate.now().plusDays(interval.getDays()), 
            UserProfile.INDIVIDUAL
        );

        FinancialDiagnostic diagnostic = calculationEngine.calculate(userId, transactions, reportGoal);
        
        List<String> tipsList = recommendationService.getHumanReadableTips(diagnostic.recommendationChunks());
        String advice = String.join(" ", tipsList);

        return new FinancialReportResponse(diagnostic, advice);
    }
}