package ecommerce.external;

import ecommerce.dto.PagamentoDTO;

public interface IPagamentoExternal
{

	PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal);

	void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId);
}
