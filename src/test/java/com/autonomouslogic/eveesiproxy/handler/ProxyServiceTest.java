package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderValues;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(30)
@Log4j2
public class ProxyServiceTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	MockWebServer mockEsi;

	@Inject
	protected ProxyServiceTest() {}

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

	@CartesianTest
	@ValueSource(strings = {"/esi", "/esi/with/multiple/segments", "/esi?with=query"})
	@SneakyThrows
	void shouldProxyRequests(
			@CartesianTest.Values(strings = {"HEAD", "GET", "PUT", "POST", "DELETE", "OPTIONS"}) String method,
			@CartesianTest.Values(strings = {"/esi", "/esi/with/multiple/segments", "/esi?with=query"}) String path) {
		var requestBodyExpected = method.equals("PUT") || method.equals("POST");
		var requestBody = requestBodyExpected ? "Test request" : null;
		var responseBodyExpected = !method.equals("HEAD");
		if (responseBodyExpected) {
			TestHttpUtils.enqueueResponse(
					mockEsi, 200, "Test response", Map.of("X-Server-Header", "Test server header"));
		} else {
			TestHttpUtils.enqueueResponse(mockEsi, 200, Map.of("X-Server-Header", "Test server header"));
		}
		var proxyResponse = TestHttpUtils.callProxy(
				client, proxy, method, path, Map.of("X-Client-Header", "Test client header"), requestBody);
		//		if (responseBodyExpected) {
		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				responseBodyExpected ? "Test response" : null,
				Map.of(
						"X-Server-Header",
						"Test server header",
						ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS));
		//		}

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		TestHttpUtils.assertRequest(
				esiRequest, method, path, Map.of("X-Client-Header", "Test client header"), requestBody);
	}

	@Test
	@SneakyThrows
	void shouldNotFollowRedirects() {
		TestHttpUtils.enqueueResponse(
				mockEsi, 302, Map.of("Location", "http://localhost:" + mockEsi.getPort() + "/redirected"));

		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse, 302, Map.of("Location", "http://localhost:" + MOCK_ESI_PORT + "/redirected"));

		assertNotNull(TestHttpUtils.takeRequest(mockEsi));
	}
}
