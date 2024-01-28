package com.autonomouslogic.esiproxy;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Tests the basic ESI relay functionality.
 */
@MicronautTest
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
public class EsiRelayTest {
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
		mockEsi.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldRelaySuccessfulGetRequests() {
		mockEsi.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody("Test body")
				.addHeader("X-Server-Header", "Test server header"));

		var response = client.newCall(new Request.Builder()
						.get()
						.url("http://localhost:" + server.getPort())
						.header("X-Client-Header", "Test client header")
						.build())
				.execute();
		assertEquals(200, response.code());
		assertEquals("Test body", response.body().string());
		assertEquals("Test server header", response.header("X-Server-Header"));

		var request = mockEsi.takeRequest();
		assertEquals("GET", request.getMethod());
		assertEquals("/", request.getPath());
		assertEquals("Test client header", request.getHeader("X-Client-Header"));
	}
}
