-- Tabela para controle de idempotência em transferências
CREATE TABLE TRANSFERENCIA_LOG (
    ID BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    from_id BIGINT NOT NULL,
    to_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_idempotency_key ON TRANSFERENCIA_LOG(idempotency_key);
CREATE INDEX idx_created_at ON TRANSFERENCIA_LOG(created_at);
CREATE INDEX idx_status ON TRANSFERENCIA_LOG(status);

COMMENT ON TABLE TRANSFERENCIA_LOG IS 'Log de transferências para controle de idempotência';
COMMENT ON COLUMN TRANSFERENCIA_LOG.idempotency_key IS 'Chave única para garantir idempotência (UUID gerado pelo cliente)';
COMMENT ON COLUMN TRANSFERENCIA_LOG.status IS 'Status: PROCESSANDO, PROCESSADO, ERRO';