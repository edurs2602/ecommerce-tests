# E-Commerce – CompraService

Serviço para calcular o total do pedido (subtotal, frete por região/peso, desconto por tipo de cliente e taxa por item frágil). Testes em **JUnit 5** com **AssertJ**.

## Como Executar o Projeto

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6 ou superior

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

## Observações
- Os testes usam AssertJ para comparar BigDecimal:
  assertThat(total).isEqualByComparingTo("123.45");
- Evite “números mágicos” nos testes; prefira constantes e @BeforeEach.
- Para rodar via IDE (IntelliJ/VS Code), selecione JDK 17 como SDK do projeto.
