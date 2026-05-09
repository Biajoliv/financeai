package com.financeai.api.controller;

import com.financeai.api.dto.ApiResponse;
import com.financeai.api.dto.GenerateTransactionRequest;
import com.financeai.api.dto.TransactionDto;
import com.financeai.application.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction")
@Tag(name = "Transações", description = "Listagem, geração aleatória e exclusão de transações financeiras")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Listar transações do usuário",
               description = "Retorna todas as transações ativas do usuário autenticado (excluindo soft-deleted)")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> listTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<TransactionDto> transactions = transactionService.listByUser(userDetails.getUsername());

        if (transactions.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Não foram encontrados dados.", null));
        }
        return ResponseEntity.ok(ApiResponse.success(
                transactions.size() + " transação(ões) encontrada(s).", transactions));
    }

    @PostMapping("/generate")
    @Operation(summary = "Gerar transações aleatórias",
               description = "Gera entre 1 e 10 transações aleatórias. " +
                             "Para transações de outros bancos (is_open_finance=true), o Open Finance deve estar ativo no perfil.")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> generate(
            @Valid @RequestBody GenerateTransactionRequest request,
            @RequestParam(defaultValue = "false") boolean fromOtherBank,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<TransactionDto> generated = transactionService.generateRandom(
                userDetails.getUsername(), request.count(), fromOtherBank);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        request.count() + " transação(ões) gerada(s) com sucesso.", generated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar transação (soft delete)",
               description = "Marca a transação como deletada. Não aparecerá mais em listagens.")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        transactionService.softDelete(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Transação excluída com sucesso.", null));
    }
}
