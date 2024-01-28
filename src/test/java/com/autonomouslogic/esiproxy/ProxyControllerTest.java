package com.autonomouslogic.esiproxy;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.ZonedDateTime;
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
		assertNoMoreRequests();
		mockEsi.shutdown();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "/path", "/path/with/multiple/segments"})
	@SneakyThrows
	void shouldRelaySuccessfulGetRequests(String path) {
		enqueueResponse(200, "Test body", Map.of("X-Server-Header", "Test server header"));

		var proxyResponse = callProxy("GET", path, Map.of("X-Client-Header", "Test client header"));
		assertResponse(proxyResponse, 200, "Test body", Map.of("X-Server-Header", "Test server header"));

		var esiRequest = takeRequest();
		assertNotNull(esiRequest);

		assertEquals("localhost:" + MOCK_ESI_PORT, esiRequest.getHeader("Host"));
		assertEquals("Host", esiRequest.getHeaders().name(0));
		assertEquals("GET", esiRequest.getMethod());
		assertEquals(path, esiRequest.getPath());
		assertEquals("Test client header", esiRequest.getHeader("X-Client-Header"));
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
		assertResponse(proxyResponse1, 200, "Test body");

		// ESI request.
		assertNotNull(takeRequest());

		// Second proxy response should be served from cache.
		var proxyResponse2 = callProxy("GET", "/");
		assertResponse(proxyResponse2, 200, "Test body");

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
		assertResponse(proxyResponse1, 200, "Test body", Map.of("Cache-Control", cacheControl));

		// ESI request.
		assertNotNull(takeRequest());

		// Second proxy response should be sent to server too.
		var proxyResponse2 = callProxy("GET", "/");
		assertResponse(proxyResponse2, 200, "Test body", Map.of("Cache-Control", cacheControl));

		// A second request to the ESI should be made.
		assertNotNull(takeRequest());
	}

	@Test
	@SneakyThrows
	void shouldMakeConditionalRequestsBeforeServiceFromCache() {
		enqueueResponse(
				200,
				"Test body",
				Map.of(
						"Cache-Control",
						"public, max-age=60",
						"ETag",
						"\"aa698174d2c33ae33b6080b84cd1cb6b8c18a6966baeff3f774a9bbb\"",
						"Last-Modified",
						RFC_1123_DATE_TIME.format(ZonedDateTime.now().minusSeconds(60)),
						"Expires",
						RFC_1123_DATE_TIME.format(ZonedDateTime.now())));
		enqueueResponse(304);

		// First proxy response.
		var proxyResponse1 = callProxy("GET", "/");
		assertResponse(proxyResponse1, 200, "Test body");

		// ESI request.
		assertNotNull(takeRequest());

		Thread.sleep(2000);

		// Second proxy response should be served from cache.
		var proxyResponse2 = callProxy("GET", "/");
		//		assertResponse(proxyResponse2, 200, "Test body");

		// The second ESI request should be conditional.
		var conditionalRequest = takeRequest();
		assertNotNull(conditionalRequest);
		assertNotNull(conditionalRequest.getHeader("If-Modified-Since"));
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
		assertEquals(contentLength, Integer.parseInt(proxyResponse.header("Content-Length")));
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
}
