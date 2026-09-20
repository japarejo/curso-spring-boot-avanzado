package org.springframework.samples.petclinic.apiclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.samples.petclinic.configuration.GenericIdToEntityConverter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.springframework.samples.petclinic.apiclients.BillClient;
import org.springframework.samples.petclinic.model.Bill;

@SpringBootTest(classes = BillApiClientTest.FeignClientTestApplication.class)
class BillApiClientTest {

	private static final AtomicReference<String> receivedAuthorizationHeader = new AtomicReference<>();

	private static HttpServer server;

	private static ExecutorService executor;

	@Autowired
	BillClient billClient;

	@DynamicPropertySource
	static void billApiProperties(DynamicPropertyRegistry registry) {
		startBillApi();
		registry.add("bills.api.url", () -> "http://localhost:" + server.getAddress().getPort());
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

	@Test
	void feignClientShouldCallBillsMicroserviceAndDeserializeResponse() {
		List<Bill> bills = billClient.getBills("Bearer test-token");

		assertThat(receivedAuthorizationHeader).hasValue("Bearer test-token");
		assertThat(bills).hasSize(2);
		assertThat(bills.get(0).getId()).isEqualTo(1);
		assertThat(bills.get(0).getConcept()).isEqualTo("Consultation");
		assertThat(bills.get(0).getAmount()).isEqualTo(35.5);
		assertThat(bills.get(1).getId()).isEqualTo(2);
		assertThat(bills.get(1).getConcept()).isEqualTo("Vaccination");
		assertThat(bills.get(1).getAmount()).isEqualTo(48.0);
	}

	private static void startBillApi() {
		if (server != null) {
			return;
		}
		try {
			server = HttpServer.create(new InetSocketAddress(0), 0);
			executor = Executors.newCachedThreadPool();
			server.setExecutor(executor);
			server.createContext("/api/v1/bills", exchange -> {
				receivedAuthorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
				writeJson(exchange, """
					[
					  {"id":1,"concept":"Consultation","amount":35.5},
					  {"id":2,"concept":"Vaccination","amount":48.0}
					]
					""");
			});
			server.start();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot start local bills API", ex);
		}
	}

	private static void writeJson(com.sun.net.httpserver.HttpExchange exchange, String json) throws IOException {
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream responseBody = exchange.getResponseBody()) {
			responseBody.write(body);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, SqlInitializationAutoConfiguration.class })
	@EnableFeignClients(clients = BillClient.class)
	static class FeignClientTestApplication {

		@Bean
		GenericIdToEntityConverter genericIdToEntityConverter() {
			return new GenericIdToEntityConverter();
		}

	}

}
