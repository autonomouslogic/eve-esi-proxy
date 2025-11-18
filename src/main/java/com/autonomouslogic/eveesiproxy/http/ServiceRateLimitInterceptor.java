package com.autonomouslogic.eveesiproxy.http;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Handles global stop if 429 responses are received.
 * This does not currently implement any group-based stops.
 * @see <a href="https://developers.eveonline.com/docs/services/esi/rate-limiting/">Rate Limiting</a>
 * @see <a href="https://developers.eveonline.com/blog/hold-your-horses-introducing-rate-limiting-to-esi">Hold your horses: introducing rate limiting to ESI</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/429">429 Too Many Requests</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After">Retry-After header</a>
 */
@Singleton
@Log4j2
public class ServiceRateLimitInterceptor implements Interceptor {
	public static final String RETRY_AFTER = "Retry-After";

	private static final AtomicBoolean globalStop = new AtomicBoolean();

	@Inject
	protected ServiceRateLimitInterceptor() {}

	@NotNull
	@Override
	@SneakyThrows
	public Response intercept(@NotNull Chain chain) throws IOException {
		var success = false;
		Response response;
		do {
			respectGlobalStop();
			response = chain.proceed(chain.request());
			if (response.code() == 429) {
				// @todo there's possibly a race condition here on concurrent requests, though it might not matter in
				// practice.
				globalStop.set(true);
				var resetTime = parseResetTime(
						Optional.ofNullable(response.header(RETRY_AFTER)).orElse("10"));
				log.warn(String.format("ESI 429, waiting for %s", resetTime));
				Thread.sleep(resetTime.plusSeconds(1).toMillis());
				globalStop.set(false);
			} else {
				success = true;
			}
		} while (!success);
		return response;
	}

	@SneakyThrows
	private void respectGlobalStop() {
		var start = Instant.now();
		while (globalStop.get()) {
			log.debug(String.format("Waiting for ESI 429: %s", Duration.between(start, Instant.now())));
			Thread.sleep(1000);
		}
	}

	private Duration parseResetTime(@NonNull String resetTime) {
		return Duration.ofSeconds(Long.parseLong(resetTime));
	}
}
