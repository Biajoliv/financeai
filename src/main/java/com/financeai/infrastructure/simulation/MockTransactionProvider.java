package com.financeai.infrastructure.simulation;

import com.financeai.domain.model.Transaction;
import com.financeai.domain.service.TransactionProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PRINCÍPIO SOLID APLICADO: Dependency Inversion Principle (DIP)
 *
 * PROBLEMA IDENTIFICADO:
 * AnalyzeGoalUseCase e ReportUseCase chamavam MockIngestionService diretamente,
 * acoplando camadas de aplicação à infraestrutura de simulação.
 *
 * SOLUÇÃO — Implementação temporária de TransactionProvider:
 * Esta classe serve como adaptador entre a interface TransactionProvider e
 * MockIngestionService durante o período de desenvolvimento sem banco de dados.
 *
 * COMO SUBSTITUIR PELO BANCO:
 * 1. Criar DatabaseTransactionProvider implementando TransactionProvider
 * 2. Injetar TransactionRepository nela
 * 3. Anotar com @Primary ou remover esta classe
 * 4. AnalyzeGoalUseCase e ReportUseCase continuam inalterados
 *
 * O userId é recebido mas ignorado propositalmente nesta implementação mock:
 * o mock retorna sempre os mesmos dados de teste independente do usuário.
 */
@Service
public class MockTransactionProvider implements TransactionProvider {

    private final MockIngestionService ingestionService;

    public MockTransactionProvider(MockIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public List<Transaction> getTransactionsByUserId(String userId) {
        // Substitua por consulta real: transactionRepository.findByUserId(userId)
        return ingestionService.loadTransactions();
    }
}
