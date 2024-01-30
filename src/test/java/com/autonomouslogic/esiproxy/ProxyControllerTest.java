package com.autonomouslogic.esiproxy;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Tests the basic ESI relay functionality.
 */
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
			assertNoMoreRequests();
		} finally {
			mockEsi.shutdown();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "/path", "/path/with/multiple/segments"})
	@SneakyThrows
	void shouldRelaySuccessfulGetRequests(String path) {
		enqueueResponse(200, "Test body", Map.of("X-Server-Header", "Test server header"));

		var proxyResponse = callProxy("GET", path, Map.of("X-Client-Header", "Test client header"));
		assertResponse(
				proxyResponse,
				200,
				"Test body",
				Map.of(
						"X-Server-Header",
						"Test server header",
						ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS));

		var esiRequest = takeRequest();
		assertNotNull(esiRequest);

		assertRequest(esiRequest, "GET", path, Map.of("X-Client-Header", "Test client header"));
	}

	@Test
	@SneakyThrows
	void shouldNotFollowRedirects() {
		enqueueResponse(302, Map.of("Location", "http://localhost:" + MOCK_ESI_PORT + "/redirected"));

		var proxyResponse = callProxy("GET", "/");
		assertResponse(proxyResponse, 302, Map.of("Location", "http://localhost:" + MOCK_ESI_PORT + "/redirected"));

		assertNotNull(takeRequest());
	}

	@Test
	@SneakyThrows
	void shouldCacheImmutableResponsesAndServeFromCache() {
		enqueueResponse(200, "Test body", Map.of("Cache-Control", "public, max-age=60, immutable"));

		// First proxy response.
		var proxyResponse1 = callProxy("GET", "/");
		assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// ESI request.
		assertNotNull(takeRequest());

		// Second proxy response should be served from cache.
		var proxyResponse2 = callProxy("GET", "/");
		assertResponse(
				proxyResponse2,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_HIT));

		// A second request to the ESI should never be made.
		assertNoMoreRequests();
	}

	@ParameterizedTest
	@ValueSource(strings = {"private", "no-store", "no-cache", "max-age=0"})
	@SneakyThrows
	void shouldNotCacheUncachableResponses(String cacheControl) {
		for (int i = 0; i < 2; i++) {
			enqueueResponse(200, "Test body", Map.of("Cache-Control", cacheControl));
		}

		// First proxy response.
		var proxyResponse1 = callProxy("GET", "/");
		assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(
						"Cache-Control",
						cacheControl,
						ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS));

		// ESI request.
		assertNotNull(takeRequest());

		// Second proxy response should be sent to server too.
		var proxyResponse2 = callProxy("GET", "/");
		assertResponse(
				proxyResponse2,
				200,
				"Test body",
				Map.of(
						"Cache-Control",
						cacheControl,
						ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS,
						ProxyHeaderValues.CACHE_STATUS_MISS));

		// A second request to the ESI should be made.
		assertNotNull(takeRequest());
	}

	@Test
	@SneakyThrows
	void shouldRefreshExpiredResourcesUsingConditionalRequests() {
		enqueueResponse(
				200,
				"Test body",
				Map.of(
						"Cache-Control",
						"public",
						"Expires",
						ZonedDateTime.now()
								.minus(10, ChronoUnit.SECONDS)
								.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
		enqueueResponse(NOT_MODIFIED.code());

		// First proxy response.
		var proxyResponse1 = callProxy("GET", "/");
		assertResponse(
				proxyResponse1,
				200,
				"Test body",
				Map.of(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// ESI request.
		assertNotNull(takeRequest());

		// Second proxy response should be sent to server too.
		var proxyResponse2 = callProxy("GET", "/");
		assertResponse(
				proxyResponse2,
				NOT_MODIFIED.code(),
				Map.of(ProxyHeaderNames.X_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));

		// A second request to the ESI should be made.
		var conditionalEsiRequest = takeRequest();
		assertNotNull(conditionalEsiRequest);
		assertEquals("todo", conditionalEsiRequest.getHeader("If-Modified-Since"));
		assertEquals("todo", conditionalEsiRequest.getHeader("If-None-Match"));
	}

	// ===============================================================

	private void enqueueResponse(int status) {
		mockEsi.enqueue(new MockResponse().setResponseCode(status));
	}

	private void enqueueResponse(int status, @NonNull String body) {
		mockEsi.enqueue(new MockResponse().setResponseCode(status).setBody(body));
	}

	private void enqueueResponse(int status, @NonNull Map<String, String> headers) {
		var response = new MockResponse().setResponseCode(status);
		headers.forEach(response::addHeader);
		mockEsi.enqueue(response);
	}

	private void enqueueResponse(int status, @NonNull String body, @NonNull Map<String, String> headers) {
		var response = new MockResponse().setResponseCode(status).setBody(body);
		headers.forEach(response::addHeader);
		mockEsi.enqueue(response);
	}

	private Request.Builder proxyRequest(String method, String path) {
		return new Request.Builder().method(method, null).url("http://localhost:" + server.getPort() + path);
	}

	private Request.Builder proxyRequest(String method, String path, Map<String, String> headers) {
		var req = proxyRequest(method, path);
		headers.forEach(req::header);
		return req;
	}

	@SneakyThrows
	private Response callProxy(Request request) {
		return client.newCall(request).execute();
	}

	private Response callProxy(String method, String path, Map<String, String> headers) {
		return callProxy(proxyRequest(method, path, headers).build());
	}

	private Response callProxy(String method, String path) {
		return callProxy(proxyRequest(method, path).build());
	}

	@SneakyThrows
	private RecordedRequest takeRequest() {
		return mockEsi.takeRequest(0, TimeUnit.SECONDS);
	}

	private void assertNoMoreRequests() {
		assertNull(takeRequest());
	}

	@SneakyThrows
	private void assertResponse(Response proxyResponse, int status, String body, Map<String, String> headers) {
		assertEquals(status, proxyResponse.code());
		assertEquals(body == null ? "" : body, proxyResponse.body().string());
		var contentLength = body == null ? 0 : body.length();
		assertEquals(contentLength, proxyResponse.body().contentLength());
		if (contentLength > 0) {
			assertEquals(contentLength, Integer.parseInt(proxyResponse.header("Content-Length")));
		}
		if (headers != null) {
			headers.forEach((name, value) -> assertEquals(value, proxyResponse.header(name), name));
		}
	}

	private void assertResponse(Response proxyResponse, int status, String body) {
		assertResponse(proxyResponse, status, body, null);
	}

	private void assertResponse(Response proxyResponse, int status, Map<String, String> headers) {
		assertResponse(proxyResponse, status, null, headers);
	}

	private void assertRequest(RecordedRequest esiRequest, String get, String path, Map<String, String> headers) {
		assertNotNull(esiRequest);
		assertEquals("localhost:" + MOCK_ESI_PORT, esiRequest.getHeader("Host"));
		assertEquals("Host", esiRequest.getHeaders().name(0));
		assertEquals(get, esiRequest.getMethod());
		assertEquals(path, esiRequest.getPath());
		headers.forEach((name, value) -> assertEquals(value, esiRequest.getHeader(name), name));
	}
}
