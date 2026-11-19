package org.springframework.samples.petclinic.roles.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La matriz completa con la herencia de privilegios ACTIVADA (configuracion por
 * defecto). Se autentica con httpBasic de verdad, no con {@code @WithMockUser}:
 * asi pasa por {@code UsuarioDetailsService} y por la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatrizDeAccesoTests {

	@Autowired
	MockMvc mvc;

	@ParameterizedTest(name = "{0} {1} {2} -> {3}")
	@CsvSource(delimiter = '|', textBlock = """
			# usuario | metodo | ruta              | estado
			# --- por ROL + jerarquia: todos son, como minimo, EMPLEADO ---
			eva       | GET    | /api/turnos       | 200
			rita      | GET    | /api/turnos       | 200
			victor    | GET    | /api/turnos       | 200
			admin     | GET    | /api/turnos       | 200
			# --- MASCOTA_LEER: privilegio de EMPLEADO, heredado por todos ---
			eva       | GET    | /api/mascotas     | 200
			rita      | GET    | /api/mascotas     | 200
			victor    | GET    | /api/mascotas     | 200
			admin     | GET    | /api/mascotas     | 200
			# --- MASCOTA_EDITAR: solo la rama de RECEPCION ---
			eva       | POST   | /api/mascotas     | 403
			rita      | POST   | /api/mascotas     | 201
			victor    | POST   | /api/mascotas     | 403
			pablo     | POST   | /api/mascotas     | 201
			admin     | POST   | /api/mascotas     | 201
			# --- VISITA_REGISTRAR: solo la rama de VETERINARIO ---
			eva       | POST   | /api/visitas      | 403
			rita      | POST   | /api/visitas      | 403
			victor    | POST   | /api/visitas      | 201
			pablo     | POST   | /api/visitas      | 201
			admin     | POST   | /api/visitas      | 201
			# --- FACTURA_VER ---
			victor    | GET    | /api/facturas     | 403
			rita      | GET    | /api/facturas     | 200
			admin     | GET    | /api/facturas     | 200
			# --- regla por RUTA: /api/admin/** exige ROLE_ADMIN ---
			pablo     | GET    | /api/admin/roles  | 403
			admin     | GET    | /api/admin/roles  | 200
			""")
	void matriz(String usuario, String metodo, String ruta, int estado) throws Exception {
		mvc.perform(request(HttpMethod.valueOf(metodo), ruta)
				.with(httpBasic(usuario, "demo"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "Kira (gato)", "mascota": "Leo (gato)", "motivo": "vacuna"}
						"""))
				.andExpect(status().is(estado));
	}

	@Test
	void sinCredencialesEs401YNo403() throws Exception {
		mvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized());
	}

	@Test
	void elEndpointYoEnsenaLoGuardadoYLoEfectivo() throws Exception {
		mvc.perform(get("/api/yo").with(httpBasic("rita", "demo")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rolesAsignados", containsInAnyOrder("ROLE_RECEPCION")))
				.andExpect(jsonPath("$.privilegios",
						containsInAnyOrder("FACTURA_VER", "MASCOTA_EDITAR", "MASCOTA_LEER")))
				.andExpect(jsonPath("$.autoridadesEfectivas", containsInAnyOrder(
						"ROLE_RECEPCION", "ROLE_EMPLEADO", "FACTURA_VER", "MASCOTA_EDITAR", "MASCOTA_LEER")));
	}

}
