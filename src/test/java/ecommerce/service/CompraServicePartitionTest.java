package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

@DisplayName("CompraService – Partições e Regras com Boas Práticas")
class CompraServicePartitionTest {

    // ===================== Constantes (evita valores mágicos) =====================
    private static final BigDecimal ZERO       = new BigDecimal("0.00");
    private static final BigDecimal CENTAVO    = new BigDecimal("0.01");
    private static final BigDecimal PRECO_300  = new BigDecimal("300.00");
    private static final BigDecimal PRECO_350  = new BigDecimal("350.00");
    private static final BigDecimal PRECO_1200 = new BigDecimal("1200.00");

    private static final BigDecimal PESO_5  = new BigDecimal("5.00");
    private static final BigDecimal PESO_7  = new BigDecimal("7.00");
    private static final BigDecimal PESO_12 = new BigDecimal("12.00");
    private static final BigDecimal PESO_60 = new BigDecimal("60.00");

    // Mensagens esperadas (ajuste para o texto real do seu service)
    private static final String MSG_PRECO_INVALIDO  = "preço";
    private static final String MSG_PESO_INVALIDO   = "peso";
    private static final String MSG_REGIAO_NULA     = "região";
    private static final String MSG_CLIENTE_NULO    = "cliente";
    private static final String MSG_QTD_INVALIDA    = "quantidade";

    // ===================== SUT =====================
    private CompraService service;

    @BeforeEach
    void setUp() {
        this.service = new CompraService(null, null, null, null);
    }

    // ===================== Helpers =====================
    private static Produto produto(BigDecimal preco, BigDecimal pesoKg) {
        Produto p = new Produto();
        p.setPreco(preco);
        p.setPesoFisico(pesoKg);
        p.setTipo(TipoProduto.values()[0]);
        return p;
    }

    private static ItemCompra item(Produto p, long qtd) {
        ItemCompra i = new ItemCompra();
        i.setProduto(p);
        i.setQuantidade(qtd);
        return i;
    }

    private static CarrinhoDeCompras carrinho(List<ItemCompra> itens) {
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(itens);
        return c;
    }

    // ===== Helper para testes de Região (usa peso 7kg para garantir frete > 0) =====
    private BigDecimal totalFretePorRegiao(Regiao regiao) {
        Produto p = produto(CENTAVO, PESO_7);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));
        return service.calcularTotalPedido(c, regiao, TipoCliente.BRONZE);
    }

    // ===================== SUBTOTAL (P1–P4) =====================

    @Test
    @DisplayName("P1 | Subtotal com preço <= 0 → inválida (preços devem ser positivos)")
    void calcularTotal_quandoPrecoNegativo_entaoLancaExcecao() {
        Produto p = produto(new BigDecimal("-1.00"), ZERO);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PRECO_INVALIDO);
    }

    @Test
    @DisplayName("P2 | Subtotal em (0..500] → sem desconto (300 → 300)")
    void calcularTotal_quandoSubtotalAte500_entaoSemDesconto() {
        Produto p = produto(PRECO_300, ZERO);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("P3 | Subtotal em (500..1000] → desconto 10% (700 → 630)")
    void calcularTotal_quandoSubtotalAte1000_entaoDesconto10() {
        Produto p = produto(PRECO_350, ZERO);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 2)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("630.00");
    }

    @Test
    @DisplayName("P4 | Subtotal > 1000 → desconto 20% (1200 → 960)")
    void calcularTotal_quandoSubtotalMaiorQue1000_entaoDesconto20() {
        Produto p = produto(PRECO_1200, ZERO);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("960.00");
    }

    // ===================== PESO / FRETE (P5–P9) =====================

    @Test
    @DisplayName("P5 | Peso Total < 0 → inválida (peso não pode ser negativo)")
    void calcularTotal_quandoPesoNegativo_entaoLancaExcecao() {
        Produto p = produto(CENTAVO, new BigDecimal("-0.10"));
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PESO_INVALIDO);
    }

    @ParameterizedTest(name = "[{index}] Peso {0}kg → total frete esperado {1}")
    @MethodSource("dadosFaixasPeso")
    @DisplayName("P6–P9 | Faixas de peso → regra de frete aplicada")
    void calcularTotal_quandoFaixaDePeso_entaoAplicaRegra(BigDecimal pesoKg, String totalEsperado) {
        Produto p = produto(CENTAVO, pesoKg);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(totalEsperado);
    }

    private static Stream<Arguments> dadosFaixasPeso() {
        return Stream.of(
                Arguments.of(PESO_5,  "0.01"),
                Arguments.of(PESO_7,  "26.01"),
                Arguments.of(PESO_12, "60.01"),
                Arguments.of(PESO_60, "432.01")
        );
    }

    // ===================== REGIÃO (P10–P15) =====================

    @ParameterizedTest(name = "[{index}] Região {0} → total esperado {1} (peso 7kg, Bronze)")
    @MethodSource("dadosRegiaoFrete")
    @DisplayName("P10–P14 | Multiplicador por Região aplicado ao frete")
    void calcularTotal_quandoRegiao_aplicaMultiplicador(Regiao regiao, String totalEsperado) {
        BigDecimal total = totalFretePorRegiao(regiao);
        assertThat(total).isEqualByComparingTo(totalEsperado);
    }

    private static Stream<Arguments> dadosRegiaoFrete() {
        return Stream.of(
                Arguments.of(Regiao.SUDESTE,      "26.01"),
                Arguments.of(Regiao.SUL,          "27.31"),
                Arguments.of(Regiao.NORDESTE,     "28.61"),
                Arguments.of(Regiao.CENTRO_OESTE, "31.21"),
                Arguments.of(Regiao.NORTE,        "33.81")
        );
    }

    @Test
    @DisplayName("P15 | Região nula → inválida")
    void calcularTotal_quandoRegiaoNula_entaoLancaExcecao() {
        Produto p = produto(CENTAVO, PESO_7);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, null, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_REGIAO_NULA);
    }

    // ===================== CLIENTE (P16–P19) =====================

    @Test
    @DisplayName("P16 | Cliente OURO → 100% de desconto no frete")
    void calcularTotal_quandoClienteOuro_entaoFreteZerado() {
        Produto p = produto(CENTAVO, PESO_7);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.OURO);
        assertThat(total).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("P17 | Cliente PRATA → 50% de desconto no frete")
    void calcularTotal_quandoClientePrata_entaoMeioFrete() {
        Produto p = produto(CENTAVO, PESO_7);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.PRATA);
        assertThat(total).isEqualByComparingTo("13.01");
    }

    @Test
    @DisplayName("P18 | Cliente BRONZE → frete integral")
    void calcularTotal_quandoClienteBronze_entaoFreteIntegral() {
        Produto p = produto(CENTAVO, PESO_7);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("26.01");
    }

    @Test
    @DisplayName("P19 | Cliente nulo → inválida")
    void calcularTotal_quandoClienteNulo_entaoLancaExcecao() {
        Produto p = produto(CENTAVO, PESO_7);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, null));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_CLIENTE_NULO);
    }

    // ===================== FRÁGIL (P20–P21) =====================

    @ParameterizedTest(name = "[{index}] Frágil=true, qtd={0} → taxa esperada {1}")
    @CsvSource({
            "1, 5.01",
            "2, 10.02",
            "3, 15.03"
    })
    @DisplayName("P20 | Frágil TRUE → adiciona taxa 5.00 por unidade")
    void calcularTotal_quandoFragilTrue_entaoSomaTaxa(long qtd, String esperado) {
        Produto p = produto(CENTAVO, ZERO);
        p.setFragil(Boolean.TRUE);
        CarrinhoDeCompras c = carrinho(List.of(item(p, qtd)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado);
    }

    @Test
    @DisplayName("P21 | Frágil FALSE → sem taxa adicional")
    void calcularTotal_quandoFragilFalse_entaoNaoSomaTaxa() {
        Produto p = produto(CENTAVO, ZERO);
        p.setFragil(Boolean.FALSE);

        CarrinhoDeCompras c = carrinho(List.of(item(p, 2)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("0.02");
    }

    // ===================== QUANTIDADE (P22–P23) =====================

    @Test
    @DisplayName("P22 | Quantidade <= 0 → inválida")
    void calcularTotal_quandoQuantidadeMenorOuIgualZero_entaoLancaExcecao() {
        Produto p = produto(new BigDecimal("10.00"), ZERO);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 0)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_QTD_INVALIDA);
    }

    @Test
    @DisplayName("P23 | Quantidade > 0 → cálculo normal")
    void calcularTotal_quandoQuantidadeValida_entaoCalculaSubtotal() {
        Produto p = produto(new BigDecimal("10.00"), ZERO);
        CarrinhoDeCompras c = carrinho(List.of(item(p, 1)));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("10.00");
    }
}
