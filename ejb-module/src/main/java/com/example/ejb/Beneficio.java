package com.example.ejb;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;

import com.example.ejb.exception.SaldoInsuficienteException;

@Entity
@Table(name = "BENEFICIO")
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "Nome do benefício é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Size(max = 255, message = "Descrição não pode exceder 255 caracteres")
    @Column(name = "DESCRICAO", length = 255)
    private String descricao;

    @NotNull(message = "Valor do benefício é obrigatório")
    @DecimalMin(value = "0.00", inclusive = true, message = "Valor não pode ser negativo")
    @Column(name = "VALOR", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "ATIVO")
    private Boolean ativo = true;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public Beneficio() {
        this.ativo = true;
    }

    public Beneficio(String nome, String descricao, BigDecimal valor) {
        this.nome = nome;
        this.descricao = descricao;
        this.valor = valor;
        this.ativo = true;
    }

    // Métodos de negócio
    public void validarAtivo() {
        if (!Boolean.TRUE.equals(this.ativo)) {
            throw new IllegalStateException(
                    String.format("Benefício ID %d está inativo e não pode ser utilizado", this.id)
            );
        }
    }

    public void validarSaldoSuficiente(BigDecimal valor) {
        if (this.valor.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(
                    String.format("Saldo insuficiente. Disponível: %s, Solicitado: %s",
                            this.valor, valor)
            );
        }
    }

    public void debitar(BigDecimal valor) {
        validarAtivo();
        validarSaldoSuficiente(valor);
        this.valor = this.valor.subtract(valor);
    }

    public void creditar(BigDecimal valor) {
        validarAtivo();
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor a creditar deve ser positivo");
        }
        this.valor = this.valor.add(valor);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Beneficio that = (Beneficio) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Beneficio{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", valor=" + valor +
                ", ativo=" + ativo +
                ", version=" + version +
                '}';
    }
}