package com.autonomouslogic.esiproxy;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
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

	MockWebServer mockEsi;

	@BeforeEach
	@SneakyThrows
	void setup() {
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
		assertEquals(200, proxyResponse.code());
		assertEquals("Test body", proxyResponse.body().string());
		assertEquals("Test server header", proxyResponse.header("X-Server-Header"));

		var esiRequest = takeRequest();

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
		assertEquals(302, proxyResponse.code());
		assertEquals("http://localhost:" + MOCK_ESI_PORT + "/redirected", proxyResponse.header("Location"));

		assertNotNull(takeRequest());
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
}
