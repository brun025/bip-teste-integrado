package com.example.ejb;  // ← PACOTE CORRETO (ejb, não backend.ejb)

import com.example.ejb.exception.BeneficioNotFoundException;
import com.example.ejb.exception.SaldoInsuficienteException;
import com.example.ejb.exception.TransferenciaInvalidaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste Unitário para BeneficioEjbService (Sistema Legado).
 *
 * LOCALIZAÇÃO: ejb-module/src/test/java/com/example/ejb/
 *
 * OBJETIVO:
 * - Testar lógica de negócio do EJB isoladamente
 * - Validar regras de transferência (saldo, benefício ativo, etc)
 * - Verificar Optimistic Locking
 * - Testar validações de parâmetros
 *
 * @author Senior Java Developer
 */
@ExtendWith(MockitoExtension.class)
class BeneficioEjbServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BeneficioEjbService ejbService;

    private Beneficio origem;
    private Beneficio destino;

    @BeforeEach
    void setUp() {
        // Benefício origem com saldo suficiente
        origem = new Beneficio(
                "Benefício Origem",
                "Para débito",
                new BigDecimal("1000.00")
        );
        origem.setId(1L);
        origem.setVersion(0L);
        origem.setAtivo(true);

        // Benefício destino
        destino = new Beneficio(
                "Benefício Destino",
                "Para crédito",
                new BigDecimal("500.00")
        );
        destino.setId(2L);
        destino.setVersion(0L);
        destino.setAtivo(true);
    }

    // ==================== TESTES DE TRANSFERÊNCIA BEM-SUCEDIDA ====================

    @Test
    @DisplayName("✅ Deve executar transferência com sucesso")
    void deveExecutarTransferenciaComSucesso() {
        // Arrange
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal saldoOrigemAntes = origem.getValor();
        BigDecimal saldoDestinoAntes = destino.getValor();

        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);
        when(entityManager.merge(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(entityManager).flush();

        // Act
        ejbService.transfer(1L, 2L, valor);

        // Assert: Saldos devem ter mudado
        assertThat(origem.getValor())
                .as("Saldo da origem deve ter sido debitado")
                .isEqualByComparingTo(saldoOrigemAntes.subtract(valor));

        assertThat(destino.getValor())
                .as("Saldo do destino deve ter sido creditado")
                .isEqualByComparingTo(saldoDestinoAntes.add(valor));

        // Assert: Métodos do EntityManager foram chamados
        verify(entityManager).find(Beneficio.class, 1L);
        verify(entityManager).find(Beneficio.class, 2L);
        verify(entityManager, times(2)).merge(any(Beneficio.class));
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("✅ Deve debitar origem e creditar destino corretamente")
    void deveDebitarOrigemECreditarDestinoCorretamente() {
        // Arrange
        BigDecimal valor = new BigDecimal("250.00");

        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);
        when(entityManager.merge(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ejbService.transfer(1L, 2L, valor);

        // Assert
        assertThat(origem.getValor()).isEqualByComparingTo(new BigDecimal("750.00")); // 1000 - 250
        assertThat(destino.getValor()).isEqualByComparingTo(new BigDecimal("750.00")); // 500 + 250
    }

    // ==================== TESTES DE VALIDAÇÃO DE PARÂMETROS ====================

    @Test
    @DisplayName("❌ Deve falhar se fromId é null")
    void deveFalharSeFromIdEhNull() {
        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(null, 2L, new BigDecimal("100.00")))
                .isInstanceOf(TransferenciaInvalidaException.class)
                .hasMessageContaining("origem não pode ser nulo");

        verify(entityManager, never()).find(any(), any());
    }

    @Test
    @DisplayName("❌ Deve falhar se toId é null")
    void deveFalharSeToIdEhNull() {
        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, null, new BigDecimal("100.00")))
                .isInstanceOf(TransferenciaInvalidaException.class)
                .hasMessageContaining("destino não pode ser nulo");

        verify(entityManager, never()).find(any(), any());
    }

    @Test
    @DisplayName("❌ Deve falhar se IDs são iguais")
    void deveFalharSeIdsIguais() {
        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, 1L, new BigDecimal("100.00")))
                .isInstanceOf(TransferenciaInvalidaException.class)
                .hasMessageContaining("mesmo benefício");

        verify(entityManager, never()).find(any(), any());
    }

    @Test
    @DisplayName("❌ Deve falhar se benefício origem não existe")
    void deveFalharSeBeneficioOrigemNaoExiste() {
        // Arrange
        when(entityManager.find(Beneficio.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(999L, 2L, new BigDecimal("100.00")))
                .isInstanceOf(BeneficioNotFoundException.class)
                .hasMessageContaining("999");

        verify(entityManager).find(Beneficio.class, 999L);
    }

    @Test
    @DisplayName("❌ Deve falhar se benefício destino não existe")
    void deveFalharSeBeneficioDestinoNaoExiste() {
        // Arrange
        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, 999L, new BigDecimal("100.00")))
                .isInstanceOf(BeneficioNotFoundException.class)
                .hasMessageContaining("999");

        verify(entityManager).find(Beneficio.class, 1L);
        verify(entityManager).find(Beneficio.class, 999L);
    }

    // ==================== TESTES DE SALDO INSUFICIENTE ====================

    @Test
    @DisplayName("❌ Deve falhar se saldo insuficiente")
    void deveFalharSeSaldoInsuficiente() {
        // Arrange
        BigDecimal valorExcessivo = new BigDecimal("2000.00"); // Maior que saldo (1000)

        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);

        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, 2L, valorExcessivo))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("Saldo insuficiente")
                .hasMessageContaining("1000")
                .hasMessageContaining("2000");

        verify(entityManager, never()).merge(any());
        verify(entityManager, never()).flush();
    }

    @Test
    @DisplayName("✅ Deve permitir transferir saldo completo")
    void devePermitirTransferirSaldoCompleto() {
        // Arrange
        BigDecimal saldoCompleto = origem.getValor(); // 1000.00

        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);
        when(entityManager.merge(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ejbService.transfer(1L, 2L, saldoCompleto);

        // Assert
        assertThat(origem.getValor()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(destino.getValor()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    // ==================== TESTES DE BENEFÍCIO INATIVO ====================

    @Test
    @DisplayName("❌ Deve falhar se benefício origem está inativo")
    void deveFalharSeBeneficioOrigemEstaInativo() {
        // Arrange
        origem.setAtivo(false);

        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);

        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, 2L, new BigDecimal("100.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativo");

        verify(entityManager, never()).merge(any());
    }

    @Test
    @DisplayName("❌ Deve falhar se benefício destino está inativo")
    void deveFalharSeBeneficioDestinoEstaInativo() {
        // Arrange
        destino.setAtivo(false);

        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);

        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, 2L, new BigDecimal("100.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativo");

        verify(entityManager, never()).merge(any());
    }

    // ==================== TESTES DE OPTIMISTIC LOCKING ====================

    @Test
    @DisplayName("🔒 Deve lançar OptimisticLockException em conflito de versão")
    void deveLancarOptimisticLockExceptionEmConflitoDeVersao() {
        // Arrange
        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);
        when(entityManager.find(Beneficio.class, 2L)).thenReturn(destino);
        when(entityManager.merge(any(Beneficio.class))).thenReturn(origem);

        // Simular conflito de versão no flush
        doThrow(new OptimisticLockException("Conflito de versão"))
                .when(entityManager).flush();

        // Act & Assert
        assertThatThrownBy(() -> ejbService.transfer(1L, 2L, new BigDecimal("100.00")))
                .isInstanceOf(OptimisticLockException.class);

        verify(entityManager).flush();
    }

    // ==================== TESTES DE BUSCA DE BENEFÍCIO ====================

    @Test
    @DisplayName("✅ Deve buscar benefício por ID com sucesso")
    void deveBuscarBeneficioPorIdComSucesso() {
        // Arrange
        when(entityManager.find(Beneficio.class, 1L)).thenReturn(origem);

        // Act
        Beneficio resultado = ejbService.findBeneficioById(1L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNome()).isEqualTo("Benefício Origem");

        verify(entityManager).find(Beneficio.class, 1L);
    }

    @Test
    @DisplayName("❌ Deve lançar exceção se benefício não encontrado na busca")
    void deveLancarExcecaoSeBeneficioNaoEncontradoNaBusca() {
        // Arrange
        when(entityManager.find(Beneficio.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> ejbService.findBeneficioById(999L))
                .isInstanceOf(BeneficioNotFoundException.class)
                .hasMessageContaining("999");

        verify(entityManager).find(Beneficio.class, 999L);
    }
}