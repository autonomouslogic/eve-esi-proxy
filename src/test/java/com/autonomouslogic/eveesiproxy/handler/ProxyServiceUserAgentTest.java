package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;
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

/**
 * Tests user agent handling.
 */
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(30)
@Log4j2
public class ProxyServiceUserAgentTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	@Inject
	@Named("version")
	String version;

	String proxyVersionString;

	MockWebServer mockEsi;

	@Inject
	protected ProxyServiceUserAgentTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		proxyVersionString =
				String.format("eve-esi-proxy/%s (+https://github.com/autonomouslogic/eve-esi-proxy)", version);

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
				TestHttpUtils.callProxy(client, proxy, "GET", "/esi", Map.of(HeaderNames.USER_AGENT.defaultCase(), ""));
		TestHttpUtils.assertResponse(proxyResponse, 204);

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals(
				1,
				esiRequest
						.getHeaders()
						.values(HeaderNames.USER_AGENT.defaultCase())
						.size());
		TestHttpUtils.assertRequest(
				esiRequest,
				"GET",
				"/esi",
				Map.of(HeaderNames.USER_AGENT.lowerCase(), "test@example.com " + proxyVersionString));
	}

	@ParameterizedTest
	@ValueSource(strings = {"header", "x-header", "query"})
	@SneakyThrows
	void shouldForwardSuppliedUserAgent(String userAgentSource) {
		TestHttpUtils.enqueueResponse(mockEsi, 204);
		var headers = new HashMap<String, String>();
		var path = "/esi";
		switch (userAgentSource) {
			case "header":
				headers.put(HeaderNames.USER_AGENT.defaultCase(), "test-agent");
				break;
			case "x-header":
				headers.put(HeaderNames.USER_AGENT.defaultCase(), "");
				headers.put("X-User-Agent", "test-agent");
				break;
			case "query":
				headers.put(HeaderNames.USER_AGENT.defaultCase(), "");
				path += "?user_agent=test-agent";
				break;
		}
		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", path, headers);
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
				path,
				Map.of(HeaderNames.USER_AGENT.lowerCase(), "test-agent test@example.com " + proxyVersionString));
	}

	@Test
	@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "")
	@SneakyThrows
	void shouldForwardSuppliedUserAgentEvenIfOneIsntConfigured() {
		TestHttpUtils.enqueueResponse(mockEsi, 204);
		var proxyResponse = TestHttpUtils.callProxy(
				client, proxy, "GET", "/esi", Map.of(HeaderNames.USER_AGENT.defaultCase(), "test-agent"));
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
				Map.of(HeaderNames.USER_AGENT.lowerCase(), "test-agent " + proxyVersionString));
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
