package com.autonomouslogic.eveesiproxy.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TokenBucketTest {

	@Test
	void shouldCreateTokenBucket() {
		var bucket = new TokenBucket(100, 10.0);
		// Should start with full bucket
		assertEquals(100.0, bucket.getAvailableTokens(), 0.01);
	}

	@Test
	void shouldThrowExceptionForInvalidMaxTokens() {
		assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 10.0));
		assertThrows(IllegalArgumentException.class, () -> new TokenBucket(-1, 10.0));
	}

	@Test
	void shouldThrowExceptionForInvalidTokensPerSecond() {
		assertThrows(IllegalArgumentException.class, () -> new TokenBucket(100, 0.0));
		assertThrows(IllegalArgumentException.class, () -> new TokenBucket(100, -1.0));
	}

	@Test
	void shouldConsumeToken() throws InterruptedException {
		var bucket = new TokenBucket(100, 10.0);
		bucket.acquire("test-group");
		assertEquals(99.0, bucket.getAvailableTokens(), 0.01);
	}

	@Test
	void shouldConsumeMultipleTokens() throws InterruptedException {
		var bucket = new TokenBucket(100, 10.0);
		bucket.acquire("test-group");
		bucket.acquire("test-group");
		bucket.acquire("test-group");
		assertEquals(97.0, bucket.getAvailableTokens(), 0.01);
	}

	@Test
	void shouldRefillTokensOverTime() throws InterruptedException {
		var bucket = new TokenBucket(100, 10.0); // 10 tokens per second
		bucket.acquire("test-group"); // Consume 1 token
		assertEquals(99.0, bucket.getAvailableTokens(), 0.01);

		Thread.sleep(200); // Wait 200ms = 0.2 seconds
		// Should have refilled 0.2 * 10 = 2 tokens
		// 99 + 2 = 101, but capped at 100
		assertEquals(100.0, bucket.getAvailableTokens(), 0.5);
	}

	@Test
	void shouldNotExceedMaxTokens() throws InterruptedException {
		var bucket = new TokenBucket(100, 10.0);
		Thread.sleep(1000); // Wait 1 second, should try to add 10 tokens
		// Should still be capped at 100
		assertEquals(100.0, bucket.getAvailableTokens(), 0.01);
	}

	@Test
	void shouldWaitWhenNoTokensAvailable() throws InterruptedException {
		var bucket = new TokenBucket(2, 10.0); // Small bucket, 10 tokens per second

		long startTime = System.currentTimeMillis();

		// Consume all tokens
		bucket.acquire("test-group");
		bucket.acquire("test-group");

		// Next acquire should wait approximately 100ms (1 token / 10 tokens per second = 0.1s)
		bucket.acquire("test-group");

		long elapsedTime = System.currentTimeMillis() - startTime;

		// Should have waited at least 80ms (allowing some margin for timing)
		assertTrue(elapsedTime >= 80, "Expected to wait at least 80ms, but waited " + elapsedTime + "ms");

		// Bucket should have approximately 0 tokens left (or slightly more due to refill)
		assertTrue(bucket.getAvailableTokens() < 1.0, "Expected less than 1 token remaining");
	}

	@Test
	void shouldHandleHighThroughput() throws InterruptedException {
		var bucket = new TokenBucket(600, 600.0 / 900.0); // char-industry: 600 tokens / 15 minutes

		long startTime = System.currentTimeMillis();

		// Consume 5 tokens rapidly
		for (int i = 0; i < 5; i++) {
			bucket.acquire("char-industry");
		}

		long elapsedTime = System.currentTimeMillis() - startTime;

		// With 0.6666 tokens per second, acquiring 5 tokens should take about:
		// First token: instant (from initial bucket)
		// Remaining 4 tokens: 4 / 0.6666 = ~6 seconds
		// But we start with 600 tokens, so it should be nearly instant
		assertTrue(elapsedTime < 500, "Expected fast acquisition from full bucket, took " + elapsedTime + "ms");

		// Should have consumed 5 tokens
		assertEquals(595.0, bucket.getAvailableTokens(), 1.0);
	}

	@Test
	void shouldHandleFractionalRefill() throws InterruptedException {
		var bucket = new TokenBucket(150, 150.0 / 900.0); // char-wallet: 150 tokens / 15 minutes = 0.1666... per sec

		bucket.acquire("char-wallet"); // Consume 1 token
		assertEquals(149.0, bucket.getAvailableTokens(), 0.01);

		Thread.sleep(100); // Wait 100ms = 0.1 seconds
		// Should have refilled 0.1 * 0.1666 = 0.01666 tokens
		assertEquals(149.0, bucket.getAvailableTokens(), 0.05);

		Thread.sleep(900); // Wait another 900ms = 0.9 seconds (total 1 second)
		// Should have refilled approximately 1 * 0.1666 = 0.1666 tokens
		assertEquals(149.16, bucket.getAvailableTokens(), 0.1);
	}
}
