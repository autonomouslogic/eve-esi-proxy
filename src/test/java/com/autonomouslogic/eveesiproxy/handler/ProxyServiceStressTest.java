package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.*;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(120)
@Log4j2
public class ProxyServiceStressTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	MockWebServer mockEsi;

	@Inject
	protected ProxyServiceStressTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		mockEsi = new MockWebServer();
		mockEsi.start(MOCK_ESI_PORT);
		proxy.start();
	}

	@AfterEach
	@SneakyThrows
	void stop() {
		proxy.stop();
		mockEsi.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldHandleRequests() {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ZERO));
		for (int i = 0; i < 100; i++) {
			log.info(String.format("Executing request %s", i));
			try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
				assertEquals(204, proxyResponse.code());
			}
		}
		assertEquals(100, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleConcurrentRequests() {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ZERO));
		var p = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
		var n = 1000 / p;

		CompletableFuture.allOf(Stream.iterate(0, j -> j < p, j -> j + 1)
						.map(j -> CompletableFuture.runAsync(() -> {
							for (int i = 0; i < n; i++) {
								log.info(String.format("Executing request %s/%s", j, i));
								try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
									assertEquals(204, proxyResponse.code());
								}
							}
						}))
						.toArray(CompletableFuture[]::new))
				.join();

		assertEquals(p * n, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleErrorRequests() {
		mockEsi.setDispatcher(new ErrorDispatcher());
		for (int i = 0; i < 1; i++) {
			log.info(String.format("Executing request %s", i));
			assertThrows(SocketTimeoutException.class, () -> TestHttpUtils.callProxy(client, proxy, "GET", "/esi")
					.close());
		}
		assertEquals(1, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HTTP_CALL_TIMEOUT", value = "PT0.1S")
	void shouldHandleEsiTimeouts() {
		testTimeouts(10);
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HTTP_CALL_TIMEOUT", value = "PT0.001S")
	void shouldHandleShortEsiTimeouts() {
		testTimeouts(100);
	}

	void testTimeouts(int n) {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ofSeconds(1)));
		for (int i = 0; i < n; i++) {
			log.info(String.format("Executing request %s", i));
			try {
				try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
					assertEquals(500, proxyResponse.code());
				}
			} catch (Exception e) {
				log.info("Request exception", e);
			}
		}
		assertEquals(n, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleClientTimeouts() {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ofSeconds(5)));
		client.newBuilder()
				//			.connectTimeout(Duration.ofMillis(1))
				//			.readTimeout(Duration.ofMillis(1))
				//			.writeTimeout(Duration.ofMillis(1))
				.callTimeout(Duration.ofMillis(1))
				.build();
		for (int i = 0; i < 10; i++) {
			log.info(String.format("Executing request %s", i));
			var start = Instant.now();
			assertThrows(SocketTimeoutException.class, () -> {
				try (var response = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
					log.info("Response: {}", response);
				}
			});
			var time = Duration.between(start, Instant.now());
			assertTrue(time.compareTo(Duration.ofSeconds(1)) <= 0, String.format("%s is longer than 1 second", time));
		}
		assertEquals(10, mockEsi.getRequestCount());
	}

	@RequiredArgsConstructor
	static class DelayDispatcher extends Dispatcher {
		private final Duration delay;

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
			Thread.sleep(delay.toMillis());
			return new MockResponse().setResponseCode(204);
		}
	}

	@RequiredArgsConstructor
	static class ErrorDispatcher extends Dispatcher {
		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
			throw new RuntimeException();
		}
	}
}
