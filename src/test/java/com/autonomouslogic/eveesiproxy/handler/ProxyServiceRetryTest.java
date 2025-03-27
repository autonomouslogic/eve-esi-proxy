package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.time.Instant;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "HTTP_RETRY_DELAY", value = "PT0S")
@Timeout(30)
@Log4j2
public class ProxyServiceRetryTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	MockWebServer mockEsi;

	@Inject
	protected ProxyServiceRetryTest() {}

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
		try {
			TestHttpUtils.assertNoMoreRequests(mockEsi);
		} finally {
			proxy.stop();
			mockEsi.shutdown();
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {500, 503, 504})
	@SneakyThrows
	void shouldRetryServerErrors(int status) {
		TestHttpUtils.enqueueResponse(mockEsi, status, "Error");
		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body");

		try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
			TestHttpUtils.assertResponse(proxyResponse, 200, "Test body");
		}

		for (int i = 0; i < 2; i++) {
			assertNotNull(TestHttpUtils.takeRequest(mockEsi));
		}
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HTTP_MAX_TRIES", value = "3")
	void shouldNotTryMoreThanConfigured() {
		for (int i = 0; i < 3; i++) {
			TestHttpUtils.enqueueResponse(mockEsi, 500, "Timeout");
		}

		try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
			TestHttpUtils.assertResponse(proxyResponse, 500, "Timeout");
		}

		for (int i = 0; i < 3; i++) {
			assertNotNull(TestHttpUtils.takeRequest(mockEsi));
		}
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "HTTP_RETRY_DELAY", value = "PT2S")
	void shouldDelayRetries() {
		TestHttpUtils.enqueueResponse(mockEsi, 500, "Error");
		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body");

		var start = Instant.now();
		try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi")) {
			var time = Duration.between(start, Instant.now());
			assertEquals(2.0, time.toMillis() / 1000.0, 100.0);
		}

		for (int i = 0; i < 2; i++) {
			assertNotNull(TestHttpUtils.takeRequest(mockEsi));
		}
	}
}
