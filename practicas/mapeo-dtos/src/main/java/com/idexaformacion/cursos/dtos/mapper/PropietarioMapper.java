package com.idexaformacion.cursos.dtos.mapper;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.idexaformacion.cursos.dtos.dto.MascotaResponse;
import com.idexaformacion.cursos.dtos.dto.PropietarioRequest;
import com.idexaformacion.cursos.dtos.dto.PropietarioResponse;
import com.idexaformacion.cursos.dtos.dto.PropietarioResumen;
import com.idexaformacion.cursos.dtos.modelo.Mascota;
import com.idexaformacion.cursos.dtos.modelo.Propietario;

/**
 * Solo se declara QUE se convierte en QUE. MapStruct escribe la clase
 * PropietarioMapperImpl al compilar: abrela en
 * target/generated-sources/annotations/ y comparala con OwnerResponse.from().
 *
 * Los campos con el mismo nombre se copian solos. Solo se anota lo que cambia.
 *
 * componentModel = SPRING: la implementacion lleva @Component y se inyecta
 * como cualquier otro bean.
 *
 * unmappedTargetPolicy = ERROR: si un campo del DESTINO se queda sin rellenar,
 * no compila. Asi, si alguien anade un campo al DTO y se olvida del mapper, se
 * entera al compilar y no en produccion con un null. Por defecto es solo un
 * aviso. Lo que sobra en el origen (password, propietario...) no da error.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PropietarioMapper {

	// --- Entidad -> DTO --------------------------------------------------------------

	/**
	 * Todo se copia por nombre salvo username, que esta un nivel mas abajo. Si
	 * usuario es null, username queda a null: MapStruct genera la comprobacion
	 * (el ternario que en OwnerResponse.from() se escribe a mano).
	 *
	 * La lista de mascotas se convierte sola usando toResponse(Mascota).
	 */
	@Mapping(target = "username", source = "usuario.username")
	PropietarioResponse toResponse(Propietario propietario);

	/**
	 * dateFormat: LocalDate -> String con ese patron.
	 * qualifiedByName: el valor sale de un metodo propio (edad, abajo).
	 */
	@Mapping(target = "fechaNacimiento", dateFormat = "dd/MM/yyyy")
	@Mapping(target = "edad", source = "fechaNacimiento", qualifiedByName = "edad")
	MascotaResponse toResponse(Mascota mascota);

	/**
	 * Campos que no existen en la entidad. expression mete codigo Java tal cual
	 * en la clase generada: util para cosas cortas, pero el compilador no lo
	 * revisa hasta generar el codigo. Para algo mas largo, mejor un metodo
	 * @Named como edad().
	 */
	@Mapping(target = "nombreCompleto", expression = "java(propietario.getNombre() + \" \" + propietario.getApellidos())")
	@Mapping(target = "numeroMascotas", expression = "java(propietario.getMascotas().size())")
	PropietarioResumen toResumen(Propietario propietario);

	/** Las listas se mapean elemento a elemento con el metodo de arriba. */
	List<PropietarioResumen> toResumenes(List<Propietario> propietarios);

	/**
	 * A los metodos propios MapStruct les pasa el valor tal cual, aunque sea
	 * null: la comprobacion es cosa nuestra.
	 */
	@Named("edad")
	default Integer edad(LocalDate fechaNacimiento) {
		return fechaNacimiento == null ? null : Period.between(fechaNacimiento, LocalDate.now()).getYears();
	}

	// --- DTO -> entidad --------------------------------------------------------------

	/**
	 * Alta. Lo que el request no trae hay que ignorarlo de forma explicita; con
	 * unmappedTargetPolicy = ERROR, si no, no compila. Es justo lo que interesa:
	 * queda escrito que el cliente NO decide id, version, usuario ni mascotas.
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "mascotas", ignore = true)
	Propietario toEntity(PropietarioRequest request);

	/**
	 * Modificacion parcial (PATCH) sobre una entidad que ya existe.
	 *
	 * @MappingTarget: no crea un objeto nuevo, escribe en el que se le pasa.
	 * IGNORE: un campo null en el request no machaca el valor actual.
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "mascotas", ignore = true)
	void actualizar(PropietarioRequest request, @MappingTarget Propietario propietario);

}
