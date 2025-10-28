package ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Cliente
{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nome;

	private Regiao regiao;

	@Enumerated(EnumType.STRING) // Armazenar o enum como String no banco
	private TipoCliente tipo;

	public Cliente()
	{
	}

	public Cliente(Long id, String nome, Regiao regiao, TipoCliente tipo)
	{
		this.id = id;
		this.nome = nome;
		this.regiao = regiao;
		this.tipo = tipo;
	}

	// Getters e Setters
	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getNome()
	{
		return nome;
	}

	public void setNome(String nome)
	{
		this.nome = nome;
	}

	public Regiao getRegiao()
	{
		return regiao;
	}

	public void setRegiao(Regiao regiao)
	{
		this.regiao = regiao;
	}

	public TipoCliente getTipo()
	{
		return tipo;
	}

	public void setTipo(TipoCliente tipo)
	{
		this.tipo = tipo;
	}
}
