package ecommerce.dto;

import java.util.List;

public record DisponibilidadeDTO(Boolean disponivel, List<Long> idsProdutosIndisponiveis)
{
}
