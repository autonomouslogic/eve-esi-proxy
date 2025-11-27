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
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

/**
 * Handles global stop if 420 responses are received.
 * This is the "old" system for limiting requests on the ESI.
 * The newer method is implemented in {@link ServiceRateLimitInterceptor}.
 */
@Singleton
@Log4j2
public class ErrorLimitInterceptor implements Interceptor {
	public static final String ESI_420_TEXT = "This software has exceeded the error limit for ESI.";
	public static final String ERROR_LIMIT_RESET = "x-esi-error-limit-reset";
	public static final String ERROR_LIMIT_REMAIN = "x-esi-error-limit-remain";

	private static final AtomicBoolean globalStop = new AtomicBoolean();

	@Inject
	protected ErrorLimitInterceptor() {}

	@NotNull
	@Override
	@SneakyThrows
	public Response intercept(@NotNull Chain chain) throws IOException {
		var success = false;
		Response response;
		do {
			respectGlobalStop();
			response = chain.proceed(chain.request());
			var body = "";
			// If body is small, consume it so it can be checked later.
			if (response.body().contentLength() < 8 * 1024) {
				body = response.body().string();
				// Reconstruct.
				response = response.newBuilder()
						.body(ResponseBody.create(response.body().contentType(), body))
						.build();
			}
			if (response.code() == 420 || body.contains(ESI_420_TEXT)) {
				// @todo there's a race condition here on concurrent requests, though it might not matter in practice.
				globalStop.set(true);
				try {
					var resetTime = parseResetTime(Optional.ofNullable(response.header(ERROR_LIMIT_RESET))
							.orElse("10"));
					log.warn(String.format("ESI 420, waiting for %s", resetTime));
					response.close();
					Thread.sleep(resetTime.plusSeconds(1).toMillis());
				} finally {
					globalStop.set(false);
				}
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
			log.debug(String.format("Waiting for ESI 420: %s", Duration.between(start, Instant.now())));
			Thread.sleep(1000);
		}
	}

	private Duration parseResetTime(@NonNull String resetTime) {
		return Duration.ofSeconds(Long.parseLong(resetTime));
	}
}
