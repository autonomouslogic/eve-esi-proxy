package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class ServerRetryInterceptor implements Interceptor {
	private static List<String> retryMethods = List.of("GET", "HEAD", "OPTIONS", "TRACE");
	private final int maxTries = Configs.HTTP_MAX_TRIES.getRequired();
	private final Duration retryDelay = Configs.HTTP_RETRY_DELAY.getRequired();

	@Inject
	protected ServerRetryInterceptor() {}

	@NotNull
	@Override
	@SneakyThrows
	public Response intercept(@NotNull Chain chain) throws IOException {
		var req = chain.request();
		if (!retryMethods.contains(req.method())) {
			return chain.proceed(req);
		}
		try {
			var response = chain.proceed(req);
			for (int i = 1; i < maxTries; i++) {
				if (response.code() / 100 != 5) {
					return response;
				}
				log.trace("Retrying {} {}: {}", req.method(), req.url(), response.code());
				response.close();
				Thread.sleep(retryDelay);
				response = chain.proceed(req);
			}
			log.trace("Max retries reached for {} {}", req.method(), req.url());
			return response;
		} catch (Exception e) {
			log.warn("Error during retry " + req.method() + " " + req.url(), e);
			throw e;
		}
	}
}
