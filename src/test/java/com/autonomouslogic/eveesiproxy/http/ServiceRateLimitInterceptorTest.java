package com.autonomouslogic.eveesiproxy.http;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServiceRateLimitInterceptorTest {

	private ServiceRateLimitInterceptor interceptor;
	private EsiUrlGroupResolver mockResolver;

	@BeforeEach
	void setUp() {
		// Save original config value
		mockResolver = mock(EsiUrlGroupResolver.class);
		interceptor = new ServiceRateLimitInterceptor();
		interceptor.urlGroupResolver = mockResolver;
	}

	@AfterEach
	void tearDown() {
		// Reset config to default
		System.clearProperty("ESI_WINDOW_RATE_LIMIT_ENABLED");
	}

	@Test
	void shouldApplyRateLimitingWhenEnabled() throws IOException, InterruptedException {
		System.setProperty("ESI_WINDOW_RATE_LIMIT_ENABLED", "true");

		var groupInfo = EsiUrlGroup.builder()
				.url("/characters/{character_id}/wallet")
				.group("char-wallet")
				.maxTokens(150)
				.windowSize("15m")
				.build();

		when(mockResolver.resolveGroupInfo(any())).thenReturn(java.util.Optional.of(groupInfo));

		var mockChain = mock(Interceptor.Chain.class);
		var request = new Request.Builder()
				.url("https://esi.evetech.net/latest/characters/12345/wallet")
				.build();

		when(mockChain.request()).thenReturn(request);
		when(mockChain.proceed(any()))
				.thenReturn(new Response.Builder()
						.request(request)
						.protocol(Protocol.HTTP_2)
						.code(200)
						.message("OK")
						.build());

		long startTime = System.currentTimeMillis();

		// First request should be fast (bucket has tokens)
		interceptor.intercept(mockChain);

		long elapsedTime = System.currentTimeMillis() - startTime;

		// Should complete quickly
		assertTrue(elapsedTime < 500, "First request should be fast, took " + elapsedTime + "ms");
		verify(mockChain, times(1)).proceed(any());
	}

	@Test
	void shouldNotApplyRateLimitingWhenDisabled() throws IOException, InterruptedException {
		System.setProperty("ESI_WINDOW_RATE_LIMIT_ENABLED", "false");

		var groupInfo = EsiUrlGroup.builder()
				.url("/characters/{character_id}/wallet")
				.group("char-wallet")
				.maxTokens(150)
				.windowSize("15m")
				.build();

		when(mockResolver.resolveGroupInfo(any())).thenReturn(java.util.Optional.of(groupInfo));

		var mockChain = mock(Interceptor.Chain.class);
		var request = new Request.Builder()
				.url("https://esi.evetech.net/latest/characters/12345/wallet")
				.build();

		when(mockChain.request()).thenReturn(request);
		when(mockChain.proceed(any()))
				.thenReturn(new Response.Builder()
						.request(request)
						.protocol(Protocol.HTTP_2)
						.code(200)
						.message("OK")
						.build());

		long startTime = System.currentTimeMillis();

		interceptor.intercept(mockChain);

		long elapsedTime = System.currentTimeMillis() - startTime;

		// Should complete quickly (no rate limiting applied)
		assertTrue(elapsedTime < 500, "Request should be fast when rate limiting disabled, took " + elapsedTime + "ms");
		verify(mockChain, times(1)).proceed(any());
	}

	@Test
	void shouldNotApplyRateLimitingForUrlsWithoutRateLimits() throws IOException, InterruptedException {
		System.setProperty("ESI_WINDOW_RATE_LIMIT_ENABLED", "true");

		var groupInfo = EsiUrlGroup.builder()
				.url("/alliances/{alliance_id}")
				.group(null)
				.maxTokens(null)
				.windowSize(null)
				.build();

		when(mockResolver.resolveGroupInfo(any())).thenReturn(java.util.Optional.of(groupInfo));

		var mockChain = mock(Interceptor.Chain.class);
		var request = new Request.Builder()
				.url("https://esi.evetech.net/latest/alliances/123")
				.build();

		when(mockChain.request()).thenReturn(request);
		when(mockChain.proceed(any()))
				.thenReturn(new Response.Builder()
						.request(request)
						.protocol(Protocol.HTTP_2)
						.code(200)
						.message("OK")
						.build());

		long startTime = System.currentTimeMillis();

		interceptor.intercept(mockChain);

		long elapsedTime = System.currentTimeMillis() - startTime;

		// Should complete quickly (no rate limit info)
		assertTrue(
				elapsedTime < 500,
				"Request should be fast when no rate limit info available, took " + elapsedTime + "ms");
		verify(mockChain, times(1)).proceed(any());
	}

	@Test
	void shouldThrottleMultipleRequestsToSameGroup() throws IOException, InterruptedException {
		System.setProperty("ESI_WINDOW_RATE_LIMIT_ENABLED", "true");

		// Create a very restrictive rate limit for testing (2 tokens, 1 token per second)
		var groupInfo = EsiUrlGroup.builder()
				.url("/characters/{character_id}/wallet")
				.group("test-group")
				.maxTokens(2)
				.windowSize("2s")
				.build();

		when(mockResolver.resolveGroupInfo(any())).thenReturn(java.util.Optional.of(groupInfo));

		var mockChain = mock(Interceptor.Chain.class);
		var request = new Request.Builder()
				.url("https://esi.evetech.net/latest/characters/12345/wallet")
				.build();

		when(mockChain.request()).thenReturn(request);
		when(mockChain.proceed(any()))
				.thenReturn(new Response.Builder()
						.request(request)
						.protocol(Protocol.HTTP_2)
						.code(200)
						.message("OK")
						.build());

		long startTime = System.currentTimeMillis();

		// Make 3 requests (bucket has 2 tokens initially, 3rd should wait)
		interceptor.intercept(mockChain);
		interceptor.intercept(mockChain);
		interceptor.intercept(mockChain);

		long elapsedTime = System.currentTimeMillis() - startTime;

		// Should have waited for at least the 3rd request (~1 second wait)
		assertTrue(elapsedTime >= 800, "Expected to wait at least 800ms, but waited " + elapsedTime + "ms");
		verify(mockChain, times(3)).proceed(any());
	}
}
