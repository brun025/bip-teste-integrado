package com.example.backend.controller;

import com.example.backend.dto.request.BeneficioCreateRequest;
import com.example.backend.dto.request.TransferenciaRequest;
import com.example.backend.dto.response.BeneficioResponse;
import com.example.backend.service.BeneficioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficios")
@Tag(name = "Benefícios", description = "API para gerenciamento de benefícios")
public class BeneficioController {

    private static final Logger logger = LoggerFactory.getLogger(BeneficioController.class);

    private final BeneficioService service;

    public BeneficioController(BeneficioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os benefícios",
            description = "Retorna lista de todos os benefícios ativos no sistema"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = BeneficioResponse.class)))
    })
    public ResponseEntity<List<BeneficioResponse>> listAll() {
        logger.info("GET /api/v1/beneficios - Listando todos os benefícios");
        List<BeneficioResponse> beneficios = service.findAll();
        logger.info("Retornando {} benefícios", beneficios.size());
        return ResponseEntity.ok(beneficios);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar benefício por ID",
            description = "Retorna os detalhes de um benefício específico"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício encontrado",
                    content = @Content(schema = @Schema(implementation = BeneficioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado")
    })
    public ResponseEntity<BeneficioResponse> findById(
            @Parameter(description = "ID do benefício", required = true, example = "1")
            @PathVariable Long id) {

        logger.info("GET /api/v1/beneficios/{} - Buscando benefício", id);
        BeneficioResponse beneficio = service.findById(id);
        return ResponseEntity.ok(beneficio);
    }

    @PostMapping
    @Operation(
            summary = "Criar novo benefício",
            description = "Cria um novo benefício no sistema"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Benefício criado com sucesso",
                    content = @Content(schema = @Schema(implementation = BeneficioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<BeneficioResponse> create(
            @Parameter(description = "Dados do benefício a ser criado", required = true)
            @Valid @RequestBody BeneficioCreateRequest request) {

        logger.info("POST /api/v1/beneficios - Criando benefício: {}", request);
        BeneficioResponse created = service.create(request);
        logger.info("Benefício criado com ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar benefício",
            description = "Atualiza os dados de um benefício existente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = BeneficioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<BeneficioResponse> update(
            @Parameter(description = "ID do benefício", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Novos dados do benefício", required = true)
            @Valid @RequestBody BeneficioCreateRequest request) {

        logger.info("PUT /api/v1/beneficios/{} - Atualizando benefício", id);
        BeneficioResponse updated = service.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desativar benefício",
            description = "Desativa um benefício (soft delete) - não remove do banco"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Benefício desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do benefício", required = true, example = "1")
            @PathVariable Long id) {

        logger.info("DELETE /api/v1/beneficios/{} - Desativando benefício", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @Operation(
            summary = "Transferir valor entre benefícios",
            description = "Realiza transferência de valor de um benefício para outro. " +
                    "Operação segura com controle de concorrência (Optimistic Locking) e retry automático."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de concorrência - tente novamente"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public ResponseEntity<Void> transfer(
            @Parameter(description = "Dados da transferência", required = true)
            @Valid @RequestBody TransferenciaRequest request) {

        logger.info("POST /api/v1/beneficios/transfer - Transferência: FROM={}, TO={}, AMOUNT={}",
                request.getFromId(), request.getToId(), request.getAmount());

        service.transfer(request);

        logger.info("Transferência concluída com sucesso");
        return ResponseEntity.ok().build();
    }
}