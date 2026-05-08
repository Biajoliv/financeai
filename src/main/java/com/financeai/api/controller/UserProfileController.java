package com.financeai.api.controller;

import com.financeai.api.dto.ApiResponse;
import com.financeai.api.dto.UpdateProfileRequest;
import com.financeai.api.dto.UserProfileDto;
import com.financeai.application.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Perfil", description = "Configurações de perfil do usuário (open finance, report interval, etc.)")
public class UserProfileController {

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Obter perfil do usuário",
               description = "Retorna o perfil. Cria um perfil padrão (PF, MONTHLY, open_finance=false) se ainda não existir.")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileDto profile = profileService.getOrCreateProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Perfil do usuário.", profile));
    }

    @PutMapping
    @Operation(summary = "Atualizar perfil do usuário",
               description = "Permite alterar: name, hasOpenFinanceActive, reportPreference (WEEKLY/BIWEEKLY/MONTHLY), postalCode. " +
                             "Campos imutáveis (email, password, cpfCnpj, holderType) são rejeitados.")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {

        UserProfileDto updated = profileService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Perfil atualizado com sucesso.", updated));
    }
}
