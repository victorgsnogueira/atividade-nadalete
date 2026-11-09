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
        BigDecimal total = calcularTotal(solicitacao);
        reservarEstoque(solicitacao);
        Pagamento pagamento = cobrar(solicitacao, total);
        String numeroPedido = registrarPedido(solicitacao, total, pagamento);
        boolean notificacaoEnviada = notificar(solicitacao.email(), numeroPedido, total);
        return new ResultadoCheckout(numeroPedido, total, notificacaoEnviada);
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

    private BigDecimal calcularTotal(SolicitacaoCheckout solicitacao) {
        return solicitacao.precoUnitario()
                .multiply(BigDecimal.valueOf(solicitacao.quantidade()));
    }

    private void reservarEstoque(SolicitacaoCheckout solicitacao) {
        if (!estoqueGateway.reservar(solicitacao.sku(), solicitacao.quantidade())) {
            throw new EstoqueInsuficienteException();
        }
    }

    private Pagamento cobrar(SolicitacaoCheckout solicitacao, BigDecimal total) {
        Pagamento pagamento;
        try {
            pagamento = pagamentoGateway.cobrar(solicitacao.clienteId(), total);
        } catch (FalhaPagamentoException falha) {
            liberarEstoque(solicitacao);
            throw falha;
        }
        if (!pagamento.aprovado()) {
            liberarEstoque(solicitacao);
            throw new PagamentoRecusadoException();
        }
        return pagamento;
    }

    private void liberarEstoque(SolicitacaoCheckout solicitacao) {
        estoqueGateway.liberar(solicitacao.sku(), solicitacao.quantidade());
    }

    private String registrarPedido(
            SolicitacaoCheckout solicitacao, BigDecimal total, Pagamento pagamento) {
        return pedidoRepository.salvar(new PedidoConfirmado(
                solicitacao.clienteId(),
                solicitacao.email(),
                solicitacao.sku(),
                solicitacao.quantidade(),
                total,
                pagamento.transacaoId()));
    }

    private boolean notificar(String email, String numeroPedido, BigDecimal total) {
        try {
            notificador.enviarConfirmacao(email, numeroPedido, total);
            return true;
        } catch (FalhaNotificacaoException falha) {
            return false;
        }
    }
}
