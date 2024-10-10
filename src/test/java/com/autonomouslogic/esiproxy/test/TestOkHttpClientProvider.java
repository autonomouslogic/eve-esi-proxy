package com.autonomouslogic.esiproxy.test;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class TestOkHttpClientProvider {
	@Provides
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder()
				//				.callTimeout(Duration.ofSeconds(2))
				//				.connectTimeout(Duration.ofSeconds(1))
				//				.readTimeout(Duration.ofSeconds(1))
				//				.writeTimeout(Duration.ofSeconds(1))
				.followRedirects(false)
				.followSslRedirects(false)
				.build();
	}
}
