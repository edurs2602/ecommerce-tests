package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

@Service
public class CompraService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

	private static final BigDecimal LIMIAR_10 = new BigDecimal("500.00");
	private static final BigDecimal LIMIAR_20 = new BigDecimal("1000.00");
	private static final BigDecimal DESC_10 = new BigDecimal("0.10");
	private static final BigDecimal DESC_20 = new BigDecimal("0.20");

	private static final int QTD_MIN_5 = 3;
	private static final int QTD_MIN_10 = 5;
	private static final int QTD_MIN_15 = 8;
	private static final BigDecimal DESC_5P  = new BigDecimal("0.05");
	private static final BigDecimal DESC_10P = new BigDecimal("0.10");
	private static final BigDecimal DESC_15P = new BigDecimal("0.15");

	private static final BigDecimal KG_5  = new BigDecimal("5.00");
	private static final BigDecimal KG_10 = new BigDecimal("10.00");
	private static final BigDecimal KG_50 = new BigDecimal("50.00");

	private static final BigDecimal TARIFA_KG_B = new BigDecimal("2.00");
	private static final BigDecimal TARIFA_KG_C = new BigDecimal("4.00");
	private static final BigDecimal TARIFA_KG_D = new BigDecimal("7.00");

	private static final BigDecimal TAXA_MINIMA = new BigDecimal("12.00");
	private static final BigDecimal TAXA_FRAGIL = new BigDecimal("5.00");

	private static final BigDecimal DIVISOR_PESO_CUBICO = new BigDecimal("6000");

	private final CarrinhoDeComprasService cartService;
	private final ClienteService customerService;
	private final IEstoqueExternal stockGateway;
	private final IPagamentoExternal paymentGateway;

	public CompraService(CarrinhoDeComprasService cartService,
						 ClienteService customerService,
						 IEstoqueExternal stockGateway,
						 IPagamentoExternal paymentGateway) {
		this.cartService = cartService;
		this.customerService = customerService;
		this.stockGateway = stockGateway;
		this.paymentGateway = paymentGateway;
	}

	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		return finalizarPedido(carrinhoId, clienteId);
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		return calcularTotalPedido(carrinho, regiao, tipoCliente);
	}

	@Transactional
	public CompraDTO finalizarPedido(Long carrinhoId, Long clienteId) {
		Cliente cliente = customerService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = cartService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> ids = carrinho.getItens().stream()
				.map(i -> i.getProduto().getId())
				.collect(Collectors.toList());

		List<Long> qts = carrinho.getItens().stream()
				.map(ItemCompra::getQuantidade)
				.map(Number::longValue)
				.collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = stockGateway.verificarDisponibilidade(ids, qts);
		if (!Boolean.TRUE.equals(disponibilidade.disponivel())) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal total = calcularTotalPedido(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = paymentGateway.autorizarPagamento(cliente.getId(), total.doubleValue());
		if (!Boolean.TRUE.equals(pagamento.autorizado())) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixa = stockGateway.darBaixa(ids, qts);
		if (!Boolean.TRUE.equals(baixa.sucesso())) {
			paymentGateway.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		return new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");
	}

	public BigDecimal calcularTotalPedido(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		validarEntrada(carrinho, regiao, tipoCliente);

		BigDecimal subtotal = calcularSubtotal(carrinho);
		BigDecimal subtotalAposTipo = aplicarDescontosPorTipo(carrinho, subtotal);
		BigDecimal subtotalFinal = aplicarDescontoPorValor(subtotalAposTipo);

		BigDecimal frete = calcularFreteFinal(carrinho, regiao, tipoCliente);

		return subtotalFinal.add(frete).setScale(2, ROUNDING);
	}

	private BigDecimal calcularSubtotal(CarrinhoDeCompras carrinho) {
		BigDecimal subtotal = ZERO;
		for (ItemCompra item : carrinho.getItens()) {
			Long qtd = item.getQuantidade();
			if (qtd == null || qtd.longValue() <= 0L) {
				throw new IllegalArgumentException("Quantidade deve ser maior que zero");
			}
			Produto p = item.getProduto();
			if (p == null || p.getPreco() == null || p.getPreco().compareTo(ZERO) < 0) {
				throw new IllegalArgumentException("Produto/preço inválido");
			}
			subtotal = subtotal.add(p.getPreco().multiply(BigDecimal.valueOf(qtd.longValue())));
		}
		return subtotal;
	}

	private BigDecimal aplicarDescontosPorTipo(CarrinhoDeCompras carrinho, BigDecimal subtotalGlobal) {
		Map<TipoProduto, BigDecimal> subtotalPorTipo = new EnumMap<>(TipoProduto.class);
		Map<TipoProduto, Integer> qtPorTipo = new EnumMap<>(TipoProduto.class);

		for (ItemCompra item : carrinho.getItens()) {
			Produto p = item.getProduto();
			TipoProduto t = p.getTipo();

			BigDecimal linha = p.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade().longValue()));
			subtotalPorTipo.merge(t, linha, BigDecimal::add);

			int qtdItem = Math.toIntExact(item.getQuantidade().longValue());
			qtPorTipo.merge(t, qtdItem, Integer::sum);
		}

		BigDecimal descontoTotal = ZERO;
		for (Map.Entry<TipoProduto, BigDecimal> e : subtotalPorTipo.entrySet()) {
			int qtd = qtPorTipo.getOrDefault(e.getKey(), 0);
			BigDecimal perc =
					(qtd >= QTD_MIN_15) ? DESC_15P :
							(qtd >= QTD_MIN_10) ? DESC_10P :
									(qtd >= QTD_MIN_5)  ? DESC_5P  : ZERO;

			if (perc.signum() > 0) {
				descontoTotal = descontoTotal.add(e.getValue().multiply(perc));
			}
		}

		return subtotalGlobal.subtract(descontoTotal);
	}

	private BigDecimal aplicarDescontoPorValor(BigDecimal subtotal) {
		BigDecimal perc =
				(subtotal.compareTo(LIMIAR_20) > 0) ? DESC_20 :
						(subtotal.compareTo(LIMIAR_10) > 0) ? DESC_10 : ZERO;

		return (perc.signum() > 0) ? subtotal.subtract(subtotal.multiply(perc)) : subtotal;
	}

	private BigDecimal calcularFreteFinal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		BigDecimal pesoTotal = calcularPesoTributavelTotal(carrinho);
		BigDecimal freteBase = calcularFaixaFrete(pesoTotal);

		BigDecimal taxaManuseioFrageis = calcularTaxaFrageis(carrinho);
		BigDecimal minimo = (freteBase.signum() > 0) ? TAXA_MINIMA : ZERO;
		BigDecimal fatorRegiao = fatorPorRegiao(regiao);

		BigDecimal freteBruto = freteBase.add(minimo).add(taxaManuseioFrageis).multiply(fatorRegiao);
		return aplicarBeneficioNivel(freteBruto, tipoCliente);
	}

	private BigDecimal calcularPesoTributavelTotal(CarrinhoDeCompras carrinho) {
		BigDecimal total = ZERO;
		for (ItemCompra item : carrinho.getItens()) {
			Produto p = item.getProduto();
			if (p.getPesoFisico() == null || p.getPesoFisico().compareTo(ZERO) < 0) {
				throw new IllegalArgumentException("Peso físico não pode ser nulo/negativo");
			}
			BigDecimal fisico = p.getPesoFisico();
			BigDecimal cubico = calcularPesoCubico(p);
			BigDecimal tributavel = fisico.max(cubico);
			total = total.add(tributavel.multiply(BigDecimal.valueOf(item.getQuantidade().longValue())));
		}
		return total;
	}

	private BigDecimal calcularPesoCubico(Produto p) {
		if (p.getComprimento() == null || p.getLargura() == null || p.getAltura() == null) {
			return ZERO;
		}
		if (p.getComprimento().compareTo(ZERO) < 0 || p.getLargura().compareTo(ZERO) < 0 || p.getAltura().compareTo(ZERO) < 0) {
			throw new IllegalArgumentException("Dimensões não podem ser negativas");
		}
		BigDecimal volume = p.getComprimento().multiply(p.getLargura()).multiply(p.getAltura());
		return volume.divide(DIVISOR_PESO_CUBICO, 2, ROUNDING);
	}

	private BigDecimal calcularFaixaFrete(BigDecimal pesoTotal) {
		if (pesoTotal.compareTo(KG_5) <= 0) {
			return ZERO;
		} else if (pesoTotal.compareTo(KG_10) <= 0) {
			return TARIFA_KG_B.multiply(pesoTotal);
		} else if (pesoTotal.compareTo(KG_50) <= 0) {
			return TARIFA_KG_C.multiply(pesoTotal);
		} else {
			return TARIFA_KG_D.multiply(pesoTotal);
		}
	}

	private BigDecimal calcularTaxaFrageis(CarrinhoDeCompras carrinho) {
		BigDecimal taxa = ZERO;
		for (ItemCompra item : carrinho.getItens()) {
			Boolean fragil = item.getProduto().isFragil();
			if (fragil != null && fragil) {
				taxa = taxa.add(TAXA_FRAGIL.multiply(BigDecimal.valueOf(item.getQuantidade().longValue())));
			}
		}
		return taxa;
	}

	private BigDecimal fatorPorRegiao(Regiao regiao) {
		return switch (regiao) {
			case SUDESTE      -> new BigDecimal("1.00");
			case SUL          -> new BigDecimal("1.05");
			case NORDESTE     -> new BigDecimal("1.10");
			case CENTRO_OESTE -> new BigDecimal("1.20");
			case NORTE        -> new BigDecimal("1.30");
		};
	}

	private BigDecimal aplicarBeneficioNivel(BigDecimal frete, TipoCliente tipo) {
		return switch (tipo) {
			case OURO   -> ZERO;
			case PRATA  -> frete.multiply(new BigDecimal("0.50"));
			case BRONZE -> frete;
		};
	}

	private void validarEntrada(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipo) {
		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			throw new IllegalArgumentException("Carrinho não pode ser nulo/vazio");
		}
		if (regiao == null) {
			throw new IllegalArgumentException("Região não pode ser nula");
		}
		if (tipo == null) {
			throw new IllegalArgumentException("Tipo de cliente não pode ser nulo");
		}
	}
}
