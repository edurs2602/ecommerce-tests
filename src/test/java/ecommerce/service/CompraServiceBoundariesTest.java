package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.*;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

@DisplayName("CompraService – Testes de Limite (L01–L16)")
class CompraServiceBoundariesTest {

    // ===================== Constantes (evita números mágicos) =====================
    private static final BigDecimal ZERO          = new BigDecimal("0.00");
    private static final BigDecimal CENTAVO       = new BigDecimal("0.01");
    private static final BigDecimal PRECO_500     = new BigDecimal("500.00");
    private static final BigDecimal PRECO_500_01  = new BigDecimal("500.01");
    private static final BigDecimal PRECO_1000    = new BigDecimal("1000.00");
    private static final BigDecimal PRECO_1000_01 = new BigDecimal("1000.01");

    private static final BigDecimal KG_NEG_0_01   = new BigDecimal("-0.01");
    private static final BigDecimal KG_0          = new BigDecimal("0.00");
    private static final BigDecimal KG_5          = new BigDecimal("5.00");
    private static final BigDecimal KG_5_01       = new BigDecimal("5.01");
    private static final BigDecimal KG_10         = new BigDecimal("10.00");
    private static final BigDecimal KG_10_01      = new BigDecimal("10.01");
    private static final BigDecimal KG_50         = new BigDecimal("50.00");
    private static final BigDecimal KG_50_01      = new BigDecimal("50.01");

    private static final String MSG_QTD_INVALIDA   = "quantidade";
    private static final String MSG_PRECO_INVALIDO = "preço";
    private static final String MSG_PESO_INVALIDO  = "peso";

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
        p.setFragil(Boolean.FALSE);
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

    // ===================== Quantidade (L01–L02) =====================

    @Test
    @DisplayName("L01 | Quantidade = 0 → IllegalArgumentException (limite superior inválido)")
    void quantidade_igualAZero_deveLancarExcecao() {
        CarrinhoDeCompras c = carrinho(item(produto(new BigDecimal("10.00"), KG_0), 0));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_QTD_INVALIDA);
    }

    @Test
    @DisplayName("L02 | Quantidade = 1 → valor válido (limite inferior válido)")
    void quantidade_igualAUm_deveSerValida() {
        CarrinhoDeCompras c = carrinho(item(produto(new BigDecimal("10.00"), KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("10.00");
    }

    // ===================== Subtotal (L03–L08) =====================

    @Test
    @DisplayName("L03 | Subtotal = 0.00 → IllegalArgumentException (preço/subtotal não positivo)")
    void subtotal_igualAZero_deveLancarExcecao() {
        CarrinhoDeCompras c = carrinho(item(produto(ZERO, KG_0), 1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PRECO_INVALIDO);
    }

    @Test
    @DisplayName("L04 | Subtotal = 0.01 → sem desconto (faixa mínima aberta)")
    void subtotal_minimoAposZero_semDesconto() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("L05 | Subtotal = 500.00 → sem desconto (limite superior da faixa sem desconto)")
    void subtotal_igualA500_semDesconto() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_500, KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("L06 | Subtotal = 500.01 → 10% de desconto (limite inferior da faixa 10%)")
    void subtotal_imediatamenteAcimaDe500_desconto10() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_500_01, KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("450.01");
    }

    @Test
    @DisplayName("L07 | Subtotal = 1000.00 → 10% de desconto (limite superior da faixa 10%)")
    void subtotal_igualA1000_desconto10() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_1000, KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("L08 | Subtotal = 1000.01 → 20% de desconto (limite inferior da faixa 20%)")
    void subtotal_imediatamenteAcimaDe1000_desconto20() {
        CarrinhoDeCompras c = carrinho(item(produto(PRECO_1000_01, KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("800.01");
    }

    // ===================== Peso / Frete (L09–L16) =====================

    @Test
    @DisplayName("L09 | Peso = -0.01 → IllegalArgumentException (abaixo do mínimo)")
    void peso_negativoMinimo_deveLancarExcecao() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_NEG_0_01), 1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PESO_INVALIDO);
    }

    @Test
    @DisplayName("L10 | Peso = 0.00 → frete isento (total = subtotal)")
    void peso_zero_freteIsento() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_0), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE); // frete 0
        assertThat(total).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("L11 | Peso = 5.00 → frete isento (limite superior da faixa isenta)")
    void peso_cinco_freteIsento() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_5), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE); // frete 0
        assertThat(total).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("L12 | Peso = 5.01 → frete faixa B (2/kg + 12) → 22.02 + 0.01 = 22.03")
    void peso_cincoMais01_faixaB() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_5_01), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("22.03");
    }

    @Test
    @DisplayName("L13 | Peso = 10.00 → frete faixa B (2/kg + 12) → 32.00 + 0.01 = 32.01")
    void peso_dez_faixaB_limiteSuperior() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_10), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("32.01");
    }

    @Test
    @DisplayName("L14 | Peso = 10.01 → frete faixa C (4/kg + 12) → 52.04 + 0.01 = 52.05")
    void peso_dezMais01_faixaC() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_10_01), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("52.05");
    }

    @Test
    @DisplayName("L15 | Peso = 50.00 → frete faixa C (4/kg + 12) → 212.00 + 0.01 = 212.01")
    void peso_cinquenta_faixaC_limiteSuperior() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_50), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("212.01");
    }

    @Test
    @DisplayName("L16 | Peso = 50.01 → frete faixa D (7/kg + 12) → 362.07 + 0.01 = 362.08")
    void peso_cinquentaMais01_faixaD() {
        CarrinhoDeCompras c = carrinho(item(produto(CENTAVO, KG_50_01), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("362.08");
    }
}
