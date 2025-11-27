package com.autonomouslogic.eveesiproxy.test;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Named;
import java.time.Duration;
import okhttp3.OkHttpClient;

@Module
public class TestOkHttpClientProvider {
	@Provides
	@Named("test")
	public OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(30))
				.connectTimeout(Duration.ofSeconds(30))
				.readTimeout(Duration.ofSeconds(30))
				.writeTimeout(Duration.ofSeconds(30))
				.followRedirects(false)
				.followSslRedirects(false)
				.build();
	}
}
