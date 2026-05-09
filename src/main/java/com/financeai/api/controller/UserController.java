package com.financeai.api.controller;

import com.financeai.api.dto.ApiResponse;
import com.financeai.api.dto.UserDto;
import com.financeai.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Usuário", description = "Gerenciamento de dados do usuário autenticado")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Obter dados do usuário autenticado")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = userService.getByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Dados do usuário.", user));
    }

    /**
     * PUT /api/user — aceita apenas { "systemRole": "FREE"|"PREMIUM" }
     * Campos imutáveis (email, password, etc.) são rejeitados com mensagem clara.
     */
    @PutMapping
    @Operation(summary = "Atualizar dados mutáveis do usuário",
               description = "Permite alterar systemRole (FREE/PREMIUM). Campos email, senha e documento são imutáveis.")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        // Rejeita tentativas de alterar campos imutáveis
        if (body.containsKey("email")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("E-mail não pode ser alterado."));
        }
        if (body.containsKey("password") || body.containsKey("senha")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Senha não pode ser alterada."));
        }
        if (body.containsKey("cpfCnpj") || body.containsKey("documento")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Documento não pode ser alterado."));
        }

        String newRole = body.get("systemRole");
        UserDto updated = userService.updateSystemRole(userDetails.getUsername(), newRole);
        return ResponseEntity.ok(ApiResponse.success("Dados atualizados com sucesso.", updated));
    }
}
