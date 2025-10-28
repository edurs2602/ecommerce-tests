package ecommerce.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ecommerce.dto.CompraDTO;
import ecommerce.service.CompraService;

@RestController
@RequestMapping("/")
public class CompraController
{

	private final CompraService compraService;

	@Autowired
	public CompraController(CompraService compraService)
	{
		this.compraService = compraService;
	}

	@PostMapping("/finalizar")
	public ResponseEntity<CompraDTO> finalizarCompra(@RequestParam Long carrinhoId, @RequestParam Long clienteId)
	{
		try
		{
			CompraDTO compraDTO = compraService.finalizarCompra(carrinhoId, clienteId);
			return ResponseEntity.ok(compraDTO);
		}
		catch (IllegalArgumentException e)
		{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CompraDTO(false, null, e.getMessage()));
		}
		catch (IllegalStateException e)
		{
			return ResponseEntity.status(HttpStatus.CONFLICT).body(new CompraDTO(false, null, e.getMessage()));
		}
		catch (Exception e)
		{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new CompraDTO(false, null, "Erro ao processar compra."));
		}
	}
}
