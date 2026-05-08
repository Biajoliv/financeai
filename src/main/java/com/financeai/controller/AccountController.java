package com.financeai.controller;

import com.financeai.api.dto.ApiResponse;
import com.financeai.infrastructure.persistence.UserRepository;
import com.financeai.domain.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Gerenciamento de conta e autenticação")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final UserRepository userRepository;

    public AccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @DeleteMapping("/account")
    @Operation(summary = "Deletar conta do usuário", description = "Remove a conta do usuário autenticado (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        logger.info("[Tenant: {}] Iniciando exclusão de conta", email);
        
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.warn("[Tenant: {}] Usuário não encontrado durante exclusão", email);
                        return new IllegalArgumentException("Usuário não encontrado");
                    });
            
            // Soft delete: marcar com timestamp de exclusão
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            
            logger.info("[Tenant: {}] Conta deletada com sucesso", email);
            return ResponseEntity.ok(ApiResponse.success("Conta deletada com sucesso", null));
        } catch (Exception e) {
            logger.error("[Tenant: {}] Erro ao deletar conta: {}", email, e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Erro ao deletar conta. Tente novamente mais tarde."));
        }
    }
}
