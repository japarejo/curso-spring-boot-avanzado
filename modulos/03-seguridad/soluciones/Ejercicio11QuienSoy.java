// =============================================================================
// Modulo 3 - Ejercicio 11: quien soy (SecurityContextHolder).
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================

// ---------- web/api/QuienSoyController.java ----------
package org.springframework.samples.petclinic.web.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/yo")
public class QuienSoyController {

	/** 1. Acceso estatico: funciona en cualquier sitio, incluso fuera de un controlador. */
	@GetMapping("/estatico")
	public Map<String, Object> estatico() {
		return describir(SecurityContextHolder.getContext().getAuthentication());
	}

	/** 2. Spring resuelve el parametro a partir del mismo contexto. Mas facil de probar. */
	@GetMapping("/parametro")
	public Map<String, Object> parametro(Authentication authentication) {
		return describir(authentication);
	}

	/**
	 * 3. Solo el "principal". Con usuario anonimo, el principal es la cadena
	 * "anonymousUser", que no es un UserDetails: llega null.
	 */
	@GetMapping("/principal")
	public Map<String, Object> principal(@AuthenticationPrincipal UserDetails usuario) {
		if (usuario == null) {
			return Map.of("usuario", "ninguno (anonimo o principal de otro tipo)");
		}
		return Map.of("usuario", usuario.getUsername(), "autoridades", nombres(usuario.getAuthorities()));
	}

	/** 4. El contexto vive en un ThreadLocal: un hilo nuevo no lo ve. */
	@GetMapping("/hilos")
	public Map<String, Object> hilos() throws Exception {
		ExecutorService normal = Executors.newSingleThreadExecutor();
		ExecutorService delegado = new DelegatingSecurityContextExecutorService(Executors.newSingleThreadExecutor());
		try {
			String enHiloNormal = CompletableFuture.supplyAsync(QuienSoyController::nombreActual, normal).get();
			String enHiloDelegado = CompletableFuture.supplyAsync(QuienSoyController::nombreActual, delegado).get();
			return Map.of("hiloDeLaPeticion", nombreActual(), "hiloNuevo", enHiloNormal,
					"hiloNuevoConDelegado", enHiloDelegado);
		}
		finally {
			normal.shutdown();
			delegado.shutdown();
		}
	}

	private static String nombreActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth == null ? "(contexto vacio)" : auth.getName();
	}

	private Map<String, Object> describir(Authentication auth) {
		if (auth == null) {
			return Map.of("autenticado", false);
		}
		return Map.of(
				"nombre", auth.getName(),
				"autoridades", nombres(auth.getAuthorities()),
				"tipo", auth.getClass().getSimpleName(),
				"autenticado", auth.isAuthenticated());
	}

	private List<String> nombres(java.util.Collection<? extends GrantedAuthority> autoridades) {
		return autoridades.stream().map(GrantedAuthority::getAuthority).toList();
	}

}

// ---------- Prueba: web/security/QuienSoyControllerTests.java ----------
/*
package org.springframework.samples.petclinic.web.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class QuienSoyControllerTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	@WithMockUser(username = "pepe", authorities = "vet")
	void usuarioFicticio() throws Exception {
		mockMvc.perform(get("/api/v1/yo/parametro"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("pepe"))
			.andExpect(jsonPath("$.autoridades[0]").value("vet"));
	}

	// Carga owner1 de la base de datos con el UserDetailsService del contexto
	// (el JdbcUserDetailsManager de SecurityConfiguration).
	@Test
	@WithUserDetails("owner1")
	void usuarioReal() throws Exception {
		mockMvc.perform(get("/api/v1/yo/principal"))
			.andExpect(jsonPath("$.usuario").value("owner1"))
			.andExpect(jsonPath("$.autoridades[0]").value("owner"));
	}

	@Test
	void anonimo() throws Exception {
		mockMvc.perform(get("/api/v1/yo/estatico"))
			.andExpect(jsonPath("$.nombre").value("anonymousUser"))
			.andExpect(jsonPath("$.tipo").value("AnonymousAuthenticationToken"));
	}
}
*/
// La prueba "anonimo" solo pasa mientras /api/** sea publico. Si hiciste el
// ejercicio 2, 6 o 13, esperara un 401: es el momento de comprobarlo.
