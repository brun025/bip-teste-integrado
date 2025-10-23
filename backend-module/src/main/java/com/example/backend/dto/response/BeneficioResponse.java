package com.example.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Dados de um benefício")
public class BeneficioResponse {

    @Schema(description = "ID do benefício", example = "1")
    private Long id;

    @Schema(description = "Nome do benefício", example = "Vale Alimentação")
    private String nome;

    @Schema(description = "Descrição do benefício", example = "Benefício para compra de alimentos")
    private String descricao;

    @Schema(description = "Valor atual do benefício", example = "1500.00")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal valor;

    @Schema(description = "Indica se o benefício está ativo", example = "true")
    private Boolean ativo;

    @Schema(description = "Versão para controle de concorrência (Optimistic Locking)", example = "5")
    private Long version;

    public BeneficioResponse() {
    }

    public BeneficioResponse(Long id, String nome, String descricao, BigDecimal valor, Boolean ativo, Long version) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.valor = valor;
        this.ativo = ativo;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}