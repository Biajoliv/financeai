package com.financeai.domain.repository;

import com.financeai.domain.entity.Diagnostic; // Import atualizado
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiagnosticRepository extends JpaRepository<Diagnostic, String> {
    // Busca histórico de diagnósticos de um usuário específico
    List<Diagnostic> findByUserIdOrderByGeneratedAtDesc(String userId);
}