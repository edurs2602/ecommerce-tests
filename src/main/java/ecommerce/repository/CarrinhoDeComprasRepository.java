package ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;

@Repository
public interface CarrinhoDeComprasRepository extends JpaRepository<CarrinhoDeCompras, Long>
{

	Optional<CarrinhoDeCompras> findByIdAndCliente(Long id, Cliente cliente);
}
