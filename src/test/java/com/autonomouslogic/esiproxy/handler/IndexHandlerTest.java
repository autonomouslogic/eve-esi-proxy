package com.autonomouslogic.esiproxy.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.esiproxy.EveEsiProxy;
import com.autonomouslogic.esiproxy.test.DaggerTestComponent;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IndexHandlerTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	OkHttpClient client;

	@Inject
	protected IndexHandlerTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		proxy.start();
	}

	@AfterEach
	@SneakyThrows
	void stop() {
		proxy.stop();
	}

	@Test
	@SneakyThrows
	void shouldRespondToRequests() {
		var response = client.newCall(new Request.Builder()
						.url("http://localhost:" + proxy.port())
						.build())
				.execute();
		assertEquals(200, response.code());
	}

	@Test
	@SneakyThrows
	void shouldDenyNonGetRequests() {
		var response = client.newCall(new Request.Builder()
						.url("http://localhost:" + proxy.port())
						.post(RequestBody.create(new byte[]{}))
						.build())
				.execute();
		assertEquals(405, response.code());
	}
}
