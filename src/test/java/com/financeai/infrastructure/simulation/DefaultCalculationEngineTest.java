package com.financeai.infrastructure.simulation;

import com.financeai.domain.model.*;
import com.financeai.domain.port.WeightStrategy;
import com.financeai.infrastructure.utils.DataSanitizer;
import com.financeai.infrastructure.algorithm.KnapsackOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Suíte de testes para validar o motor de cálculo conforme os requisitos do Tema 4.
 * Valida Multi-tenancy, Algoritmos de Otimização e Java 21.
 *
 * ATUALIZADO: construtor agora recebe WeightStrategy e TaxStrategyFactory via injeção.
 * WeightStrategy é mockada para retornar pesos vazios por padrão — isolamento de teste.
 */
class DefaultCalculationEngineTest {

    private DefaultCalculationEngine engine;
    private DataSanitizer sanitizer;
    private KnapsackOptimizer knapsackOptimizer;
    private WeightStrategy weightStrategy;
    private TaxStrategyFactory taxStrategyFactory;
    private final String userId = "user-aloana-123";

    @BeforeEach
    void setUp() {
        sanitizer = Mockito.mock(DataSanitizer.class);
        knapsackOptimizer = Mockito.mock(KnapsackOptimizer.class);
        weightStrategy = Mockito.mock(WeightStrategy.class);

        // TaxStrategyFactory não é mockada: usa implementação real para validar estratégias PF/PJ
        taxStrategyFactory = new TaxStrategyFactory();

        Mockito.when(sanitizer.sanitizeDescription(anyString())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(knapsackOptimizer.optimizeExpenses(Mockito.anyList(), Mockito.any(), Mockito.anyMap()))
                .thenReturn(new ArrayList<>());

        // Pesos retornados vazios por padrão — KnapsackOptimizer usa getOrDefault(category, 1.0)
        Mockito.when(weightStrategy.calculateWeights()).thenReturn(new HashMap<>());

        engine = new DefaultCalculationEngine(sanitizer, knapsackOptimizer, weightStrategy, taxStrategyFactory);
    }

    @Test
    @DisplayName("Cenário 1: Deve calcular saldo inicial e manter liquidez segura para PF")
    void testInitialBalanceAndSafeLiquidity() {
        // GIVEN: Receita de 5000 e Gasto de 2000
        List<Transaction> transactions = List.of(
            new Transaction("Salário", new BigDecimal("5000.00"), LocalDate.now(), "SALARY", true, 5),
            new Transaction("Aluguel", new BigDecimal("-2000.00"), LocalDate.now(), "HOUSING", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Reserva", new BigDecimal("10000.00"), LocalDate.now().plusMonths(6), UserProfile.INDIVIDUAL);

        // WHEN
        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // THEN: Saldo deve ser 3000 e sem risco de liquidez em 30 dias [cite: 65, 326]
        assertEquals(new BigDecimal("3000.00"), diagnostic.currentBalance());
        assertNull(diagnostic.daysUntilNegativeBalance());
    }

    @Test
    @DisplayName("Cenário 2: Deve aplicar regra fiscal para perfil Business (PJ)")
    void testBusinessTaxApplication() {
        // GIVEN: Faturamento de 2000
        List<Transaction> transactions = List.of(
            new Transaction("Faturamento", new BigDecimal("2000.00"), LocalDate.now(), "INCOME", true, 5)
        );
        // PJ aplica dedução de 75.00 via BusinessStrategy [cite: 293, 339]
        FinancialGoal goal = new FinancialGoal("Expansão", new BigDecimal("5000.00"), LocalDate.now().plusMonths(6), UserProfile.BUSINESS);

        // WHEN
        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // THEN: 2000.00 - 75.00 = 1925.00
        assertEquals(new BigDecimal("1925.00"), diagnostic.currentBalance());
    }

    @Test
    @DisplayName("Cenário 3: Deve detectar o 'Dia D' de risco de liquidez")
    void testLiquidityRiskDetection() {
        // GIVEN: Saldo de 500 e Gasto Essencial Diário de 100 (3000/30)
        List<Transaction> transactions = List.of(
            new Transaction("Saldo", new BigDecimal("500.00"), LocalDate.now(), "OTHER", true, 5),
            new Transaction("Gasto Fixo", new BigDecimal("-3000.00"), LocalDate.now(), "HEALTH", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        // WHEN
        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // THEN: O saldo acaba em 5 dias [cite: 330]
        assertNotNull(diagnostic.daysUntilNegativeBalance());
        assertTrue(diagnostic.daysUntilNegativeBalance() >= 1);
        assertTrue(diagnostic.alerts().contains("Risco de liquidez detectado no curto prazo."));
    }

    @Test
    @DisplayName("Cenário 4: Deve gerar Chunks de otimização quando houver gastos supérfluos")
    void testOptimizationMochilaLogic() {
        // GIVEN: Saldo alto mas com gastos não essenciais passíveis de corte [cite: 34]
        List<Transaction> transactions = List.of(
            new Transaction("Salário", new BigDecimal("5000.00"), LocalDate.now(), "SALARY", true, 5),
            new Transaction("Assinatura TV", new BigDecimal("-150.00"), LocalDate.now(), "ENTERTAINMENT", false, 1),
            new Transaction("Jantar Fora", new BigDecimal("-250.00"), LocalDate.now(), "FOOD", false, 2)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("6000.00"), LocalDate.now().plusMonths(12), UserProfile.INDIVIDUAL);

        // WHEN: Mock knapsack to return some optimization items for this test
        List<Transaction> optionalExpenses = transactions.stream()
                .filter(t -> !t.isEssential() && t.amount().compareTo(BigDecimal.ZERO) < 0)
                .toList();
        Mockito.when(knapsackOptimizer.optimizeExpenses(Mockito.anyList(), Mockito.any(), Mockito.anyMap()))
                .thenReturn(optionalExpenses);
        
        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // THEN: Deve identificar o chunk de otimização para o pipeline RAG [cite: 124, 321]
        assertTrue(diagnostic.recommendationChunks().contains("OTIMIZACAO_CORTES_DISPONIVEL"));
    }

    @Test
    @DisplayName("Cenário 5: Deve sugerir revisão estrutural para metas fora da realidade")
    void testStructuralRevisionLogic() {
        // GIVEN: Saldo baixo e meta inatingível
        List<Transaction> transactions = List.of(
            new Transaction("Salário", new BigDecimal("1000.00"), LocalDate.now(), "SALARY", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Milionário", new BigDecimal("1000000.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        // WHEN
        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // THEN: Deve sinalizar a necessidade de ajuste na meta [cite: 325]
        assertTrue(diagnostic.recommendationChunks().contains("REVISAO_ESTRUTURAL_NECESSARIA"));
        assertTrue(diagnostic.alerts().stream().anyMatch(a -> a.contains("ALERTA ESTRATÉGICO")));
    }

    // --- CENÁRIOS ABSURDOS E DE BORDA ---

    @Test
    @DisplayName("Cenário 6: Gasto diário maior que o saldo total no primeiro dia")
    void testExtremeDailySpending() {
        // Saldo de 100, mas um único gasto essencial de 6000 no mês (200/dia)
        List<Transaction> transactions = List.of(
            new Transaction("Saldo", new BigDecimal("100.00"), LocalDate.now(), "OTHER", true, 5),
            new Transaction("Gasto Absurdo", new BigDecimal("-6000.00"), LocalDate.now(), "HEALTH", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        assertEquals(1, diagnostic.daysUntilNegativeBalance(), "Deve quebrar no dia 1");
    }

    @Test
    @DisplayName("Cenário 7: Transações com valores gigantescos (Billionaire Test)")
    void testBillionaireTransactions() {
        BigDecimal billion = new BigDecimal("1000000000.00");
        List<Transaction> transactions = List.of(
            new Transaction("Aporte Bilionário", billion, LocalDate.now(), "INCOME", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Meta", billion.multiply(new BigDecimal("2")), LocalDate.now().plusYears(10), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        assertEquals(billion, diagnostic.currentBalance());
        assertDoesNotThrow(() -> diagnostic.currentBalance().toPlainString());
    }

    @Test
    @DisplayName("Cenário 8: Meta com valor ZERO ou Negativo")
    void testZeroOrNegativeGoal() {
        List<Transaction> transactions = List.of(
            new Transaction("Salário", new BigDecimal("1000.00"), LocalDate.now(), "SALARY", true, 5)
        );
        // Meta de R$ 0.00 (Teoricamente já batida)
        FinancialGoal goal = new FinancialGoal("Meta Grátis", BigDecimal.ZERO, LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // Se a meta é zero e o saldo é 1000, não deve haver gap ou necessidade de revisão
        assertFalse(diagnostic.recommendationChunks().contains("REVISAO_ESTRUTURAL_NECESSARIA"));
    }

    @Test
    @DisplayName("Cenário 9: Milhares de transações minúsculas (Stress de Performance)")
    void testMassiveSmallTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            transactions.add(new Transaction("Gasto " + i, new BigDecimal("-0.01"), LocalDate.now(), "OTHER", false, 1));
        }
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("10.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // 1000 * 0.01 = 10.00 de gasto
        assertEquals(new BigDecimal("-10.00"), diagnostic.currentBalance());
    }

    @Test
    @DisplayName("Cenário 10: Transações com datas no futuro distante")
    void testFutureTransactions() {
        // O motor deve processar, mas o DTO AnalysisRequest tem @PastOrPresent que barraria isso na API
        List<Transaction> transactions = List.of(
            new Transaction("Renda Futura", new BigDecimal("5000.00"), LocalDate.now().plusYears(100), "INCOME", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("1000.00"), LocalDate.now().plusYears(101), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        assertEquals(new BigDecimal("5000.00"), diagnostic.currentBalance());
    }

    @Test
    @DisplayName("Cenário 11: Todos os gastos marcados como essenciais (Mochila impossível)")
    void testAllEssentialOptimization() {
        List<Transaction> transactions = List.of(
            new Transaction("Aluguel", new BigDecimal("-2000.00"), LocalDate.now(), "HOUSING", true, 1),
            new Transaction("Remédio", new BigDecimal("-500.00"), LocalDate.now(), "HEALTH", true, 1)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("10000.00"), LocalDate.now().plusMonths(6), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        // Não deve haver cortes sugeridos pois tudo é essencial
        assertFalse(diagnostic.recommendationChunks().contains("OTIMIZACAO_CORTES_DISPONIVEL"));
    }

    @Test
    @DisplayName("Cenário 12: Saldo Business com valor menor que a taxa fixa e risco de liquidez")
    void testBusinessNegativeBalanceAfterTax() {
        List<Transaction> transactions = List.of(
            new Transaction("Receita Pequena", new BigDecimal("10.00"), LocalDate.now(), "INCOME", true, 5),
            // Adicionando um gasto essencial para gerar taxa diária e disparar o alerta de liquidez
            new Transaction("Gasto Essencial", new BigDecimal("-100.00"), LocalDate.now(), "HEALTH", true, 5)
        );
        // Taxa fixa Business é 75.00. Receita(10) - Gasto(100) - Taxa(75) = -165.00
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("100.00"), LocalDate.now().plusMonths(1), UserProfile.BUSINESS);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        assertEquals(new BigDecimal("-165.00"), diagnostic.currentBalance());
        assertTrue(diagnostic.alerts().contains("Risco de liquidez detectado no curto prazo."));
    }

    // --- TESTES DE VALIDAÇÃO DE ESTADO E NULOS ---

    @Test
    @DisplayName("Cenário 13: Campos de texto da meta vazios (Empty Strings)")
    void testEmptyGoalFields() {
        List<Transaction> transactions = List.of(
            new Transaction("Salário", new BigDecimal("1000.00"), LocalDate.now(), "INCOME", true, 5)
        );
        // Nome vazio ""
        FinancialGoal goal = new FinancialGoal("", new BigDecimal("500.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        FinancialDiagnostic diagnostic = engine.calculate(userId, transactions, goal);

        assertNotNull(diagnostic);
        assertEquals(new BigDecimal("1000.00"), diagnostic.currentBalance());
    }

    @Test
    @DisplayName("Cenário 14: Prioridades fora do range (Extremos)")
    void testExtremePriorities() {
        List<Transaction> transactions = List.of(
            // Prioridade 100 (DTO limita a 5, mas motor deve processar)
            new Transaction("Gasto Estranho", new BigDecimal("-100.00"), LocalDate.now(), "OTHER", false, 100)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("500.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        assertDoesNotThrow(() -> engine.calculate(userId, transactions, goal));
    }

    @Test
    @DisplayName("Cenário 15: Tentativa de passar userId nulo (Multi-tenant check)")
    void testNullUserContext() {
        List<Transaction> transactions = List.of(
            new Transaction("Salário", new BigDecimal("1000.00"), LocalDate.now(), "INCOME", true, 5)
        );
        FinancialGoal goal = new FinancialGoal("Meta", new BigDecimal("500.00"), LocalDate.now().plusMonths(1), UserProfile.INDIVIDUAL);

        // O motor deve lidar com o ID nulo sem estourar NullPointerException, apenas logando ou ignorando
        assertDoesNotThrow(() -> engine.calculate(null, transactions, goal));
    }
}