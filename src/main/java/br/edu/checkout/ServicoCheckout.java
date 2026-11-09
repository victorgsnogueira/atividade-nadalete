package br.edu.checkout;

import java.math.BigDecimal;
import java.util.Objects;

public final class ServicoCheckout {

    private final EstoqueGateway estoqueGateway;
    private final PagamentoGateway pagamentoGateway;
    private final PedidoRepository pedidoRepository;
    private final Notificador notificador;

    public ServicoCheckout(
            EstoqueGateway estoqueGateway,
            PagamentoGateway pagamentoGateway,
            PedidoRepository pedidoRepository,
            Notificador notificador) {
        this.estoqueGateway = Objects.requireNonNull(estoqueGateway);
        this.pagamentoGateway = Objects.requireNonNull(pagamentoGateway);
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.notificador = Objects.requireNonNull(notificador);
    }

    public ResultadoCheckout finalizar(SolicitacaoCheckout solicitacao) {
        validar(solicitacao);
        BigDecimal total = solicitacao.precoUnitario()
                .multiply(BigDecimal.valueOf(solicitacao.quantidade()));
        estoqueGateway.reservar(solicitacao.sku(), solicitacao.quantidade());
        Pagamento pagamento = pagamentoGateway.cobrar(solicitacao.clienteId(), total);
        String numeroPedido = pedidoRepository.salvar(new PedidoConfirmado(
                solicitacao.clienteId(),
                solicitacao.email(),
                solicitacao.sku(),
                solicitacao.quantidade(),
                total,
                pagamento.transacaoId()));
        notificador.enviarConfirmacao(solicitacao.email(), numeroPedido, total);
        return new ResultadoCheckout(numeroPedido, total, true);
    }

    private void validar(SolicitacaoCheckout solicitacao) {
        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação não pode ser nula");
        }
        if (emBranco(solicitacao.clienteId())
                || emBranco(solicitacao.email())
                || emBranco(solicitacao.sku())) {
            throw new IllegalArgumentException("Cliente, e-mail e SKU são obrigatórios");
        }
        if (solicitacao.quantidade() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        if (solicitacao.precoUnitario() == null
                || solicitacao.precoUnitario().signum() <= 0) {
            throw new IllegalArgumentException("Preço unitário deve ser maior que zero");
        }
    }

    private boolean emBranco(String texto) {
        return texto == null || texto.isBlank();
    }
}
