package com.financeai.infrastructure.persistence;

import com.financeai.domain.model.Transaction;
import com.financeai.domain.port.TransactionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementação REAL de TransactionProvider usando o banco de dados PostgreSQL.
 * Substitui MockTransactionProvider como @Primary — injetado automaticamente nos use cases.
 *
 * Converte JPA entities (com amounts positivos + tipo INCOME/EXPENSE)
 * para domain model Transaction records (com amounts SIGNED negativos para despesas).
 * Essa conversão é necessária para o motor de cálculo funcionar corretamente.
 */
@Service
@Primary
public class DatabaseTransactionProvider implements TransactionProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTransactionProvider.class);
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public DatabaseTransactionProvider(TransactionRepository transactionRepository,
                                       UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Transaction> getTransactionsByUserId(String userEmail) {
        // O userId aqui é o email (conforme JwtService gera tokens com email)
        return userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .map(user -> {
                    List<com.financeai.domain.entity.Transaction> entities =
                            transactionRepository.findActiveByUserId(user.getId());

                    logger.debug("[Tenant: {}] {} transações carregadas do banco", userEmail, entities.size());

                    if (entities.isEmpty()) {
                        logger.warn("[Tenant: {}] Nenhuma transação no banco — use POST /api/transaction/generate", userEmail);
                        return List.<Transaction>of();
                    }

                    // Converte JPA entity → domain model record com amount signed
                    return entities.stream()
                            .map(this::toDomainModel)
                            .toList();
                })
                .orElse(List.of());
    }

    private Transaction toDomainModel(com.financeai.domain.entity.Transaction entity) {
        // Despesas ficam negativas no domain model (usado pelo motor de cálculo)
        var signedAmount = entity.signedAmount();

        // Mapeia categoria da entidade para string (usado no KnapsackOptimizer)
        String category = entity.getCategory().name();

        // Essencial: HOUSING, HEALTH, SALARY, TRANSPORT são sempre essenciais
        boolean isEssential = switch (entity.getCategory()) {
            case HOUSING, HEALTH, SALARY, TRANSPORT -> true;
            default -> false;
        };

        // Prioridade baseada no tipo e categoria
        int priority = switch (entity.getCategory()) {
            case SALARY -> 5;
            case HOUSING, HEALTH -> 4;
            case TRANSPORT, EDUCATION -> 3;
            case FOOD -> 2;
            default -> 1;
        };

        return new Transaction(
                entity.getDescription(),
                signedAmount,
                entity.getDate(),
                category,
                isEssential,
                priority
        );
    }
}
