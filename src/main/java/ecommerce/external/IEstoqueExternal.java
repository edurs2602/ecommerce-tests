package ecommerce.external;

import java.util.List;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;

public interface IEstoqueExternal
{

	public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades);

	public DisponibilidadeDTO verificarDisponibilidade(List<Long> produtosIds, List<Long> produtosQuantidades);

}
