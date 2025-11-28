package com.autonomouslogic.eveesiproxy.http;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Handles per-group rate limiting when 429 responses are received.
 * When a 429 response includes an x-ratelimit-group header, only requests to that specific group are stopped.
 * URLs without a rate limit group are not affected by rate limiting.
 * @see <a href="https://developers.eveonline.com/docs/services/esi/rate-limiting/">Rate Limiting</a>
 * @see <a href="https://developers.eveonline.com/blog/hold-your-horses-introducing-rate-limiting-to-esi">Hold your horses: introducing rate limiting to ESI</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/429">429 Too Many Requests</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After">Retry-After header</a>
 */
@Singleton
@Log4j2
public class RateLimitGroupStopInterceptor implements Interceptor {
	public static final String RETRY_AFTER = "Retry-After";
	public static final String X_RATELIMIT_GROUP = "x-ratelimit-group";

	private final ConcurrentHashMap<String, AtomicBoolean> groupStops = new ConcurrentHashMap<>();

	@Inject
	EsiUrlGroupResolver urlGroupResolver;

	@Inject
	protected RateLimitGroupStopInterceptor() {}

	@NotNull
	@Override
	@SneakyThrows
	public Response intercept(@NotNull Chain chain) throws IOException {
		var request = chain.request();
		var urlPath = request.url().encodedPath();
		var requestGroup = urlGroupResolver.resolveGroup(urlPath);

		var success = false;
		Response response;
		do {
			// If the URL belongs to a rate limit group, respect that group's stop
			if (requestGroup.isPresent()) {
				respectGroupStop(requestGroup.get());
			}

			response = chain.proceed(request);
			if (response.code() == 429) {
				// Get the rate limit group from the response header, or use the resolved group
				var rateLimitGroup = Optional.ofNullable(response.header(X_RATELIMIT_GROUP))
						.or(() -> requestGroup)
						.orElse(null);

				// If there's a rate limit group, stop other requests to this group
				if (rateLimitGroup != null) {
					var groupStop = groupStops.computeIfAbsent(rateLimitGroup, k -> new AtomicBoolean());
					groupStop.set(true);
					try {
						var resetTime = parseResetTime(Optional.ofNullable(response.header(RETRY_AFTER))
								.orElse("10"));
						log.warn(String.format("ESI 429 for group '%s', waiting for %s", rateLimitGroup, resetTime));
						response.close();
						Thread.sleep(resetTime.plusSeconds(1).toMillis());
					} finally {
						groupStop.set(false);
					}
				} else {
					// No rate limit group, just retry without blocking other requests
					var resetTime = parseResetTime(
							Optional.ofNullable(response.header(RETRY_AFTER)).orElse("10"));
					log.warn(String.format("ESI 429 (no group), waiting for %s", resetTime));
					response.close();
					Thread.sleep(resetTime.plusSeconds(1).toMillis());
				}
			} else {
				success = true;
			}
		} while (!success);
		return response;
	}

	@SneakyThrows
	private void respectGroupStop(@NonNull String group) {
		var groupStop = groupStops.get(group);
		if (groupStop == null) {
			return;
		}

		var start = Instant.now();
		while (groupStop.get()) {
			log.debug(String.format(
					"Waiting for ESI 429 (group '%s'): %s", group, Duration.between(start, Instant.now())));
			Thread.sleep(1000);
		}
	}

	private Duration parseResetTime(@NonNull String resetTime) {
		return Duration.ofSeconds(Long.parseLong(resetTime));
	}
}
