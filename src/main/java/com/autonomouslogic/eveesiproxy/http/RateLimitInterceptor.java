package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Global rate limiter for ESI requests.
 */
@Singleton
@Log4j2
public class RateLimitInterceptor implements Interceptor {
	private final RateLimiter rateLimiter;
	private Instant lastRateLimitLog = Instant.MIN;
	private final Duration rateLimitLogInterval = Duration.ofSeconds(5);

	@Inject
	protected RateLimitInterceptor() {
		var limit = Configs.ESI_RATE_LIMIT_PER_S.getRequired();
		if (limit <= 0.0) {
			throw new IllegalArgumentException(Configs.ESI_RATE_LIMIT_PER_S.getName() + " must positive");
		}
		rateLimiter = RateLimiter.create(limit);
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		if (!rateLimiter.tryAcquire()) {
			logRateLimit();
			rateLimiter.acquire();
		}
		var response = chain.proceed(chain.request());
		return response;
	}

	private void logRateLimit() {
		var now = Instant.now();
		var time = Duration.between(lastRateLimitLog, now);
		if (time.compareTo(rateLimitLogInterval) > 0) {
			lastRateLimitLog = now;
			log.info("Limiting requests to the ESI");
		}
	}
}
