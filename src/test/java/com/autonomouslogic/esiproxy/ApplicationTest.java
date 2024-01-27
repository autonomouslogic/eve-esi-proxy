package com.autonomouslogic.esiproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

@MicronautTest
public class ApplicationTest {
	@Inject
	EmbeddedServer server;

	@Inject
	OkHttpClient client;

	@Test
	@SneakyThrows
	void test() {
		var response = client.newCall(new Request.Builder()
						.get()
						.url("http://localhost:" + server.getPort())
						.build())
				.execute();
		assertEquals(200, response.code());
		assertEquals(Protocol.HTTP_2, response.protocol());
		assertEquals("Hello World!\n", response.body().string());
	}
}
