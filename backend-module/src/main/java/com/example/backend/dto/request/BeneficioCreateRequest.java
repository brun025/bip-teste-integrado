package com.example.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Dados para criação de um novo benefício")
public class BeneficioCreateRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome do benefício", example = "Vale Alimentação", required = true)
    private String nome;

    @Size(max = 255, message = "Descrição não pode exceder 255 caracteres")
    @Schema(description = "Descrição do benefício", example = "Benefício para compra de alimentos")
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.00", inclusive = true, message = "Valor não pode ser negativo")
    @Digits(integer = 13, fraction = 2, message = "Valor deve ter no máximo 13 dígitos inteiros e 2 decimais")
    @Schema(description = "Valor inicial do benefício", example = "1500.00", required = true)
    private BigDecimal valor;

    @Schema(description = "Indica se o benefício está ativo", example = "true", defaultValue = "true")
    private Boolean ativo = true;

    public BeneficioCreateRequest() {
    }

    public BeneficioCreateRequest(String nome, String descricao, BigDecimal valor) {
        this.nome = nome;
        this.descricao = descricao;
        this.valor = valor;
        this.ativo = true;
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

    @Override
    public String toString() {
        return "BeneficioCreateRequest{" +
                "nome='" + nome + '\'' +
                ", valor=" + valor +
                ", ativo=" + ativo +
                '}';
    }
}