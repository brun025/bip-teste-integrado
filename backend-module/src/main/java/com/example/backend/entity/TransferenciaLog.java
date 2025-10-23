package com.example.backend.entity;

import com.example.backend.enums.TransferenciaStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "TRANSFERENCIA_LOG",
        indexes = {
                @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
public class TransferenciaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "from_id", nullable = false)
    private Long fromId;

    @Column(name = "to_id", nullable = false)
    private Long toId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferenciaStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TransferenciaLog() {
        this.createdAt = LocalDateTime.now();
    }

    public TransferenciaLog(String idempotencyKey, Long fromId, Long toId, BigDecimal amount) {
        this.idempotencyKey = idempotencyKey;
        this.fromId = fromId;
        this.toId = toId;
        this.amount = amount;
        this.status = TransferenciaStatus.PROCESSANDO;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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

    public TransferenciaStatus getStatus() {
        return status;
    }

    public void setStatus(TransferenciaStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void marcarComoProcessado() {
        this.status = TransferenciaStatus.PROCESSADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void marcarComoErro(String mensagemErro) {
        this.status = TransferenciaStatus.ERRO;
        this.errorMessage = mensagemErro;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean foiProcessadaComSucesso() {
        return TransferenciaStatus.PROCESSADO.equals(this.status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferenciaLog that = (TransferenciaLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransferenciaLog{" +
                "id=" + id +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", fromId=" + fromId +
                ", toId=" + toId +
                ", amount=" + amount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}