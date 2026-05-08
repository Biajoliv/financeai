package com.financeai.application.service;

import com.financeai.api.dto.TransactionDto;
import com.financeai.domain.entity.Transaction;
import com.financeai.domain.entity.User;
import com.financeai.domain.entity.UserProfile;
import com.financeai.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final UserProfileService profileService;

    // Categorias e faixas de valores realistas para geração aleatória
    private record CategoryConfig(
            Transaction.Category category,
            Transaction.TransactionType type,
            double minAmount,
            double maxAmount,
            boolean essential,
            int priority,
            int weight  // probabilidade relativa
    ) {}

    private static final List<CategoryConfig> PF_CONFIGS = List.of(
        new CategoryConfig(Transaction.Category.SALARY,    Transaction.TransactionType.INCOME,  2500, 8000,  true,  5, 5),
        new CategoryConfig(Transaction.Category.HOUSING,   Transaction.TransactionType.EXPENSE, 800,  2500,  true,  5, 10),
        new CategoryConfig(Transaction.Category.FOOD,      Transaction.TransactionType.EXPENSE, 50,   500,   false, 3, 15),
        new CategoryConfig(Transaction.Category.TRANSPORT, Transaction.TransactionType.EXPENSE, 30,   300,   true,  4, 12),
        new CategoryConfig(Transaction.Category.HEALTH,    Transaction.TransactionType.EXPENSE, 50,   400,   true,  5, 8),
        new CategoryConfig(Transaction.Category.LEISURE,   Transaction.TransactionType.EXPENSE, 20,   300,   false, 2, 10),
        new CategoryConfig(Transaction.Category.EDUCATION, Transaction.TransactionType.EXPENSE, 100,  600,   true,  4, 6),
        new CategoryConfig(Transaction.Category.OTHER,     Transaction.TransactionType.EXPENSE, 10,   200,   false, 1, 8)
    );

    private static final List<CategoryConfig> PJ_CONFIGS = List.of(
        new CategoryConfig(Transaction.Category.SALARY,    Transaction.TransactionType.INCOME,  5000, 30000, true,  5, 5),
        new CategoryConfig(Transaction.Category.HOUSING,   Transaction.TransactionType.EXPENSE, 1500, 8000,  true,  5, 10),
        new CategoryConfig(Transaction.Category.FOOD,      Transaction.TransactionType.EXPENSE, 200,  2000,  false, 3, 10),
        new CategoryConfig(Transaction.Category.TRANSPORT, Transaction.TransactionType.EXPENSE, 100,  1500,  true,  4, 10),
        new CategoryConfig(Transaction.Category.HEALTH,    Transaction.TransactionType.EXPENSE, 200,  1000,  true,  5, 8),
        new CategoryConfig(Transaction.Category.EDUCATION, Transaction.TransactionType.EXPENSE, 500,  3000,  true,  4, 7),
        new CategoryConfig(Transaction.Category.OTHER,     Transaction.TransactionType.EXPENSE, 100,  2000,  false, 2, 10)
    );

    private static final List<String> DESCRIPTIONS_INCOME = List.of(
        "SALARIO MENSAL", "PAGAMENTO FREELANCE", "RECEITA OPERACIONAL", "REPASSE FINANCEIRO"
    );
    private static final List<String> DESCRIPTIONS_EXPENSE = List.of(
        "PAGAMENTO CONTA", "COMPRA MERCADO", "SERVICO CONTRATADO",
        "DEBITO AUTOMATICO", "TRANSFERENCIA PIX", "PAGAMENTO BOLETO"
    );

    public TransactionService(TransactionRepository transactionRepository,
                              UserService userService,
                              UserProfileService profileService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.profileService = profileService;
    }

    /**
     * Lista transações ativas do usuário autenticado.
     */
    public List<TransactionDto> listByUser(String email) {
        User user = userService.getEntityByEmail(email);
        return transactionRepository.findActiveByUserId(user.getId())
                .stream().map(TransactionDto::from).toList();
    }

    /**
     * Gera N transações aleatórias para o usuário.
     * Valida open_finance: transações de outros bancos só se permitido no perfil.
     */
    @Transactional
    public List<TransactionDto> generateRandom(String email, int count, boolean fromOtherBank) {
        User user = userService.getEntityByEmail(email);
        UUID userId = user.getId();

        // Validação Open Finance
        if (fromOtherBank) {
            UserProfile profile = profileService.getProfileEntity(userId);
            boolean openFinanceEnabled = profile != null && profile.isHasOpenFinanceActive();
            if (!openFinanceEnabled) {
                throw new IllegalArgumentException(
                    "Transações de outros bancos requerem Open Finance ativo. Ative em PUT /api/profile."
                );
            }
        }

        // Determina perfil PF ou PJ para calibrar valores
        UserProfile profile = profileService.getProfileEntity(userId);
        boolean isPj = profile != null && "PJ".equals(profile.getHolderType());
        List<CategoryConfig> configs = isPj ? PJ_CONFIGS : PF_CONFIGS;

        List<Transaction> saved = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            CategoryConfig config = weightedRandom(configs, random);

            BigDecimal amount = randomAmount(config.minAmount(), config.maxAmount(), random);
            String description = buildDescription(config.type(), random);

            // Data aleatória nos últimos 30 dias
            LocalDate date = LocalDate.now().minusDays(random.nextInt(30));

            Transaction tx = Transaction.builder()
                    .description(description)
                    .amount(amount)
                    .type(config.type())
                    .category(config.category())
                    .date(date)
                    .isOpenFinance(fromOtherBank)
                    .user(user)
                    .build();

            saved.add(transactionRepository.save(tx));
        }

        logger.info("[Tenant: {}] {} transações geradas (open_finance={})", email, count, fromOtherBank);
        return saved.stream().map(TransactionDto::from).toList();
    }

    /**
     * Soft delete de transação — verifica ownership antes de deletar.
     */
    @Transactional
    public void softDelete(String email, String transactionId) {
        User user = userService.getEntityByEmail(email);
        Transaction tx = transactionRepository
                .findActiveByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada ou não autorizada."));

        tx.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(tx);
        logger.info("[Tenant: {}] Transação {} deletada (soft)", email, transactionId);
    }

    private CategoryConfig weightedRandom(List<CategoryConfig> configs, Random random) {
        int totalWeight = configs.stream().mapToInt(CategoryConfig::weight).sum();
        int pick = random.nextInt(totalWeight);
        int current = 0;
        for (CategoryConfig c : configs) {
            current += c.weight();
            if (pick < current) return c;
        }
        return configs.get(0);
    }

    private BigDecimal randomAmount(double min, double max, Random random) {
        double value = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildDescription(Transaction.TransactionType type, Random random) {
        List<String> pool = type == Transaction.TransactionType.INCOME ? DESCRIPTIONS_INCOME : DESCRIPTIONS_EXPENSE;
        return pool.get(random.nextInt(pool.size())) + " " + (1000 + new Random().nextInt(9000));
    }
}
