package com.autonomouslogic.esiproxy;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class ApplicationTest {
	@Inject
	EmbeddedServer server;
	@Inject
	OkHttpClient client;

	@Test
	@SneakyThrows
	void test() {
		var response = client.newCall(new Request.Builder().get().url("http://localhost:" + server.getPort()).build()).execute();
		assertEquals(200, response.code());
		assertEquals("Hello World!\n", response.body().string());
	}
}
