package com.autonomouslogic.esiproxy;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;

import com.autonomouslogic.esiproxy.test.TestHttpUtils;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Tests the basic ESI relay functionality.
 */
@Disabled
@Deprecated
@MicronautTest
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
public class ProxyControllerTest {
	@Inject
	EmbeddedServer server;

	@Inject
	OkHttpClient client;

	@Inject
	EsiRelay esiRelay;

	MockWebServer mockEsi;

	@BeforeEach
	@SneakyThrows
	void setup() {
		esiRelay.clearCache();
		mockEsi = new MockWebServer();
		mockEsi.start(MOCK_ESI_PORT);
	}

	@AfterEach
	@SneakyThrows
	void stop() {
		try {
			TestHttpUtils.assertNoMoreRequests(mockEsi);
		} finally {
			mockEsi.shutdown();
		}
	}

	//	@ParameterizedTest
	//	@ValueSource(strings = {"/", "/path", "/path/with/multiple/segments"})
	//	@SneakyThrows
	//	void shouldRelaySuccessfulGetRequests(String path) {
	//		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body", Map.of("X-Server-Header", "Test server header"));
	//
	//		var proxyResponse = TestHttpUtils.callProxy(client, server, "GET", path, Map.of("X-Client-Header", "Test client
	// header"));
	//		TestHttpUtils.assertResponse(
	//				proxyResponse,
	//				200,
	//				"Test body",
	//				Map.of(
	//						"X-Server-Header",
	//						"Test server header",
	//						ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS,
	//						ProxyHeaderValues.CACHE_STATUS_MISS));
	//
	//		var esiRequest = takeRequest();
	//		assertNotNull(esiRequest);
	//
	//		assertRequest(esiRequest, "GET", path, Map.of("X-Client-Header", "Test client header"));
	//	}
	//
	//	@Test
	//	@SneakyThrows
	//	void shouldNotFollowRedirects() {
	//		TestHttpUtils.enqueueResponse(mockEsi, 302, Map.of("Location", "http://localhost:" + MOCK_ESI_PORT +
	// "/redirected"));
	//
	//		var proxyResponse = callProxy("GET", "/");
	//		assertResponse(proxyResponse, 302, Map.of("Location", "http://localhost:" + MOCK_ESI_PORT + "/redirected"));
	//
	//		assertNotNull(takeRequest());
	//	}
	//
	//	@Test
	//	@SneakyThrows
	//	void shouldCacheImmutableResponsesAndServeFromCache() {
	//		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body", Map.of("Cache-Control", "public, max-age=60,
	// immutable"));
	//
	//		// First proxy response.
	//		var proxyResponse1 = callProxy("GET", "/");
	//		assertResponse(
	//				proxyResponse1,
	//				200,
	//				"Test body",
	//				Map.of(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));
	//
	//		// ESI request.
	//		assertNotNull(takeRequest());
	//
	//		// Second proxy response should be served from cache.
	//		var proxyResponse2 = callProxy("GET", "/");
	//		assertResponse(
	//				proxyResponse2,
	//				200,
	//				"Test body",
	//				Map.of(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_HIT));
	//
	//		// A second request to the ESI should never be made.
	//		assertNoMoreRequests();
	//	}
	//
	//	@ParameterizedTest
	//	@ValueSource(strings = {"private", "no-store", "no-cache", "max-age=0"})
	//	@SneakyThrows
	//	void shouldNotCacheUncachableResponses(String cacheControl) {
	//		for (int i = 0; i < 2; i++) {
	//			TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body", Map.of("Cache-Control", cacheControl));
	//		}
	//
	//		// First proxy response.
	//		var proxyResponse1 = callProxy("GET", "/");
	//		assertResponse(
	//				proxyResponse1,
	//				200,
	//				"Test body",
	//				Map.of(
	//						"Cache-Control",
	//						cacheControl,
	//						ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS,
	//						ProxyHeaderValues.CACHE_STATUS_MISS));
	//
	//		// ESI request.
	//		assertNotNull(takeRequest());
	//
	//		// Second proxy response should be sent to server too.
	//		var proxyResponse2 = callProxy("GET", "/");
	//		assertResponse(
	//				proxyResponse2,
	//				200,
	//				"Test body",
	//				Map.of(
	//						"Cache-Control",
	//						cacheControl,
	//						ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS,
	//						ProxyHeaderValues.CACHE_STATUS_MISS));
	//
	//		// A second request to the ESI should be made.
	//		assertNotNull(takeRequest());
	//	}
}
