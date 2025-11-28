package com.autonomouslogic.eveesiproxy.http;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Token bucket implementation for rate limiting.
 * Tokens are added at a constant rate, and requests consume tokens.
 * If no tokens are available, the request must wait.
 */
@Log4j2
public class TokenBucket {
	private final int maxTokens;
	private final double tokensPerSecond;
	private final Lock lock = new ReentrantLock();

	private double availableTokens;
	private Instant lastRefillTime;

	/**
	 * Create a new token bucket.
	 * @param maxTokens the maximum number of tokens in the bucket
	 * @param tokensPerSecond the rate at which tokens are added to the bucket
	 */
	public TokenBucket(int maxTokens, double tokensPerSecond) {
		if (maxTokens <= 0) {
			throw new IllegalArgumentException("maxTokens must be greater than 0");
		}
		if (tokensPerSecond <= 0) {
			throw new IllegalArgumentException("tokensPerSecond must be greater than 0");
		}

		this.maxTokens = maxTokens;
		this.tokensPerSecond = tokensPerSecond;
		this.availableTokens = maxTokens;
		this.lastRefillTime = Instant.now();
	}

	/**
	 * Acquire a token from the bucket, blocking if necessary.
	 * @param groupName the name of the rate limit group (for logging)
	 * @throws InterruptedException if interrupted while waiting
	 */
	public void acquire(@NonNull String groupName) throws InterruptedException {
		lock.lock();
		try {
			refillTokens();

			if (availableTokens >= 1.0) {
				availableTokens -= 1.0;
				return;
			}

			// Calculate how long to wait for the next token
			double tokensNeeded = 1.0 - availableTokens;
			long waitMillis = (long) Math.ceil((tokensNeeded / tokensPerSecond) * 1000.0);

			log.debug(
					"Rate limiting group '{}': waiting {}ms for token (available: {}, max: {})",
					groupName,
					waitMillis,
					String.format("%.2f", availableTokens),
					maxTokens);

			Thread.sleep(waitMillis);

			// Refill again after waiting
			refillTokens();
			if (availableTokens >= 1.0) {
				availableTokens -= 1.0;
			} else {
				// Shouldn't happen, but consume what we have
				availableTokens = 0;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Refill tokens based on elapsed time since last refill.
	 */
	private void refillTokens() {
		Instant now = Instant.now();
		Duration elapsed = Duration.between(lastRefillTime, now);
		double elapsedSeconds = elapsed.toMillis() / 1000.0;

		double tokensToAdd = elapsedSeconds * tokensPerSecond;
		availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
		lastRefillTime = now;
	}

	/**
	 * Get the current number of available tokens (for testing).
	 * @return the number of available tokens
	 */
	public double getAvailableTokens() {
		lock.lock();
		try {
			refillTokens();
			return availableTokens;
		} finally {
			lock.unlock();
		}
	}
}
