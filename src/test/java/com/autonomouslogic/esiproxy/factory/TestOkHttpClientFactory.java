package com.autonomouslogic.esiproxy.factory;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import okhttp3.OkHttpClient;

@Factory
public class TestOkHttpClientFactory {
	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder().build();
	}
}
