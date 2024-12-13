package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ErrorLimitInterceptor;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(30)
@Log4j2
class ProxyServiceErrorLimitTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	MockWebServer mockEsi;

	volatile boolean isLimited;
	String limitBody;
	int limitStatus;
	Instant limitResetTime;

	@Inject
	protected ProxyServiceErrorLimitTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		mockEsi = new MockWebServer();
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
	@ValueSource(strings = {"code", "text"})
	@SneakyThrows
	void shouldStopRequests(String type) {
		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				if (isLimited) {
					var response = new MockResponse().setResponseCode(limitStatus);
					if (limitBody != null) {
						response.setBody(limitBody);
					}
					if (limitResetTime != null) {
						response.setHeader(
								ErrorLimitInterceptor.ERROR_LIMIT_RESET,
								Duration.between(Instant.now(), limitResetTime)
										.truncatedTo(ChronoUnit.SECONDS)
										.toSeconds());
					}
					return response;
				}
				return new MockResponse().setResponseCode(200);
			}
		});

		var threads = new ArrayList<Thread>();
		var stop = new AtomicBoolean(false);
		try {
			var count = new AtomicInteger(0);
			// Start threads to constantly query.
			for (int i = 0; i < 4; i++) {
				var thread = new Thread(() -> {
					while (!stop.get()) {
						try {
							TestHttpUtils.callProxy(client, proxy, "GET", "/page");
						} catch (Exception e) {
							log.warn("Fail", e);
						}
						count.incrementAndGet();
					}
					log.debug("Thread stopped");
				});
				threads.add(thread);
				thread.start();
			}
			// Ensure requests are running.
			log.info("Running");
			Thread.sleep(500);
			log.info("Count (1): " + count.get());
			assertNotEquals(0, count.get());
			// Execute 420 request, which should initiate a global stop.
			log.info("Returning 420");
			limitResetTime = Instant.now().plusSeconds(5);
			switch (type) {
				case "code":
					limitStatus = 420;
					limitBody = null;
					break;
				case "text":
					limitStatus = 200;
					limitBody = ErrorLimitInterceptor.ESI_420_TEXT;
					break;
			}
			isLimited = true;
			log.info("Count (2): " + count.get());
			Thread.sleep(1500);
			count.set(0);
			log.info("Count (3): " + count.get());
			Thread.sleep(3500);
			log.info("Count (4): " + count.get());
			assertEquals(0, count.get());
			isLimited = false;
			// Ensure requests are running again.
			log.info("Resuming");
			Thread.sleep(5000);
			log.info("Count (6): " + count.get());
			assertNotEquals(0, count.get());
		} finally {
			stop.set(true);
			threads.forEach(thread -> {
				try {
					thread.interrupt();
				} catch (Exception e) {
					log.warn("Failed to stop thread", e);
				}
			});
		}
	}
}
