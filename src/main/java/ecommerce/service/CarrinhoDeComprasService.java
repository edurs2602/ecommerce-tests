package ecommerce.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.repository.CarrinhoDeComprasRepository;

@Service
public class CarrinhoDeComprasService
{
	private final CarrinhoDeComprasRepository repository;

	@Autowired
	public CarrinhoDeComprasService(CarrinhoDeComprasRepository repository)
	{
		this.repository = repository;
	}

	public CarrinhoDeCompras buscarPorCarrinhoIdEClienteId(Long carrinhoId, Cliente cliente)
	{
		return repository.findByIdAndCliente(carrinhoId, cliente)
				.orElseThrow(() -> new IllegalArgumentException("Carrinho não encontrado."));
	}
}
