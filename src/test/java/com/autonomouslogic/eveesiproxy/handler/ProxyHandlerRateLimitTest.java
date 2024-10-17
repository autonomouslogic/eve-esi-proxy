package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.HttpDate;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderValues;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import com.google.common.base.Stopwatch;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "ESI_RATE_LIMIT_PER_S", value = "20")
@SetEnvironmentVariable(key = "ESI_MARKET_HISTORY_RATE_LIMIT_PER_S", value = "10")
@SetEnvironmentVariable(key = "ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S", value = "5")
@Timeout(30)
@Log4j2
public class ProxyHandlerRateLimitTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	MockWebServer mockEsi;

	final Duration duration = Duration.ofSeconds(5);

	@Inject
	protected ProxyHandlerRateLimitTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		mockEsi = new MockWebServer();
		var expires = HttpDate.format(ZonedDateTime.now().plusHours(1));
		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
				return new MockResponse().setResponseCode(200).setHeader("Expires", expires);
			}
		});
		mockEsi.start(MOCK_ESI_PORT);
		proxy.start();
	}

	@AfterEach
	@SneakyThrows
	void stop() {
		proxy.stop();
		mockEsi.shutdown();
	}

	@ParameterizedTest
	@MethodSource("rateLimitTests")
	@SneakyThrows
	void shouldLimitRequests(String url, double expectedRate) {
		var count = new AtomicInteger(0);
		var watch = Stopwatch.createStarted();
		while (watch.elapsed().compareTo(duration) < 0) {
			TestHttpUtils.assertResponse(
					TestHttpUtils.callProxy(
							client, proxy, "GET", url, Map.of(HeaderNames.CACHE_CONTROL.lowerCase(), "no-cache")),
					200,
					Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));
			count.incrementAndGet();
		}
		var time = watch.elapsed().toMillis();
		var rate = count.get() / (time / 1000.0);
		log.info(String.format("Requests: %s, time: %s, rate: %.2f/s", count.get(), watch.elapsed(), rate));
		assertEquals(expectedRate, rate, expectedRate / 5.0);
	}

	@ParameterizedTest
	@MethodSource("rateLimitTests")
	@SneakyThrows
	void shouldNotLimitCachedResponses(String url, double expectedRate) {
		var count = new AtomicInteger(0);
		var watch = Stopwatch.createStarted();
		TestHttpUtils.assertResponse(
				TestHttpUtils.callProxy(client, proxy, "GET", url),
				200,
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));
		while (watch.elapsed().compareTo(duration) < 0) {
			TestHttpUtils.assertResponse(
					TestHttpUtils.callProxy(client, proxy, "GET", url),
					200,
					Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_HIT));
			count.incrementAndGet();
		}
		var time = watch.elapsed().toMillis();
		var rate = count.get() / (time / 1000.0);
		log.info(String.format("Requests: %s, time: %s, rate: %.2f/s", count.get(), watch.elapsed(), rate));
		assertTrue(rate > 100.0, "rate:" + rate);
	}

	public static Stream<Arguments> rateLimitTests() {
		return Stream.of(
				Arguments.of("/latest/characters/1452072530/corporationhistory/", 5.0),
				Arguments.of("/latest/markets/10000002/history/?datasource=tranquility&type_id=37", 10.0),
				Arguments.of("/sovereignty/campaigns/", 20.0));
	}
}
