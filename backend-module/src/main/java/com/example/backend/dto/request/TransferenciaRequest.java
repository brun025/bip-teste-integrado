package com.example.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para realizar transferência entre benefícios")
public class TransferenciaRequest {

    @NotNull(message = "ID do benefício origem é obrigatório")
    @Positive(message = "ID do benefício origem deve ser positivo")
    @Schema(description = "ID do benefício que terá o valor debitado", example = "1", required = true)
    private Long fromId;

    @NotNull(message = "ID do benefício destino é obrigatório")
    @Positive(message = "ID do benefício destino deve ser positivo")
    @Schema(description = "ID do benefício que receberá o valor", example = "2", required = true)
    private Long toId;

    @NotNull(message = "Valor da transferência é obrigatório")
    @DecimalMin(value = "0.01", inclusive = true, message = "Valor deve ser maior que zero")
    @Digits(integer = 13, fraction = 2, message = "Valor deve ter no máximo 13 dígitos inteiros e 2 decimais")
    @Schema(description = "Valor a ser transferido", example = "500.00", required = true)
    private BigDecimal amount;

    @Schema(description = "Chave de idempotência (UUID) para evitar transferências duplicadas",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String idempotencyKey;

    public TransferenciaRequest() {
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    public TransferenciaRequest(Long fromId, Long toId, BigDecimal amount) {
        this.fromId = fromId;
        this.toId = toId;
        this.amount = amount;
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    @AssertTrue(message = "Não é possível transferir para o mesmo benefício")
    public boolean isTransferenciaValida() {
        if (fromId == null || toId == null) {
            return true; // Deixa @NotNull tratar
        }
        return !fromId.equals(toId);
    }

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public String toString() {
        return "TransferenciaRequest{" +
                "fromId=" + fromId +
                ", toId=" + toId +
                ", amount=" + amount +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                '}';
    }
}