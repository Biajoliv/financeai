package com.financeai.domain.service;

import com.financeai.domain.model.Transaction;
import java.util.List;

/**
 * PRINCÍPIO SOLID APLICADO: Dependency Inversion Principle (DIP)
 *
 * PROBLEMA IDENTIFICADO:
 * AnalyzeGoalUseCase e ReportUseCase dependiam diretamente de MockIngestionService
 * (classe concreta). Isso viola DIP: módulos de alto nível (use cases) não devem
 * depender de módulos de baixo nível (infraestrutura mock).
 *
 * SOLUÇÃO:
 * Esta interface abstrai o fornecimento de transações. Use cases agora dependem
 * desta abstração. A implementação concreta (mock, banco de dados, API externa)
 * é injetada pelo Spring sem alterar os use cases.
 *
 * BENEFÍCIO:
 * Para conectar o banco de dados real, basta criar uma nova implementação desta
 * interface — AnalyzeGoalUseCase e ReportUseCase não precisam ser tocados.
 */
public interface TransactionProvider {
    List<Transaction> getTransactionsByUserId(String userId);
}
