-- Tabela de Benefícios com Optimistic Locking
CREATE TABLE BENEFICIO (
    ID BIGSERIAL PRIMARY KEY,
    NOME VARCHAR(100) NOT NULL,
    DESCRICAO VARCHAR(255),
    VALOR DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ATIVO BOOLEAN NOT NULL DEFAULT TRUE,
    VERSION BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_valor_positivo CHECK (VALOR >= 0)
);

-- Índices para performance
CREATE INDEX idx_beneficio_ativo ON BENEFICIO(ATIVO);
CREATE INDEX idx_beneficio_nome ON BENEFICIO(NOME);

COMMENT ON TABLE BENEFICIO IS 'Tabela de benefícios com suporte a Optimistic Locking';
COMMENT ON COLUMN BENEFICIO.VERSION IS 'Versão para Optimistic Locking (previne Lost Update)';