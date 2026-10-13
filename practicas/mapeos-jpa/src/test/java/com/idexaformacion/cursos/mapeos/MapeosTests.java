package com.idexaformacion.cursos.mapeos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.idexaformacion.cursos.mapeos.herencia.Trabajador;
import com.idexaformacion.cursos.mapeos.herencia.TrabajadorFijo;
import com.idexaformacion.cursos.mapeos.herencia.TrabajadorPorHoras;
import com.idexaformacion.cursos.mapeos.jointable.Articulo;
import com.idexaformacion.cursos.mapeos.muchosamuchos.Desarrollador;
import com.idexaformacion.cursos.mapeos.muchosamuchos.Tarea;
import com.idexaformacion.cursos.mapeos.reflexivas.Carpeta;
import com.idexaformacion.cursos.mapeos.reflexivas.Investigador;
import com.idexaformacion.cursos.mapeos.unoamuchos.Departamento;
import com.idexaformacion.cursos.mapeos.unoamuchos.Empleado;
import com.idexaformacion.cursos.mapeos.unoauno.Factura;
import com.idexaformacion.cursos.mapeos.unoauno.Pedido;

/**
 * Una clase anidada por diapositiva. Todas siguen el mismo patron:
 *
 *   1. construir los objetos en memoria y persistirlos;
 *   2. flush() + clear(): escribir en la BD y VACIAR el contexto de persistencia,
 *      para que la lectura siguiente vaya a la base de datos de verdad y no
 *      devuelva los mismos objetos que hay en memoria;
 *   3. leer y comprobar.
 *
 * Sin el paso 2, muchas de estas pruebas pasarian aunque el mapeo estuviera mal.
 * Ejercicio: quita el clear() de una prueba, rompe el mapeo y compruebalo.
 *
 * Con la consola abierta se ve el SQL de cada paso.
 */
@DataJpaTest
@DisplayName("Mapeos JPA de las transparencias")
class MapeosTests {

	@Autowired
	TestEntityManager em;

	private void escribirYOlvidar() {
		em.flush();
		em.clear();
	}

	private long contar(String sqlNativo) {
		return ((Number) em.getEntityManager().createNativeQuery(sqlNativo).getSingleResult()).longValue();
	}

	@Nested
	@DisplayName("1:1 Pedido - Factura (155) y clave con SEQUENCE (166)")
	class UnoAUno {

		@Test
		void guardarElPedidoGuardaSuFacturaEnCascada() {
			Pedido pedido = new Pedido("P-001");
			pedido.facturar(new Factura(new BigDecimal("99.90")));
			Long id = em.persistAndGetId(pedido, Long.class);
			escribirYOlvidar();

			Pedido leido = em.find(Pedido.class, id);
			assertThat(leido.getFactura()).isNotNull();
			assertThat(leido.getFactura().getImporte()).isEqualByComparingTo("99.90");
			assertThat(leido.getFactura().getPedido().getId()).isEqualTo(id);
		}

		@Test
		void lasFacturasTomanIdentificadoresDeLaSecuencia() {
			Pedido a = new Pedido("A");
			a.facturar(new Factura(BigDecimal.ONE));
			Pedido b = new Pedido("B");
			b.facturar(new Factura(BigDecimal.TEN));
			em.persist(a);
			em.persist(b);
			em.flush();

			// Consecutivos: el segundo sale del bloque de 50 ya reservado, sin
			// volver a preguntar a la secuencia. Mira la consola: un solo
			// "select next value for factura_seq".
			assertThat(b.getFactura().getId()).isEqualTo(a.getFactura().getId() + 1);
		}

	}

	@Nested
	@DisplayName("1:N Departamento - Empleado (156)")
	class UnoAMuchos {

		@Test
		void losEmpleadosSeGuardanConSuDepartamento() {
			Departamento it = new Departamento("Sistemas");
			it.contratar(new Empleado("Ana", 2500));
			it.contratar(new Empleado("Luis", 2300));
			Long id = em.persistAndGetId(it, Long.class);
			escribirYOlvidar();

			assertThat(em.find(Departamento.class, id).getEmpleados())
				.extracting(Empleado::getNombre)
				.containsExactlyInAnyOrder("Ana", "Luis");
		}

		@Test
		void orphanRemovalBorraAlEmpleadoQueSaleDeLaLista() {
			Departamento it = new Departamento("Sistemas");
			Empleado luis = new Empleado("Luis", 2300);
			it.contratar(new Empleado("Ana", 2500));
			it.contratar(luis);
			em.persist(it);
			em.flush();

			it.despedir(luis);
			escribirYOlvidar();

			assertThat(contar("select count(*) from empleado")).isEqualTo(1);
		}

		@Test
		void noHayTablaDeUnionSoloUnaClaveAjena() {
			// Si se quita mappedBy en Departamento.empleados, aparece la tabla
			// departamento_empleados y esta prueba falla.
			long tablas = contar("select count(*) from information_schema.tables "
					+ "where lower(table_name) like 'departamento_%'");
			assertThat(tablas).isZero();
		}

	}

	@Nested
	@DisplayName("N:M Desarrollador - Tarea (157)")
	class MuchosAMuchos {

		@Test
		void laRelacionSeLeeDesdeLosDosLados() {
			Tarea api = em.persist(new Tarea("API de pedidos"));
			Tarea web = em.persist(new Tarea("Web"));
			Desarrollador eva = new Desarrollador("Eva");
			eva.asignar(api);
			eva.asignar(web);
			em.persist(eva);
			escribirYOlvidar();

			Tarea leida = em.find(Tarea.class, api.getId());
			assertThat(leida.getDesarrolladores()).extracting(Desarrollador::getNombre).containsExactly("Eva");
			assertThat(contar("select count(*) from desarrollador_tareas")).isEqualTo(2);
		}

		@Test
		void modificarSoloElLadoInversoNoSeGuarda() {
			Tarea api = em.persist(new Tarea("API"));
			Desarrollador eva = em.persist(new Desarrollador("Eva"));
			// Solo el lado inverso (mappedBy): JPA lo ignora al escribir.
			api.getDesarrolladores().add(eva);
			escribirYOlvidar();

			assertThat(contar("select count(*) from desarrollador_tareas")).isZero();
		}

	}

	@Nested
	@DisplayName("Herencia en una sola tabla (158)")
	class Herencia {

		@Test
		void unaConsultaPolimorficaDevuelveLasDosSubclases() {
			em.persist(new TrabajadorFijo("Marta", 2400));
			em.persist(new TrabajadorPorHoras("Pablo", 15f, 80));
			escribirYOlvidar();

			List<Trabajador> todos = em.getEntityManager()
				.createQuery("select t from Trabajador t order by t.nombre", Trabajador.class)
				.getResultList();

			assertThat(todos).hasExactlyElementsOfTypes(TrabajadorFijo.class, TrabajadorPorHoras.class);
			assertThat(todos).extracting(Trabajador::costeMensual).containsExactly(2400.0, 1200.0);
		}

		@Test
		void todoVaAUnaTablaConColumnaDiscriminadora() {
			em.persist(new TrabajadorFijo("Marta", 2400));
			escribirYOlvidar();

			Object tipo = em.getEntityManager().createNativeQuery("select tipo from trabajador").getSingleResult();
			assertThat(tipo).isEqualTo("FIJO");
		}

	}

	@Nested
	@DisplayName("Dos N:M a la misma entidad con @JoinTable (167-168)")
	class JoinTables {

		@Test
		void autoresYRevisoresVanATablasDistintas() {
			Investigador ana = em.persist(new Investigador("Ana"));
			Investigador ben = em.persist(new Investigador("Ben"));
			Articulo articulo = new Articulo("Spring Boot en produccion");
			articulo.getAutores().add(ana);
			articulo.getRevisores().add(ben);
			Long id = em.persistAndGetId(articulo, Long.class);
			escribirYOlvidar();

			Articulo leido = em.find(Articulo.class, id);
			assertThat(leido.getAutores()).extracting(Investigador::getNombre).containsExactly("Ana");
			assertThat(leido.getRevisores()).extracting(Investigador::getNombre).containsExactly("Ben");
			assertThat(contar("select count(*) from articulo_autores")).isEqualTo(1);
			assertThat(contar("select count(*) from articulo_revisores")).isEqualTo(1);
		}

	}

	@Nested
	@DisplayName("Relaciones reflexivas (169-170)")
	class Reflexivas {

		@Test
		void unArbolDeCarpetas() {
			Carpeta raiz = new Carpeta("curso");
			Carpeta modulos = raiz.crear("modulos");
			Carpeta jpa = modulos.crear("06-spring-data-jpa");
			em.persist(raiz);   // la cascada guarda todo el arbol
			escribirYOlvidar();

			Carpeta leida = em.find(Carpeta.class, jpa.getId());
			assertThat(leida.ruta()).isEqualTo("/curso/modulos/06-spring-data-jpa");
			assertThat(em.find(Carpeta.class, raiz.getId()).getHijas()).hasSize(1);
		}

		@Test
		void directoresYDoctorandos() {
			Investigador directora = em.persist(new Investigador("Directora"));
			Investigador doctorando = new Investigador("Doctorando");
			doctorando.dirigidoPor(directora);
			em.persist(doctorando);
			escribirYOlvidar();

			assertThat(em.find(Investigador.class, directora.getId()).getDoctorandos())
				.extracting(Investigador::getNombre)
				.containsExactly("Doctorando");
		}

	}

}
