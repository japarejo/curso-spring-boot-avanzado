// =============================================================================
// Modulo 3 - Ejercicio 9: codificacion de contrasenas.
//
// Prueba lista para ejecutar. Copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/web/security/
// y lanzar:
//   ./mvnw -pl apps/petclinic-api test -Dtest=Ejercicio09CodificacionContrasenasTests
//
// No necesita Spring: son pruebas unitarias puras sobre spring-security-crypto.
// =============================================================================
package org.springframework.samples.petclinic.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class Ejercicio09CodificacionContrasenasTests {

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	private final PasswordEncoder delegado = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Test
	@DisplayName("La misma contrasena da dos hashes distintos: cada uno lleva su propia sal")
	void laSalHaceQueCadaHashSeaDistinto() {
		String primero = bcrypt.encode("0wn3r");
		String segundo = bcrypt.encode("0wn3r");

		System.out.println(primero);
		System.out.println(segundo);
		assertThat(primero).isNotEqualTo(segundo);
		// Formato: $2a$ (variante) 10$ (coste) + 22 caracteres de sal + 31 de hash
		assertThat(primero).startsWith("$2a$10$").hasSize(60);
	}

	@Test
	@DisplayName("matches() reconoce la contrasena en cualquiera de sus hashes")
	void matchesFuncionaConCualquierHash() {
		String hash = bcrypt.encode("0wn3r");

		assertThat(bcrypt.matches("0wn3r", hash)).isTrue();
		assertThat(bcrypt.matches("0WN3R", hash)).isFalse();
	}

	@Test
	@DisplayName("Cada punto de coste duplica el tiempo de calculo")
	void elCosteEsExponencial() {
		for (int coste = 10; coste <= 13; coste++) {
			BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(coste);
			long inicio = System.nanoTime();
			encoder.encode("0wn3r");
			long ms = (System.nanoTime() - inicio) / 1_000_000;
			System.out.printf("coste %d -> %d ms%n", coste, ms);
		}
		// No hay asercion: el tiempo depende de la maquina. Se lee la salida.
		// Regla practica: elegir el coste que tarde unos 250 ms en el servidor real.
	}

	@Test
	@DisplayName("El delegado elige el algoritmo por el prefijo guardado")
	void elDelegadoLeeElPrefijo() {
		String nuevo = delegado.encode("prueba");

		assertThat(nuevo).startsWith("{bcrypt}");
		assertThat(delegado.matches("prueba", nuevo)).isTrue();
		assertThat(delegado.matches("prueba", "{noop}prueba")).isTrue();
	}

	@Test
	@DisplayName("upgradeEncoding() senala las contrasenas guardadas con un algoritmo antiguo")
	void detectaLoQueHayQueMigrar() {
		assertThat(delegado.upgradeEncoding("{noop}prueba")).isTrue();
		assertThat(delegado.upgradeEncoding(delegado.encode("prueba"))).isFalse();
	}

	@Test
	@DisplayName("Genera la linea para data.sql")
	void generaLineaParaDataSql() {
		System.out.printf("INSERT INTO users(username,password,enabled) VALUES ('prueba','%s',TRUE);%n",
				delegado.encode("prueba"));
	}

}
