package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.*;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
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
@Timeout(30)
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
			var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
			assertEquals(204, proxyResponse.code());
		}
		assertEquals(100, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleConcurrentRequests() {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ZERO));

		CompletableFuture.allOf(Stream.iterate(0, j -> j < 8, j -> j + 1)
						.map(j -> CompletableFuture.runAsync(() -> {
							for (int i = 0; i < 100; i++) {
								log.info(String.format("Executing request %s/%s", j, i));
								var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
								assertEquals(204, proxyResponse.code());
							}
						}))
						.toArray(CompletableFuture[]::new))
				.join();

		assertEquals(800, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HTTP_CALL_TIMEOUT", value = "PT0.001S")
	void shouldHandleEsiTimeouts() {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ofSeconds(1)));
		for (int i = 0; i < 100; i++) {
			log.info(String.format("Executing request %s", i));
			try {
				var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
				assertEquals(500, proxyResponse.code());
			} catch (Exception e) {
				log.info("Request exception", e);
			}
		}
		assertEquals(100, mockEsi.getRequestCount());
	}

	@Test
	@SneakyThrows
	void shouldHandleClientTimeouts() {
		mockEsi.setDispatcher(new DelayDispatcher(Duration.ofSeconds(1)));
		client.newBuilder().callTimeout(Duration.ofMillis(1)).build();
		for (int i = 0; i < 100; i++) {
			log.info(String.format("Executing request %s", i));
			assertThrows(TimeoutException.class, () -> TestHttpUtils.callProxy(client, proxy, "GET", "/esi"));
		}
		assertEquals(100, mockEsi.getRequestCount());
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
}
