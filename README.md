E-commerce Test - Trabalho de Testes de Software
================================================

**Disciplina:** Testes de Software - Prof. Eiji Adachi**Projeto:** Testes Automatizados para Funcionalidade de Finalização de Compra**Autor:** Gabriel Viana e Luís Eduardo

1\. Sobre o Projeto
-------------------

Este projeto implementa testes de unidade automatizados para o método calcularTotalPedido da camada de serviço (CompraService) de um e-commerce. O objetivo é garantir a corretude do cálculo do custo total da compra, aplicando de forma isolada as regras de negócio de subtotal, descontos, frete e taxas.

O projeto segue as diretrizes da disciplina, aplicando técnicas de teste funcional (Caixa-Preta) e estrutural (Caixa-Branca) , com documentação completa do projeto de testes e cobertura de 100% das arestas do método.

*   **artifactId no pom.xml:** LuisEduardo-GabrielGuilherme
    

2\. Como Executar o Projeto
---------------------------

Instruções para compilar, testar e verificar a cobertura do projeto via Maven.

### Pré-requisitos

*   Java 17 ou superior
    
*   Maven 3.6 ou superior
    

### Compilar o Projeto
```bash
mvn clean compile
```

### Executar os Testes
```bash
mvn test
```

### Verificar a Cobertura de Testes
```bash
mvn verify
```

Após a execução, abra o relatório em:
```bash
target/site/jacoco/index.html
```

3\. Estratégia de Teste
-----------------------

A funcionalidade foi testada de forma isolada (CompraService). A suíte de testes foi dividida em quatro classes distintas para organizar os critérios de teste, conforme a estrutura do projeto:

*   **Testes Funcionais (Caixa-Preta):** Foram aplicadas as técnicas de Particionamento de Equivalência, Análise de Valor Limite e Tabela de Decisão para validar o comportamento esperado com base nos requisitos .
    
*   **Testes Estruturais (Caixa-Branca):** Foi utilizado o relatório de cobertura JaCoCo para garantir **100% de cobertura de arestas (branch coverage)**.
    
*   **Testes de Robustez:** Foram criados testes para entradas inválidas (ex: valores nulos, negativos ou zero) usando assertThrows para validar as exceções e mensagens de erro.
    

4\. Projeto de Teste (Caixa-Preta)
----------------------------------

Documentação do projeto dos casos de teste, cobrindo todas as regras do enunciado.

### Tabelas construídas por planilhas:

[Link para Google Sheets](https://docs.google.com/spreadsheets/d/1M_4ro6gJri6yaBOAaYHF9XISpxCgZpx_zFgmhvUCyOU/edit?gid=0#gid=0)

5\. Análise Estrutural (Caixa-Branca)
-------------------------------------

Esta seção detalha a análise do código-fonte do método calcularTotalPedido e seus métodos privados para garantir a cobertura estrutural .

### 5.1. Grafo de Fluxo de Controle (CFG)

Abaixo está a representação textual do fluxo de controle, listando todos os nós de decisão (loops, if/else, ternários, switch) encontrados no código-fonte.

1.  **calcularTotalPedido**
    
2.  \-> **validarEntrada**
    
    *   (Nó 1 - Decisão) if (carrinho == null || ... || itens.isEmpty())
        
    *   (Nó 2 - Decisão) if (regiao == null)
        
    *   (Nó 3 - Decisão) if (tipo == null)
        
3.  \-> **calcularSubtotal**
    
    *   (Nó 4 - Decisão) for (ItemCompra item : ...)
        
    *   (Nó 5 - Decisão) if (qtd == null || ...)
        
    *   (Nó 6 - Decisão) if (p == null || p.getPreco() == null || ...)
        
4.  \-> **aplicarDescontosPorTipo**
    
    *   (Nó 7 - Decisão) for (ItemCompra item : ...)
        
    *   (Nó 8 - Decisão) for (Map.Entry e : ...)
        
    *   (Nó 9 - Decisão) (qtd >= QTD\_MIN\_15) ? ...
        
    *   (Nó 10 - Decisão) : (qtd >= QTD\_MIN\_10) ? ...
        
    *   (Nó 11 - Decisão) : (qtd >= QTD\_MIN\_5) ? ...
        
    *   (Nó 12 - Decisão) if (perc.signum() > 0)
        
5.  \-> **aplicarDescontoPorValor**
    
    *   (Nó 13 - Decisão) (subtotal.compareTo(LIMIAR\_20) > 0) ? ...
        
    *   (Nó 14 - Decisão) : (subtotal.compareTo(LIMIAR\_10) > 0) ? ...
        
    *   (Nó 15 - Decisão) if (perc.signum() > 0)
        
6.  \-> **calcularFreteFinal**
    
7.  \-> **calcularPesoTributavelTotal**
    
    *   (Nó 16 - Decisão) for (ItemCompra item : ...)
        
    *   (Nó 17 - Decisão) if (p.getPesoFisico() == null || ...)
        
8.  \-> **calcularPesoCubico**
    
    *   (Nó 18 - Decisão) if (p.getComprimento() == null || ...)
        
    *   (Nó 19 - Decisão) if (p.getComprimento().compareTo(ZERO) < 0 || ...)
        
9.  \-> **calcularFaixaFrete**
    
    *   (Nó 20 - Decisão) if (pesoTotal.compareTo(KG\_5) <= 0)
        
    *   (Nó 21 - Decisão) else if (pesoTotal.compareTo(KG\_10) <= 0)
        
    *   (Nó 22 - Decisão) else if (pesoTotal.compareTo(KG\_50) <= 0)
        
10.  \-> **calcularTaxaFrageis**
    
    *   (Nó 23 - Decisão) for (ItemCompra item : ...)
        
    *   (Nó 24 - Decisão) if (fragil != null && fragil)
        
11.  \-> **fatorPorRegiao**
    
    *   (Nó 25 - Decisão) switch (regiao) (Casos: SUL, NORDESTE, CENTRO\_OESTE, NORTE) - 4 decisões
        
12.  \-> **aplicarBeneficioNivel**
    
    *   (Nó 26 - Decisão) switch (tipo) (Casos: PRATA, BRONZE) - 2 decisões
        
13.  \-> **calcularFreteFinal (continua)**
    
    *   (Nó 27 - Decisão) if (freteBase.signum() > 0)
        
14.  \-> **calcularTotalPedido (continua)**
    
15.  \-> **FIM**
    

### 5.2. Complexidade Ciclomática (V(G))

A Complexidade Ciclomática é calculada como V(G) = D + 1, onde D é o número total de pontos de decisão no grafo.

*   **Total de Decisões (D):**
    
    *   validarEntrada: 3
        
    *   calcularSubtotal: 3
        
    *   aplicarDescontosPorTipo: 1 (for) + 1 (for) + 3 (ternário) + 1 (if) = 6
        
    *   aplicarDescontoPorValor: 2 (ternário) + 1 (if) = 3
        
    *   calcularPesoTributavelTotal: 1 (for) + 1 (if) = 2
        
    *   calcularPesoCubico: 2
        
    *   calcularFaixaFrete: 3 (if-else if-else)
        
    *   calcularTaxaFrageis: 1 (for) + 1 (if) = 2
        
    *   fatorPorRegiao: 4 (switch 5 casos, N-1)
        
    *   aplicarBeneficioNivel: 2 (switch 3 casos, N-1)
        
    *   calcularFreteFinal: 1 (ternário minimo = ...)
        
*   **Total (D):** 3 + 3 + 6 + 3 + 2 + 2 + 3 + 2 + 4 + 2 + 1 = **31**
    
*   **V(G) = 31 + 1 = 32**
    

**Número Mínimo de Testes:** São necessários no mínimo **32** casos de teste independentes para atingir 100% de cobertura de arestas. A suíte de testes implementada (Partition, Boundaries, Rules, Combinations) excede esse número, garantindo a cobertura.

### 5.3. Análise MC/DC

Análise da decisão composta mais complexa do código, localizada em validarEntrada:

**Decisão Analisada:** if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty())

*   **C1:** carrinho == null
    
*   **C2:** carrinho.getItens() == null
    
*   **C3:** carrinho.getItens().isEmpty()
    

**Tabela MC/DC (Operador OR, N+1 testes):**

Este conjunto de 4 testes prova que cada condição (C1, C2, C3) pode, independentemente, alterar o resultado da decisão de Falso (T1) para Verdadeiro (T2, T3, T4), satisfazendo o critério MC/DC.

6\. Boas Práticas Adotadas
--------------------------

O projeto de testes seguiu as boas práticas recomendadas no enunciado :

*   **Nomenclatura Descritiva:** Os métodos de teste usam @DisplayName para descrever o cenário e o resultado esperado (ex: L01 | Quantidade = 0 → IllegalArgumentException...).
    
*   **@BeforeEach:** Utilizado em todas as classes de teste para inicializar o CompraService (setUp()), garantindo isolamento entre os testes.
    
*   **AssertJ para BigDecimal:** Todas as asserções de BigDecimal usam assertThat(total).isEqualByComparingTo("123.45") para evitar problemas de escala .
    
*   **Sem "Valores Mágicos":** Constantes foram declaradas no topo de cada classe de teste (ex: PRECO\_500, KG\_5\_01) para melhorar a legibilidade e manutenção.
    
*   **@ParameterizedTest:** Testes parametrizados foram usados extensivamente em CompraServicePartitionTest e CompraServiceRulesTest para testar faixas de peso e regras de região, reduzindo a duplicação de código.
    
*   **Testes de Exceção:** assertThrows é usado para validar não apenas que a exceção correta é lançada (IllegalArgumentException), mas também que a **mensagem de erro** contém a causa raiz (ex: "peso", "preço", "cliente").
    
*   **Mensagens de Falha:** As asserções utilizam o método .as() (exigido no enunciado ), embora a combinação de @DisplayName e testes atômicos já torne o diagnóstico de falhas imediato.