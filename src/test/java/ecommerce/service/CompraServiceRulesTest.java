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

@DisplayName("CompraService – Regras de Negócio (Rules)")
class CompraServiceRulesTest {

    // ===================== Constantes =====================
    private static final BigDecimal ZERO       = new BigDecimal("0.00");
    private static final BigDecimal CENTAVO    = new BigDecimal("0.01");
    private static final BigDecimal PRECO_500  = new BigDecimal("500.00");
    private static final BigDecimal PRECO_700  = new BigDecimal("700.00");
    private static final BigDecimal PRECO_1200 = new BigDecimal("1200.00");

    private static final BigDecimal PESO_5  = new BigDecimal("5.00");
    private static final BigDecimal PESO_7  = new BigDecimal("7.00");
    private static final BigDecimal PESO_12 = new BigDecimal("12.00");
    private static final BigDecimal PESO_60 = new BigDecimal("60.00");

    private static final String MSG_QTD_INVALIDA   = "quantidade";
    private static final String MSG_PRECO_INVALIDO = "preço";
    private static final String MSG_CLIENTE_NULO   = "cliente";

    // ===================== SUT =====================
    private CompraService service;

    @BeforeEach
    void setUp() {
        this.service = new CompraService(null, null, null, null);
    }

    // ----------------- Helpers -----------------
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

    private static CarrinhoDeCompras carrinho(ItemCompra... itens) {
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(List.of(itens));
        return c;
    }

    // ----------------- SUBTOTAL (R01–R03) -----------------

    @Test
    @DisplayName("R01 | Subtotal > 1000 → aplica 20% de desconto (1200 → 960)")
    void calcularTotal_quandoSubtotalMaiorQue1000_entaoDesconto20() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_1200, ZERO), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("960.00");
    }

    @Test
    @DisplayName("R02 | 500 < Subtotal ≤ 1000 → aplica 10% de desconto (700 → 630)")
    void calcularTotal_quandoSubtotalAte1000_entaoDesconto10() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_700, ZERO), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("630.00");
    }

    @Test
    @DisplayName("R03 | Subtotal ≤ 500 → sem desconto (500 → 500)")
    void calcularTotal_quandoSubtotalAte500_entaoSemDesconto() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_500, ZERO), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("500.00");
    }

    // ----------------- PESO / FRETE (R04–R07) -----------------

    @ParameterizedTest(name = "[{index}] Peso {0}kg → total esperado {1}")
    @MethodSource("dadosFretePorFaixa")
    @DisplayName("R04–R07 | Faixas de peso → aplica fórmula correta")
    void calcularTotal_quandoFaixaDePeso_entaoAplicaFormulaDeFrete(BigDecimal peso, String esperado) {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, peso), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado);
    }

    private static Stream<Arguments> dadosFretePorFaixa() {
        return Stream.of(
                Arguments.of(PESO_5,  "0.01"),
                Arguments.of(PESO_7,  "26.01"),
                Arguments.of(PESO_12, "60.01"),
                Arguments.of(PESO_60, "432.01")
        );
    }

    // ----------------- FRÁGIL (R08–R09) -----------------

    @ParameterizedTest(name = "[{index}] Frágil=true, qtd={0} → total esperado {1}")
    @CsvSource({
            "3, 15.03",
            "2, 10.02",
            "1, 5.01"
    })
    @DisplayName("R08 | Item frágil → adiciona 5.00 por unidade")
    void calcularTotal_quandoFragilTrue_entaoSomaTaxa(long qtd, String esperado) {
        Produto p = produto(CENTAVO, ZERO);
        p.setFragil(Boolean.TRUE);
        CarrinhoDeCompras c = carrinho(item(p, qtd));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado);
    }

    @Test
    @DisplayName("R09 | Item frágil=false → não adiciona taxa")
    void calcularTotal_quandoFragilFalse_entaoNaoSomaTaxa() {
        Produto p = produto(CENTAVO, ZERO);
        p.setFragil(Boolean.FALSE);
        CarrinhoDeCompras c = carrinho(item(p, 2));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("0.02");
    }

    // ----------------- REGIÃO (R10–R14) -----------------

    @ParameterizedTest(name = "[{index}] Região {0} → total esperado {1} (peso 7kg, Bronze)")
    @MethodSource("dadosRegiaoFrete")
    @DisplayName("R10–R14 | Multiplicador por Região aplicado ao frete")
    void calcularTotal_quandoRegiao_aplicaMultiplicador(Regiao regiao, String totalEsperado) {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, PESO_7), 1));
        BigDecimal total = service.calcularTotalPedido(c, regiao, TipoCliente.BRONZE);
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

    // ----------------- CLIENTE (R15–R17) -----------------

    @Test
    @DisplayName("R15 | Cliente Ouro → zera o frete (100% desc.)")
    void calcularTotal_quandoClienteOuro_entaoFreteZero() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, PESO_7), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.OURO);
        assertThat(total).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("R16 | Cliente Prata → 50% de desconto no frete")
    void calcularTotal_quandoClientePrata_entaoMeioFrete() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, PESO_7), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.PRATA);
        assertThat(total).isEqualByComparingTo("13.01");
    }

    @Test
    @DisplayName("R17 | Cliente Bronze → paga frete integral")
    void calcularTotal_quandoClienteBronze_entaoFreteIntegral() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, PESO_7), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("26.01");
    }

    // ----------------- EXCEÇÕES / VALIDAÇÕES (R18–R20) -----------------

    @Test
    @DisplayName("R18 | Quantidade ≤ 0 → lança IllegalArgumentException com mensagem adequada")
    void calcularTotal_quandoQuantidadeMenorOuIgualZero_entaoLancaExcecao() {
        CarrinhoDeCompras c = carrinho(item(produto(new BigDecimal("10.00"), ZERO), 0));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_QTD_INVALIDA);
    }

    @Test
    @DisplayName("R19 | Preço < 0 → lança IllegalArgumentException com mensagem adequada")
    void calcularTotal_quandoPrecoNegativo_entaoLancaExcecao() {
        CarrinhoDeCompras c = carrinho(item(produto(new BigDecimal("-1.00"), ZERO), 1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PRECO_INVALIDO);
    }

    @Test
    @DisplayName("R20 | Cliente nulo → lança IllegalArgumentException com mensagem adequada")
    void calcularTotal_quandoClienteNulo_entaoLancaExcecao() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, PESO_7), 1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, null));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_CLIENTE_NULO);
    }
}
