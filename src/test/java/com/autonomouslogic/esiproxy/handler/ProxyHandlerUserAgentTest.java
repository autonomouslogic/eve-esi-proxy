package com.autonomouslogic.esiproxy.handler;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.esiproxy.EveEsiProxy;
import com.autonomouslogic.esiproxy.test.DaggerTestComponent;
import com.autonomouslogic.esiproxy.test.TestHttpUtils;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(30)
@Log4j2
public class ProxyHandlerUserAgentTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	OkHttpClient client;

	@Inject
	@Named("version")
	String version;

	MockWebServer mockEsi;

	@Inject
	protected ProxyHandlerUserAgentTest() {}

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

	@Test
	@SneakyThrows
	void shouldUseConfiguredUserAgent() {
		TestHttpUtils.enqueueResponse(mockEsi, 204);
		var proxyResponse =
				TestHttpUtils.callProxy(client, proxy, "GET", "/esi", Map.of(HeaderNames.USER_AGENT.lowerCase(), ""));
		TestHttpUtils.assertResponse(proxyResponse, 204);

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals(
				1,
				esiRequest
						.getHeaders()
						.values(HeaderNames.USER_AGENT.lowerCase())
						.size());
		TestHttpUtils.assertRequest(
				esiRequest,
				"GET",
				"/esi",
				Map.of(HeaderNames.USER_AGENT.lowerCase(), "test@example.com eve-esi-proxy/" + version));
	}

	@Test
	@SneakyThrows
	void shouldForwardSuppliedUserAgent() {
		TestHttpUtils.enqueueResponse(mockEsi, 204);
		var proxyResponse = TestHttpUtils.callProxy(
				client, proxy, "GET", "/esi", Map.of(HeaderNames.USER_AGENT.lowerCase(), "test-agent"));
		TestHttpUtils.assertResponse(proxyResponse, 204);

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals(
				1,
				esiRequest
						.getHeaders()
						.values(HeaderNames.USER_AGENT.lowerCase())
						.size());
		TestHttpUtils.assertRequest(
				esiRequest,
				"GET",
				"/esi",
				Map.of(HeaderNames.USER_AGENT.lowerCase(), "test-agent test@example.com eve-esi-proxy/" + version));
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "")
	@SneakyThrows
	void shouldForwardSuppliedUserAgentEvenIfOneIsntConfigured() {
		TestHttpUtils.enqueueResponse(mockEsi, 204);
		var proxyResponse = TestHttpUtils.callProxy(
				client, proxy, "GET", "/esi", Map.of(HeaderNames.USER_AGENT.lowerCase(), "test-agent"));
		TestHttpUtils.assertResponse(proxyResponse, 204);

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals(
				1,
				esiRequest
						.getHeaders()
						.values(HeaderNames.USER_AGENT.lowerCase())
						.size());
		TestHttpUtils.assertRequest(
				esiRequest,
				"GET",
				"/esi",
				Map.of(HeaderNames.USER_AGENT.lowerCase(), "test-agent eve-esi-proxy/" + version));
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "")
	@SneakyThrows
	void shouldReturn400IfNoUserAgentAvailable() {
		var proxyResponse =
				TestHttpUtils.callProxy(client, proxy, "GET", "/esi", Map.of(HeaderNames.USER_AGENT.lowerCase(), ""));
		TestHttpUtils.assertResponse(proxyResponse, 400, "User agent must be configured or header supplied");

		TestHttpUtils.assertNoMoreRequests(mockEsi);
	}
}
