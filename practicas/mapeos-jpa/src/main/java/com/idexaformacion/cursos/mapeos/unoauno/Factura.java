package com.idexaformacion.cursos.mapeos.unoauno;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

/**
 * Diapositiva 155: lado PROPIETARIO de la 1:1 (tiene la columna pedido_id).
 *
 * Ojo con la diapositiva: declara a la vez un campo `long pedidoId` con
 * @Column(name = "PEDIDO_ID") y la relacion con @JoinColumn(name = "Pedido_ID").
 * Son DOS mapeos a la misma columna y Hibernate se niega a arrancar
 * ("Column 'pedido_id' is duplicated"). O se quita el campo, o se marca con
 * insertable = false, updatable = false.
 *
 * Diapositiva 166: clave primaria con SEQUENCE y reserva de 50 en 50. Hibernate
 * pide a la secuencia un bloque y asigna 50 identificadores sin volver a la BD,
 * lo que permite agrupar INSERT en lotes (modulo 6, ejercicios 5 y 7).
 */
@Entity
public class Factura {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factura_gen")
	@SequenceGenerator(name = "factura_gen", sequenceName = "factura_seq", allocationSize = 50)
	private Long id;

	private BigDecimal importe;

	@OneToOne(optional = false)
	@JoinColumn(name = "pedido_id", unique = true)
	private Pedido pedido;

	protected Factura() {
	}

	public Factura(BigDecimal importe) {
		this.importe = importe;
	}

	public Long getId() {
		return id;
	}

	public BigDecimal getImporte() {
		return importe;
	}

	public Pedido getPedido() {
		return pedido;
	}

	void setPedido(Pedido pedido) {
		this.pedido = pedido;
	}

}
