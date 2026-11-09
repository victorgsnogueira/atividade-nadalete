package br.edu.checkout;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ServicoCheckoutTest {

    private static final String CLIENTE_ID = "cliente-1";
    private static final String EMAIL = "cliente@loja.com";
    private static final String SKU = "SKU-100";
    private static final BigDecimal PRECO = new BigDecimal("19.90");

    @Mock
    private EstoqueGateway estoqueGateway;

    @Mock
    private PagamentoGateway pagamentoGateway;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private Notificador notificador;

    private ServicoCheckout servico;

    @BeforeEach
    void criarServicoReal() {
        servico = new ServicoCheckout(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveRejeitarSolicitacaoNulaSemConsultarColaboradores() {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(null));
        verifyNoInteractions(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @ParameterizedTest
    @MethodSource("solicitacoesInvalidas")
    void deveRejeitarSolicitacaoInvalidaSemConsultarColaboradores(
            SolicitacaoCheckout solicitacao) {
        assertThrows(
                IllegalArgumentException.class, () -> servico.finalizar(solicitacao));
        verifyNoInteractions(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    static Stream<SolicitacaoCheckout> solicitacoesInvalidas() {
        return Stream.of(
                new SolicitacaoCheckout(null, EMAIL, SKU, 1, PRECO),
                new SolicitacaoCheckout("", EMAIL, SKU, 1, PRECO),
                new SolicitacaoCheckout("   ", EMAIL, SKU, 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, null, SKU, 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, "", SKU, 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, "   ", SKU, 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, null, 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, "", 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, "   ", 1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, SKU, 0, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, SKU, -1, PRECO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, SKU, 1, null),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, SKU, 1, BigDecimal.ZERO),
                new SolicitacaoCheckout(CLIENTE_ID, EMAIL, SKU, 1, new BigDecimal("-0.01")));
    }
}
