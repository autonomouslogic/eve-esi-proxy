package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderValues;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(30)
@Log4j2
public class ProxyHandlerTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	OkHttpClient client;

	MockWebServer mockEsi;

	@Inject
	protected ProxyHandlerTest() {}

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
	@ValueSource(strings = {"/esi", "/esi/with/multiple/segments", "/esi?with=query"})
	@SneakyThrows
	void shouldProxyGetRequests(String path) {
		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test response", Map.of("X-Server-Header", "Test server header"));
		var proxyResponse =
				TestHttpUtils.callProxy(client, proxy, "GET", path, Map.of("X-Client-Header", "Test client header"));
		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				"Test response",
				Map.of(
						"X-Server-Header",
						"Test server header",
						ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS));

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		TestHttpUtils.assertRequest(esiRequest, "GET", path, Map.of("X-Client-Header", "Test client header"));
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

	@ParameterizedTest
	@ValueSource(strings = {"public, max-age=60, immutable"})
	@SneakyThrows
	void shouldCacheCachableResponses(String cacheControl) {
		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body", Map.of("Cache-Control", cacheControl));

		// First proxy response.
		var proxyResponse1 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// ESI request.
		assertNotNull(TestHttpUtils.takeRequest(mockEsi));

		// Second proxy response should be served from cache.
		var proxyResponse2 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse2,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_HIT));

		// A second request to the ESI should never be made.
		TestHttpUtils.assertNoMoreRequests(mockEsi);
	}

	@ParameterizedTest
	@ValueSource(strings = {"private", "no-store", "no-cache", "max-age=0"})
	@SneakyThrows
	void shouldNotCacheUncachableResponses(String cacheControl) {
		for (int i = 0; i < 2; i++) {
			TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body " + i, Map.of("Cache-Control", cacheControl));
		}

		// First proxy response.
		var proxyResponse1 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse1,
				200,
				"Test body 0",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// ESI request.
		assertNotNull(TestHttpUtils.takeRequest(mockEsi));

		// Second proxy response should be served from cache.
		var proxyResponse2 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse2,
				200,
				"Test body 1",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// A second request to the ESI should be made.
		assertNotNull(TestHttpUtils.takeRequest(mockEsi));
	}

	@Test
	@SneakyThrows
	void shouldCacheResponsesWithinExpireTime() {
		var expires = ZonedDateTime.now().plusSeconds(10);
		var expiresString = DateTimeFormatter.RFC_1123_DATE_TIME.format(expires);
		TestHttpUtils.enqueueResponse(
				mockEsi, 200, "Test body", Map.of(HeaderNames.EXPIRES.lowerCase(), expiresString));

		// First proxy response.
		var proxyResponse1 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(
						ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS,
						HeaderNames.EXPIRES.lowerCase(),
						expiresString));

		// ESI request.
		assertNotNull(TestHttpUtils.takeRequest(mockEsi));

		// Second proxy response should be served from cache.
		var proxyResponse2 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse2,
				200,
				"Test body",
				Map.of(
						ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_HIT,
						HeaderNames.EXPIRES.lowerCase(),
						expiresString));

		// A second request to the ESI should never be made.
		TestHttpUtils.assertNoMoreRequests(mockEsi);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@SneakyThrows
	void shouldConditionallyRequestEtags(boolean modified) {
		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body", Map.of(HeaderNames.ETAG.lowerCase(), "hash1"));
		if (modified) {
			TestHttpUtils.enqueueResponse(mockEsi, 200, "Test body new");
		} else {
			TestHttpUtils.enqueueResponse(mockEsi, 304);
		}

		// First proxy response.
		var proxyResponse1 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(
						ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS,
						HeaderNames.ETAG.lowerCase(),
						"hash1"));

		// ESI request.
		assertNotNull(TestHttpUtils.takeRequest(mockEsi));

		// Second proxy response should be served from cache.
		var proxyResponse2 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse2,
				200,
				modified ? "Test body new" : "Test body",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// A second request to the ESI should be conditional.
		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		TestHttpUtils.assertRequest(esiRequest, "GET", "/esi", Map.of(HeaderNames.IF_NONE_MATCH.lowerCase(), "hash1"));
	}

	@Test
	@SneakyThrows
	void shouldConditionallyRequestBothExpiresAndEtags() {
		var expires = ZonedDateTime.now().plusSeconds(2);
		var expiresString = DateTimeFormatter.RFC_1123_DATE_TIME.format(expires);
		TestHttpUtils.enqueueResponse(
				mockEsi,
				200,
				"Test body",
				Map.of(HeaderNames.EXPIRES.lowerCase(), expiresString, HeaderNames.ETAG.lowerCase(), "hash1"));

		// First proxy response.
		var proxyResponse1 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(
						ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS,
						HeaderNames.EXPIRES.lowerCase(),
						expiresString,
						HeaderNames.ETAG.lowerCase(),
						"hash1"));

		// ESI request.
		assertNotNull(TestHttpUtils.takeRequest(mockEsi));

		// Second proxy response should be served from cache.
		var proxyResponse2 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse2,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_HIT));

		// No requests should have been made to the ESI.
		TestHttpUtils.assertNoMoreRequests(mockEsi);

		// Wait for the cache to expire.
		while (ZonedDateTime.now().isBefore(expires.plusSeconds(2))) {
			Thread.sleep(10);
		}

		// The ESI should respond with a 304 Not Modified.
		TestHttpUtils.enqueueResponse(mockEsi, 304);

		// Third proxy response should be conditional.
		var proxyResponse3 = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse3,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// A third request to the ESI should be conditional.
		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		TestHttpUtils.assertRequest(esiRequest, "GET", "/esi", Map.of(HeaderNames.IF_NONE_MATCH.lowerCase(), "hash1"));
	}
}
