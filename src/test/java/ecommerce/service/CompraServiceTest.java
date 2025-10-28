package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

public class CompraServiceTest {

	@Test
	public void calcularCustoTotal() {
		CompraService service = new CompraService(null, null, null, null);

		// Pega um tipo válido do enum (evita depender de nomes específicos)
		TipoProduto tipoQualquer = TipoProduto.values()[0];

		// Produto com preço 0 e peso 0 (válidos) para não gerar custo nem frete
		Produto p = new Produto();
		p.setPreco(new BigDecimal("0.00"));
		p.setPesoFisico(new BigDecimal("0.00"));
		p.setTipo(tipoQualquer);

		ItemCompra item1 = new ItemCompra();
		item1.setProduto(p);
		item1.setQuantidade(1L);

		ItemCompra item2 = new ItemCompra();
		item2.setProduto(p);
		item2.setQuantidade(1L);

		ItemCompra item3 = new ItemCompra();
		item3.setProduto(p);
		item3.setQuantidade(1L);

		List<ItemCompra> itens = new ArrayList<>();
		itens.add(item1);
		itens.add(item2);
		itens.add(item3);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		carrinho.setItens(itens);

		// Se você manteve o alias calcularCustoTotal na service:
		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.OURO);
		// Caso não tenha o alias, use:
		// BigDecimal custoTotal = service.calcularTotalPedido(carrinho, Regiao.NORDESTE, TipoCliente.OURO);

		BigDecimal esperado = new BigDecimal("0.00");
		assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);
		assertThat(custoTotal).as("Custo Total da Compra").isEqualByComparingTo("0.0");
	}
}
