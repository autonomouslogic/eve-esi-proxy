package com.autonomouslogic.eveesiproxy.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
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
		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/");
		assertEquals(200, proxyResponse.code());
	}

	@Test
	@SneakyThrows
	void shouldDenyNonGetRequests() {
		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "POST", "/", "body");
		TestHttpUtils.assertResponse(proxyResponse, 405);
	}
}
