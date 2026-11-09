package br.edu.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
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
    private static final String TRANSACAO_ID = "tx-123";
    private static final String NUMERO_PEDIDO = "PED-1";

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

    @Test
    void deveConfirmarCompraAprovadaENotificarCliente() {
        BigDecimal total = new BigDecimal("59.70");
        aprovarCompra(3, total);

        ResultadoCheckout resultado = servico.finalizar(solicitacao(3, PRECO));

        assertEquals(new ResultadoCheckout(NUMERO_PEDIDO, total, true), resultado);
        verify(notificador, times(1)).enviarConfirmacao(EMAIL, NUMERO_PEDIDO, total);
        verify(estoqueGateway, never()).liberar(SKU, 3);
    }

    @Test
    void deveExecutarEfeitosDaCompraAprovadaNaOrdemDoContrato() {
        BigDecimal total = new BigDecimal("59.70");
        aprovarCompra(3, total);

        servico.finalizar(solicitacao(3, PRECO));

        InOrder ordem = inOrder(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
        ordem.verify(estoqueGateway, times(1)).reservar(SKU, 3);
        ordem.verify(pagamentoGateway, times(1)).cobrar(CLIENTE_ID, total);
        ordem.verify(pedidoRepository, times(1)).salvar(any(PedidoConfirmado.class));
        ordem.verify(notificador, times(1)).enviarConfirmacao(EMAIL, NUMERO_PEDIDO, total);
    }

    private void aprovarCompra(int quantidade, BigDecimal total) {
        when(estoqueGateway.reservar(SKU, quantidade)).thenReturn(true);
        when(pagamentoGateway.cobrar(CLIENTE_ID, total))
                .thenReturn(new Pagamento(true, TRANSACAO_ID));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class)))
                .thenReturn(NUMERO_PEDIDO);
    }

    private SolicitacaoCheckout solicitacao(int quantidade, BigDecimal precoUnitario) {
        return new SolicitacaoCheckout(CLIENTE_ID, EMAIL, SKU, quantidade, precoUnitario);
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
