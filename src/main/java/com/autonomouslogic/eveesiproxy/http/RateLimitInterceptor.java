package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.commons.config.Config;
import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
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
	private final RateLimiter characterCorporationHistoryRateLimiter;
	private final RateLimiter marketHistoryRateLimiter;
	private final RateLimiter otherRateLimiter;

	private final double loggingRateLimit = 1.0 / 5.0;

	private final RateLimiter characterCorporationHistoryLoggingRateLimiter = RateLimiter.create(loggingRateLimit);
	private final RateLimiter marketHistoryLoggingRateLimiter = RateLimiter.create(loggingRateLimit);
	private final RateLimiter otherLoggingRateLimiter = RateLimiter.create(loggingRateLimit);

	@Inject
	protected RateLimitInterceptor() {
		characterCorporationHistoryRateLimiter =
				createRateLimiter(Configs.ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S);
		marketHistoryRateLimiter = createRateLimiter(Configs.ESI_MARKET_HISTORY_RATE_LIMIT_PER_S);
		otherRateLimiter = createRateLimiter(Configs.ESI_RATE_LIMIT_PER_S);
	}

	private static RateLimiter createRateLimiter(Config<Double> config) {
		var limit = config.getRequired();
		if (limit <= 0.0) {
			throw new IllegalArgumentException(config.getName() + " must positive");
		}
		return RateLimiter.create(limit);
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var path = chain.request().url().encodedPath();
		var type = EsiRouteClassifier.classifyRoute(path);
		log.info("Classified {} as {}", path, type);
		if (!otherRateLimiter.tryAcquire()) {
			logRateLimit(EsiRouteType.OTHER, otherLoggingRateLimiter);
			otherRateLimiter.acquire();
		}
		var response = chain.proceed(chain.request());
		return response;
	}

	private void logRateLimit(EsiRouteType type, RateLimiter loggingRateLimiter) {
		if (loggingRateLimiter.tryAcquire()) {
			log.info("Limiting requests ({}) to the ESI", type);
		}
	}
}
