package com.idexaformacion.cursos.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.idexaformacion.cursos.dtos.dto.MascotaResponse;
import com.idexaformacion.cursos.dtos.dto.PropietarioRequest;
import com.idexaformacion.cursos.dtos.dto.PropietarioResponse;
import com.idexaformacion.cursos.dtos.dto.PropietarioResumen;
import com.idexaformacion.cursos.dtos.mapper.PropietarioMapper;
import com.idexaformacion.cursos.dtos.modelo.Mascota;
import com.idexaformacion.cursos.dtos.modelo.Propietario;
import com.idexaformacion.cursos.dtos.modelo.Usuario;

/**
 * Prueba unitaria pura: sin Spring ni base de datos. Mappers.getMapper()
 * instancia la clase generada directamente. Funciona porque este mapper no
 * depende de otros beans.
 */
@DisplayName("PropietarioMapper (codigo generado por MapStruct)")
class PropietarioMapperTests {

	private final PropietarioMapper mapper = Mappers.getMapper(PropietarioMapper.class);

	private static Propietario george() {
		Usuario usuario = new Usuario();
		usuario.setUsername("george");
		usuario.setPassword("secreto");

		Propietario propietario = new Propietario();
		propietario.setId(1);
		propietario.setVersion(3);
		propietario.setNombre("George");
		propietario.setApellidos("Franklin");
		propietario.setDireccion("110 W. Liberty St.");
		propietario.setCiudad("Madison");
		propietario.setTelefono("6085551023");
		propietario.setUsuario(usuario);

		Mascota leo = new Mascota();
		leo.setId(10);
		leo.setNombre("Leo");
		leo.setFechaNacimiento(LocalDate.now().minusYears(3).minusDays(1));
		leo.setPropietario(propietario);
		propietario.getMascotas().add(leo);
		return propietario;
	}

	@Test
	@DisplayName("Copia por nombre y aplana usuario.username")
	void entidadADetalle() {
		PropietarioResponse dto = mapper.toResponse(george());

		assertThat(dto.id()).isEqualTo(1);
		assertThat(dto.nombre()).isEqualTo("George");
		assertThat(dto.direccion()).isEqualTo("110 W. Liberty St.");
		assertThat(dto.username()).isEqualTo("george");
	}

	@Test
	@DisplayName("Sin usuario, username queda a null (sin NullPointerException)")
	void sinUsuario() {
		Propietario propietario = george();
		propietario.setUsuario(null);

		assertThat(mapper.toResponse(propietario).username()).isNull();
	}

	@Test
	@DisplayName("Las mascotas se convierten con su propio metodo: fecha formateada y edad calculada")
	void mascotasAnidadas() {
		Propietario propietario = george();
		LocalDate nacimiento = propietario.getMascotas().get(0).getFechaNacimiento();

		List<MascotaResponse> mascotas = mapper.toResponse(propietario).mascotas();

		assertThat(mascotas).singleElement().satisfies(m -> {
			assertThat(m.nombre()).isEqualTo("Leo");
			assertThat(m.fechaNacimiento()).isEqualTo(
				"%02d/%02d/%d".formatted(nacimiento.getDayOfMonth(), nacimiento.getMonthValue(), nacimiento.getYear()));
			assertThat(m.edad()).isEqualTo(3);
		});
	}

	@Test
	@DisplayName("El resumen tiene campos que no existen en la entidad")
	void resumen() {
		List<PropietarioResumen> resumenes = mapper.toResumenes(List.of(george()));

		assertThat(resumenes).containsExactly(new PropietarioResumen(1, "George Franklin", "Madison", 1));
	}

	@Test
	@DisplayName("Alta: el request no puede fijar id ni version")
	void requestAEntidad() {
		Propietario nuevo = mapper.toEntity(
			new PropietarioRequest("Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654"));

		assertThat(nuevo.getId()).isNull();
		assertThat(nuevo.getVersion()).isNull();
		assertThat(nuevo.getNombre()).isEqualTo("Jean");
		assertThat(nuevo.getTelefono()).isEqualTo("6085552654");
	}

	@Test
	@DisplayName("PATCH: solo cambia lo que llega; los null no machacan nada")
	void actualizacionParcial() {
		Propietario propietario = george();

		mapper.actualizar(new PropietarioRequest(null, null, null, null, "600000000"), propietario);

		assertThat(propietario.getTelefono()).isEqualTo("600000000");
		assertThat(propietario.getNombre()).isEqualTo("George");
		assertThat(propietario.getCiudad()).isEqualTo("Madison");
		assertThat(propietario.getId()).isEqualTo(1);
		assertThat(propietario.getVersion()).isEqualTo(3);
	}

}
