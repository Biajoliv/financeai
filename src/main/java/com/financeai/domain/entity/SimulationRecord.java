package com.financeai.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "simulations",
       indexes = @Index(name = "idx_simulation_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // JSON do goal enviado para a simulação
    @Column(name = "goal_data", columnDefinition = "TEXT")
    private String goalData;

    // JSON dos cenários resultantes (RED, YELLOW, GREEN)
    @Column(name = "scenario_results", columnDefinition = "TEXT")
    private String scenarioResults;

    // JSON do diagnóstico financeiro calculado
    @Column(name = "diagnostic_data", columnDefinition = "TEXT")
    private String diagnosticData;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
