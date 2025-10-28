package ecommerce.external.fake;

import org.springframework.stereotype.Service;

import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IPagamentoExternal;

@Service
public class PagamentoSimulado implements IPagamentoExternal
{

	@Override
	public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId)
	{
		// TODO Auto-generated method stub

	}
}
