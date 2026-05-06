package com.financeai.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Entity
@Table(name = "users",
       indexes = @Index(name = "idx_user_email", columnList = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String name;

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String password;

    @Column(updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relacionamentos inicializados como lista vazia — evita NullPointerException
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Goal> goals = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    /**
     * Construtor de negócio — usado pelo AuthUseCase.
     * Clean Code: construtor com intenção clara, sem campos desnecessários.
     */
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.transactions = new ArrayList<>();
        this.goals = new ArrayList<>();
        this.diagnosticos = new ArrayList<>();
    }

    /**
     * SOLID — equals e hashCode baseados APENAS no ID.
     * Garante consistência com o JPA: dois Users com mesmo ID são o mesmo objeto.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Clean Code — toString sem password.
     * Impede que a senha apareça em logs ou stack traces.
     */
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}