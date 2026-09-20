package org.springframework.samples.petclinic.apiclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ReactiveApiClientTimingTest {

	private static final int CALLS = 6;

	private static final long API_DELAY_MILLIS = 200;

	private static HttpServer server;

	private static ExecutorService executor;

	private static WebClient webClient;

	@BeforeAll
	static void startSlowBillApi() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/api/v1/bills", exchange -> {
			try {
				Thread.sleep(API_DELAY_MILLIS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			String id = exchange.getRequestURI().getPath().substring("/api/v1/bills/".length());
			writeJson(exchange, """
				{"id":%s,"concept":"Reactive demo","amount":42.0}
				""".formatted(id));
		});
		server.start();
		webClient = WebClient.builder()
			.baseUrl("http://localhost:" + server.getAddress().getPort() + "/api/v1")
			.build();
	}

	@AfterAll
	static void stopSlowBillApi() {
		if (server != null) {
			server.stop(0);
		}
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	@Test
	void parallelReactiveCallsShouldSaveTimeComparedWithSequentialCalls() {
		List<Integer> ids = List.of(1, 2, 3, 4, 5, 6);

		TimedResult<List<Bill>> sequential = time(() -> fetchSequentially(ids).collectList().block(Duration.ofSeconds(5)));
		TimedResult<List<Bill>> parallel = time(() -> fetchInParallel(ids).collectList().block(Duration.ofSeconds(5)));

		printTimingComparison(sequential, parallel);

		assertThat(sequential.result()).hasSize(CALLS);
		assertThat(parallel.result()).extracting(Bill::getId).containsExactlyInAnyOrderElementsOf(ids);
		assertThat(parallel.millis()).isLessThan(sequential.millis() - API_DELAY_MILLIS);
	}

	private Flux<Bill> fetchSequentially(List<Integer> ids) {

		return Flux.fromIterable(ids).concatMap(this::getBill);

	}

	private Flux<Bill> fetchInParallel(List<Integer> ids) {

		return Flux.fromIterable(ids).flatMap(this::getBill, CALLS);

	}

	private Mono<Bill> getBill(Integer id) {
		return webClient.get()
			.uri("/bills/{id}", id)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(Bill.class);
	}

	private static <T> TimedResult<T> time(java.util.function.Supplier<T> supplier) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		T result = supplier.get();
		stopWatch.stop();
		return new TimedResult<>(result, stopWatch.getTotalTimeMillis());
	}

	private static void printTimingComparison(TimedResult<?> sequential, TimedResult<?> parallel) {
		long savedMillis = sequential.millis() - parallel.millis();
		double speedup = (double) sequential.millis() / parallel.millis();

		System.out.println();
		System.out.println("=== Comparacion llamadas API con WebClient ===");
		System.out.printf("Peticiones: %d%n", CALLS);
		System.out.printf("Retardo simulado por peticion: %d ms%n", API_DELAY_MILLIS);
		System.out.printf("Tiempo secuencial: %d ms%n", sequential.millis());
		System.out.printf("Tiempo paralelo:   %d ms%n", parallel.millis());
		System.out.printf("Ahorro aproximado: %d ms%n", savedMillis);
		System.out.printf("Mejora aproximada: %.2fx%n", speedup);
		System.out.println("=============================================");
		System.out.println();
	}

	private static void writeJson(com.sun.net.httpserver.HttpExchange exchange, String json) throws IOException {
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream responseBody = exchange.getResponseBody()) {
			responseBody.write(body);
		}
	}

	private record TimedResult<T>(T result, long millis) {

	}

}
