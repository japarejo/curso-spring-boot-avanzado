package org.springframework.samples.petclinic.apiclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.apiclients.BillClient;
import org.springframework.samples.petclinic.apiclients.BillsFeignGateway;
import org.springframework.samples.petclinic.apiclients.BillsGateway;
import org.springframework.samples.petclinic.apiclients.BillsRestClientGateway;
import org.springframework.samples.petclinic.apiclients.BillsRestTemplateGateway;
import org.springframework.samples.petclinic.configuration.BillsProperties;
import org.springframework.samples.petclinic.configuration.GenericIdToEntityConverter;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * MODULO 2 - Las tres implementaciones de BillsGateway contra el mismo servidor.
 *
 * Se levanta un bills-service de mentira (HttpServer del JDK, en memoria) que
 * entiende GET, HEAD, POST, PUT, PATCH y DELETE, y cada prueba se ejecuta tres
 * veces, una por cliente. Si las tres pasan, las tres implementaciones cumplen
 * el mismo contrato: ese es el valor de tener una interfaz comun.
 */
@SpringBootTest(classes = BillsGatewaysTest.GatewaysTestApplication.class)
class BillsGatewaysTest {

	private static final String TOKEN = "Bearer test-token";

	private static final ObjectMapper json = new ObjectMapper();
	private static final Map<Integer, Map<String, Object>> store = new ConcurrentHashMap<>();
	private static final AtomicInteger sequence = new AtomicInteger();
	private static final AtomicReference<String> lastAuthorization = new AtomicReference<>();
	private static final AtomicReference<String> lastMethod = new AtomicReference<>();

	private static HttpServer server;
	private static ExecutorService executor;

	@Autowired
	ApplicationContext context;

	static Stream<Class<? extends BillsGateway>> gateways() {
		return Stream.of(BillsRestTemplateGateway.class, BillsRestClientGateway.class, BillsFeignGateway.class);
	}

	/** Los clientes que admiten PATCH (Feign no, con su cliente HTTP por defecto). */
	static Stream<Class<? extends BillsGateway>> gatewaysConPatch() {
		return Stream.of(BillsRestTemplateGateway.class, BillsRestClientGateway.class);
	}

	@DynamicPropertySource
	static void billApiProperties(DynamicPropertyRegistry registry) {
		startBillApi();
		registry.add("petclinic.bills.base-url", () -> "http://localhost:" + server.getAddress().getPort());
	}

	@BeforeEach
	void resetStore() {
		store.clear();
		sequence.set(0);
		store.put(sequence.incrementAndGet(), new ConcurrentHashMap<>(Map.of("id", 1, "concept", "Consultation", "amount", 35.5)));
		store.put(sequence.incrementAndGet(), new ConcurrentHashMap<>(Map.of("id", 2, "concept", "Vaccination", "amount", 48.0)));
		lastAuthorization.set(null);
	}

	@AfterAll
	static void stopBillApi() {
		if (server != null) {
			server.stop(0);
		}
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gateways")
	void getListaTodasYReenviaAuthorization(Class<? extends BillsGateway> type) {
		List<Bill> bills = context.getBean(type).findAll(TOKEN);

		assertThat(bills).extracting(Bill::getConcept).containsExactlyInAnyOrder("Consultation", "Vaccination");
		assertThat(lastAuthorization).hasValue(TOKEN);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gateways")
	void getPorIdDevuelveOptional(Class<? extends BillsGateway> type) {
		BillsGateway gateway = context.getBean(type);

		assertThat(gateway.findById(1, TOKEN)).get().extracting(Bill::getAmount).isEqualTo(35.5);
		assertThat(gateway.findById(99, TOKEN)).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gateways")
	void headComprueba404SinCuerpo(Class<? extends BillsGateway> type) {
		BillsGateway gateway = context.getBean(type);

		assertThat(gateway.exists(2, TOKEN)).isTrue();
		assertThat(lastMethod).hasValue("HEAD");
		assertThat(gateway.exists(99, TOKEN)).isFalse();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gateways")
	void postCreaYDevuelve201ConLocation(Class<? extends BillsGateway> type) {
		Bill nueva = bill("Surgery", 120.0);

		ResponseEntity<Bill> response = context.getBean(type).create(nueva, TOKEN);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/bills/3");
		assertThat(response.getBody().getId()).isEqualTo(3);
		assertThat(store).containsKey(3);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gateways")
	void putReemplazaCompleta(Class<? extends BillsGateway> type) {
		Bill updated = context.getBean(type).update(1, bill("Revision", 20.0), TOKEN);

		assertThat(updated.getConcept()).isEqualTo("Revision");
		assertThat(lastMethod).hasValue("PUT");
		assertThat(store.get(1)).containsEntry("amount", 20.0);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gatewaysConPatch")
	void patchCambiaSoloLosCamposEnviados(Class<? extends BillsGateway> type) {
		Bill patched = context.getBean(type).patch(2, Map.of("amount", 50.0), TOKEN);

		assertThat(patched.getAmount()).isEqualTo(50.0);
		assertThat(patched.getConcept()).isEqualTo("Vaccination");
		assertThat(lastMethod).hasValue("PATCH");
	}

	@Test
	void feignNoAdmitePatchConSuClientePorDefecto() {
		assertThatThrownBy(() -> context.getBean(BillsFeignGateway.class).patch(2, Map.of("amount", 1.0), TOKEN))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("gateways")
	void deleteBorra(Class<? extends BillsGateway> type) {
		context.getBean(type).delete(2, TOKEN);

		assertThat(lastMethod).hasValue("DELETE");
		assertThat(store).doesNotContainKey(2);
	}

	// --- bills-service de mentira ------------------------------------------------

	private static Bill bill(String concept, double amount) {
		Bill bill = new Bill();
		bill.setConcept(concept);
		bill.setAmount(amount);
		return bill;
	}

	private static void startBillApi() {
		if (server != null) {
			return;
		}
		try {
			server = HttpServer.create(new InetSocketAddress(0), 0);
			executor = Executors.newCachedThreadPool();
			server.setExecutor(executor);
			server.createContext(BillsGateway.BILLS_PATH, BillsGatewaysTest::handle);
			server.start();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot start local bills API", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static void handle(HttpExchange exchange) throws IOException {
		lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
		lastMethod.set(exchange.getRequestMethod());
		Matcher m = Pattern.compile(".*/bills/(\\d+)$").matcher(exchange.getRequestURI().getPath());
		Integer id = m.matches() ? Integer.valueOf(m.group(1)) : null;
		String method = exchange.getRequestMethod();

		if (id == null) {
			switch (method) {
				case "GET" -> write(exchange, 200, store.values());
				case "POST" -> {
					Map<String, Object> body = json.readValue(exchange.getRequestBody(), Map.class);
					int newId = sequence.incrementAndGet();
					body.put("id", newId);
					store.put(newId, new ConcurrentHashMap<>(withoutNulls(body)));
					exchange.getResponseHeaders().add("Location", BillsGateway.BILLS_PATH + "/" + newId);
					write(exchange, 201, body);
				}
				default -> empty(exchange, 405);
			}
			return;
		}
		Map<String, Object> current = store.get(id);
		if (current == null) {
			empty(exchange, 404);
			return;
		}
		switch (method) {
			case "GET" -> write(exchange, 200, current);
			case "HEAD" -> empty(exchange, 200);
			case "PUT" -> {
				Map<String, Object> body = json.readValue(exchange.getRequestBody(), Map.class);
				body.put("id", id);
				store.put(id, new ConcurrentHashMap<>(withoutNulls(body)));
				write(exchange, 200, store.get(id));
			}
			case "PATCH" -> {
				current.putAll(withoutNulls(json.readValue(exchange.getRequestBody(), Map.class)));
				write(exchange, 200, current);
			}
			case "DELETE" -> {
				store.remove(id);
				empty(exchange, 204);
			}
			default -> empty(exchange, 405);
		}
	}

	private static Map<String, Object> withoutNulls(Map<String, Object> map) {
		map.values().removeIf(v -> v == null);
		return map;
	}

	private static void write(HttpExchange exchange, int status, Object body) throws IOException {
		byte[] bytes = json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	private static void empty(HttpExchange exchange, int status) throws IOException {
		exchange.sendResponseHeaders(status, -1);
		exchange.close();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class })
	@EnableFeignClients(clients = BillClient.class)
	@EnableConfigurationProperties(BillsProperties.class)
	@Import({ BillsRestTemplateGateway.class, BillsRestClientGateway.class, BillsFeignGateway.class })
	static class GatewaysTestApplication {

		@Bean
		GenericIdToEntityConverter genericIdToEntityConverter() {
			return new GenericIdToEntityConverter();
		}

	}

}
