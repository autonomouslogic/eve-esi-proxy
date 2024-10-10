package com.autonomouslogic.esiproxy.handler;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.esiproxy.EveEsiProxy;
import com.autonomouslogic.esiproxy.test.DaggerTestComponent;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
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
		proxy.stop();
		mockEsi.shutdown();
	}

	@Test
	@SneakyThrows
	void shouldProxyGetRequests() {
		log.info("Mock ESI port: {}", mockEsi.getPort());
		log.info("Proxy port: {}", proxy.port());
		mockEsi.enqueue(new MockResponse().setResponseCode(200).setBody("Test response"));

		var response = client.newCall(new Request.Builder()
						.url("http://localhost:" + proxy.port() + "/test")
						.build())
				.execute();
		assertEquals(200, response.code());

		var request = mockEsi.takeRequest(0, TimeUnit.SECONDS);
		assertEquals("/test", request.getPath());
		assertEquals("GET", request.getMethod());
		assertEquals(0, request.getBody().size());
	}
}
