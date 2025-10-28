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

@DisplayName("CompraService – Combinações (Tabela T01–T07)")
class CompraServiceCombinationsTest {

    // ===================== Constantes =====================
    private static final BigDecimal ZERO          = new BigDecimal("0.00");

    private static final BigDecimal P100          = new BigDecimal("100.00");
    private static final BigDecimal P200          = new BigDecimal("200.00");
    private static final BigDecimal P600          = new BigDecimal("600.00");
    private static final BigDecimal P1500         = new BigDecimal("1500.00");

    private static final BigDecimal KG_3          = new BigDecimal("3.00");
    private static final BigDecimal KG_5          = new BigDecimal("5.00");
    private static final BigDecimal KG_7          = new BigDecimal("7.00");
    private static final BigDecimal KG_15         = new BigDecimal("15.00");
    private static final BigDecimal KG_27_5       = new BigDecimal("27.50");
    private static final BigDecimal KG_55         = new BigDecimal("55.00");
    private static final BigDecimal KG_MINUS_2    = new BigDecimal("-2.00");

    // Mensagens (ajuste para casar com o texto real lançado pelo service)
    private static final String MSG_PRECO_INVALIDO  = "preço";
    private static final String MSG_PESO_INVALIDO   = "peso";
    private static final String MSG_CLIENTE_NULO    = "cliente";

    // ===================== SUT =====================
    private CompraService service;

    @BeforeEach
    void setUp() {
        this.service = new CompraService(null, null, null, null);
    }

    // ----------------- Helpers -----------------
    private static Produto produto(BigDecimal preco, BigDecimal pesoKg, boolean fragil) {
        Produto p = new Produto();
        p.setPreco(preco);
        p.setPesoFisico(pesoKg);
        p.setTipo(TipoProduto.values()[0]);
        p.setFragil(fragil);
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

    // ----------------- Tabela de Combinações -----------------

    @Test
    @DisplayName("T01 | 100.00; 3kg; Sudeste; Bronze; Fragil=F → Total 100.00")
    void calcularTotal_T01_sudesteBronze_semFragil_semFreteDesconto() {
        CarrinhoDeCompras c = carrinho(item(produto(P100, KG_3, false), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("T02 | 600.00; 7kg; Sul; Prata; Fragil=F → Total 553.65")
    void calcularTotal_T02_sulPrata_desconto10_freteMeio_comMultiplicador() {
        CarrinhoDeCompras c = carrinho(item(produto(P600, KG_7, false), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.SUL, TipoCliente.PRATA);
        assertThat(total).isEqualByComparingTo("553.65");
    }

    @Test
    @DisplayName("T03 | 1500.00; 15kg; Norte; Ouro; Fragil=F → Total 1200.00")
    void calcularTotal_T03_norteOuro_desconto20_freteZerado() {
        CarrinhoDeCompras c = carrinho(item(produto(P1500, KG_15, false), 1));
        BigDecimal total = service.calcularTotalPedido(c, Regiao.NORTE, TipoCliente.OURO);
        assertThat(total).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("T04 | 200.00; 55kg; Centro-Oeste; Bronze; Fragil=T (2 itens) → Total 688.40")
    void calcularTotal_T04_centroOesteBronze_doisItensFragil_freteMaisTaxaComMultiplicador() {
        Produto p1 = produto(P100, KG_27_5, true);
        Produto p2 = produto(P100, KG_27_5, true);
        CarrinhoDeCompras c = carrinho(item(p1, 1), item(p2, 1));

        BigDecimal total = service.calcularTotalPedido(c, Regiao.CENTRO_OESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("688.40");
    }

    @Test
    @DisplayName("T05 | -10.00; 5kg; Sudeste; Bronze; Fragil=F → exceção (preço negativo)")
    void calcularTotal_T05_precoNegativo_lancaExcecaoComMensagem() {
        CarrinhoDeCompras c = carrinho(item(produto(new BigDecimal("-10.00"), KG_5, false), 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PRECO_INVALIDO);
    }

    @Test
    @DisplayName("T06 | 100.00; -2kg; Sudeste; Bronze; Fragil=F → exceção (peso negativo)")
    void calcularTotal_T06_pesoNegativo_lancaExcecaoComMensagem() {
        CarrinhoDeCompras c = carrinho(item(produto(P100, KG_MINUS_2, false), 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_PESO_INVALIDO);
    }

    @Test
    @DisplayName("T07 | 100.00; 5kg; Sudeste; Cliente=null; Fragil=F → exceção (cliente nulo)")
    void calcularTotal_T07_clienteNulo_lancaExcecaoComMensagem() {
        CarrinhoDeCompras c = carrinho(item(produto(P100, KG_5, false), 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularTotalPedido(c, Regiao.SUDESTE, null));
        assertThat(ex.getMessage()).containsIgnoringCase(MSG_CLIENTE_NULO);
    }
}
