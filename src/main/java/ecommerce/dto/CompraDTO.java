package ecommerce.dto;

public record CompraDTO(Boolean sucesso, Long transacaoPagamentoId, String mensagem)
{
}
