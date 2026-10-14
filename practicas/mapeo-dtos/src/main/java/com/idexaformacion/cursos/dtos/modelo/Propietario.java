package com.idexaformacion.cursos.dtos.modelo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

/**
 * Equivale al Owner de petclinic-api, con los mismos campos que usa
 * OwnerResponse. Getters y setters con Lombok: es justo el caso en el que
 * MapStruct necesita lombok-mapstruct-binding (ver pom.xml).
 */
@Entity
@Getter
@Setter
public class Propietario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Version
	private Integer version;

	private String nombre;

	private String apellidos;

	private String direccion;

	private String ciudad;

	private String telefono;

	@OneToOne
	private Usuario usuario;

	@OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL)
	@OrderBy("fechaNacimiento")
	private List<Mascota> mascotas = new ArrayList<>();

}
