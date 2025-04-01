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
	private final RateLimiter characterCorporationHistoryLimiter;
	private final RateLimiter marketHistoryLimiter;
	private final RateLimiter otherLimiter;

	private final double logRateLimit = 1.0 / 5.0;

	private final RateLimiter characterCorporationHistoryLogLimiter = RateLimiter.create(logRateLimit);
	private final RateLimiter marketHistoryLogLimiter = RateLimiter.create(logRateLimit);
	private final RateLimiter otherLogLimiter = RateLimiter.create(logRateLimit);

	@Inject
	protected RateLimitInterceptor() {
		characterCorporationHistoryLimiter =
				createRateLimiter(Configs.ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S);
		marketHistoryLimiter = createRateLimiter(Configs.ESI_MARKET_HISTORY_RATE_LIMIT_PER_S);
		otherLimiter = createRateLimiter(Configs.ESI_RATE_LIMIT_PER_S);
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
		switch (type) {
			case CHARACTER_CORPORATION_HISTORY:
				rateLimit(type, characterCorporationHistoryLimiter, characterCorporationHistoryLogLimiter);
				break;
			case MARKET_HISTORY:
				rateLimit(type, marketHistoryLimiter, marketHistoryLogLimiter);
				break;
			case OTHER:
				rateLimit(type, otherLimiter, otherLogLimiter);
				break;
			default:
				throw new IllegalStateException("Unknown ESI route type: " + type);
		}
		var response = chain.proceed(chain.request());
		return response;
	}

	private void rateLimit(EsiRouteType routeType, RateLimiter rateLimiter, RateLimiter loggingRateLimiter) {
		if (!rateLimiter.tryAcquire()) {
			logRateLimit(routeType, loggingRateLimiter);
			rateLimiter.acquire();
		}
	}

	private void logRateLimit(EsiRouteType type, RateLimiter loggingRateLimiter) {
		if (loggingRateLimiter.tryAcquire()) {
			log.info("Limiting requests ({}) to the ESI", type);
		}
	}
}
