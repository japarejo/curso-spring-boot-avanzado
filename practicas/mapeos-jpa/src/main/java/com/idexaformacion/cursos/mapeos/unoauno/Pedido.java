package com.idexaformacion.cursos.mapeos.unoauno;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * Diapositiva 155. Lado INVERSO de la relacion 1:1: no tiene columna, la clave
 * ajena esta en la tabla factura (mappedBy = "pedido").
 *
 * cascade = ALL: guardar o borrar un pedido guarda o borra su factura.
 */
@Entity
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String referencia;

	@OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private Factura factura;

	protected Pedido() {
	}

	public Pedido(String referencia) {
		this.referencia = referencia;
	}

	/**
	 * Metodo de ayuda que mantiene SINCRONIZADOS los dos lados. JPA solo mira el
	 * lado propietario (Factura.pedido) para escribir la clave ajena; si se
	 * olvida, la factura se guarda sin pedido aunque en memoria parezca bien.
	 */
	public void facturar(Factura factura) {
		this.factura = factura;
		factura.setPedido(this);
	}

	public Long getId() {
		return id;
	}

	public String getReferencia() {
		return referencia;
	}

	public Factura getFactura() {
		return factura;
	}

}
