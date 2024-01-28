package com.autonomouslogic.esiproxy.factory;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import java.time.Duration;
import okhttp3.OkHttpClient;

@Factory
public class TestOkHttpClientFactory {
	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(2))
				.connectTimeout(Duration.ofSeconds(1))
				.readTimeout(Duration.ofSeconds(1))
				.writeTimeout(Duration.ofSeconds(1))
				.followRedirects(false)
				.followSslRedirects(false)
				.build();
	}
}
