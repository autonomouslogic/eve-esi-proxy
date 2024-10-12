package com.autonomouslogic.eveesiproxy.test;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Named;
import okhttp3.OkHttpClient;

@Module
public class TestOkHttpClientProvider {
	@Provides
	@Named("test")
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
