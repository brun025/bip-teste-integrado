package com.example.backend.enums;

public enum TransferenciaStatus {

    /**
     * Transferência em processamento.
     * Log criado antes da execução.
     */
    PROCESSANDO,

    /**
     * Transferência concluída com sucesso.
     * Valor transferido entre benefícios.
     */
    PROCESSADO,

    /**
     * Transferência falhou.
     * Rollback executado, valor não transferido.
     */
    ERRO
}
