package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ServiceRateLimitInterceptor;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(60)
@Log4j2
public class ProxyServiceServiceRateLimitTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	MockWebServer mockEsi;

	volatile boolean isLimited;
	Instant limitResetTime;

	@Inject
	protected ProxyServiceServiceRateLimitTest() {}

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

	@Test
	@SneakyThrows
	void shouldStopRequests() {
		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				if (isLimited) {
					var response = new MockResponse().setResponseCode(429);
					if (limitResetTime != null) {
						response.setHeader(
								ServiceRateLimitInterceptor.RETRY_AFTER,
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
							TestHttpUtils.callProxy(client, proxy, "GET", "/page")
									.close();
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
			// Execute 429 request, which should initiate a global stop.
			log.info("Returning 429");
			limitResetTime = Instant.now().plusSeconds(5);
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

	@ParameterizedTest
	@ValueSource(ints = {3})
	@SneakyThrows
	void shouldHandleMultipleStopRequests(final int stops) {
		var stopLeft = new AtomicInteger(0);
		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				var s = stopLeft.incrementAndGet();
				log.info("Stopping request {}", s);
				if (s <= stops) {
					return new MockResponse()
							.setResponseCode(429)
							.setHeader(ServiceRateLimitInterceptor.RETRY_AFTER, 1);
				}
				return new MockResponse().setResponseCode(200).setBody("success body");
			}
		});

		try (var response = TestHttpUtils.callProxy(client, proxy, "GET", "/page")) {
			assertEquals(200, response.code());
			assertEquals("success body", response.body().string());
		}
	}

	@Test
	@SneakyThrows
	void shouldOnlyStopRequestsToSameRateLimitGroup() {
		var group1RequestCount = new AtomicInteger(0);
		var group2RequestCount = new AtomicInteger(0);
		var noGroupRequestCount = new AtomicInteger(0);
		var requestOrder = new ConcurrentLinkedQueue<String>();

		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest request) {
				var path = request.getPath();
				log.info("Request to: {}", path);

				if (path.startsWith("/latest/characters/12345/contacts")) {
					var count = group1RequestCount.incrementAndGet();
					requestOrder.add("contacts-" + count);
					// Return 429 only on first request
					if (count == 1) {
						return new MockResponse()
								.setResponseCode(429)
								.setHeader("x-ratelimit-group", "char-social")
								.setHeader(ServiceRateLimitInterceptor.RETRY_AFTER, 2);
					}
					return new MockResponse().setResponseCode(200).setBody("group1");
				} else if (path.startsWith("/latest/characters/12345/wallet")) {
					group2RequestCount.incrementAndGet();
					requestOrder.add("wallet");
					return new MockResponse().setResponseCode(200).setBody("group2");
				} else if (path.startsWith("/latest/characters/12345/assets")) {
					noGroupRequestCount.incrementAndGet();
					requestOrder.add("assets");
					return new MockResponse().setResponseCode(200).setBody("no-group");
				}

				return new MockResponse().setResponseCode(404);
			}
		});

		var group1Success = new AtomicBoolean(false);
		var group2Success = new AtomicBoolean(false);
		var noGroupSuccess = new AtomicBoolean(false);

		// Task 1: Request to char-social group (will get rate limited)
		var task1 = (Runnable) () -> {
			try {
				try (var response =
						TestHttpUtils.callProxy(client, proxy, "GET", "/latest/characters/12345/contacts")) {
					assertEquals(200, response.code());
					assertEquals("group1", response.body().string());
					group1Success.set(true);
				}
			} catch (Exception e) {
				log.info("Task 1 error", e);
			}
		};

		// Task 2: Request to char-wallet group (different group, should not be blocked)
		var task2 = (Runnable) () -> {
			try {
				Thread.sleep(500); // Wait to ensure task1 triggers rate limit
				try (var response =
						TestHttpUtils.callProxy(client, proxy, "GET", "/latest/characters/12345/wallet")) {
					assertEquals(200, response.code());
					assertEquals("group2", response.body().string());
					group2Success.set(true);
				}
			} catch (Exception e) {
				log.info("Task 2 error", e);
			}
		};

		// Task 3: Request to URL without rate limit group (should not be blocked)
		var task3 = (Runnable) () -> {
			try {
				Thread.sleep(500); // Wait to ensure task1 triggers rate limit
				try (var response =
						TestHttpUtils.callProxy(client, proxy, "GET", "/latest/characters/12345/assets")) {
					assertEquals(200, response.code());
					assertEquals("no-group", response.body().string());
					noGroupSuccess.set(true);
				}
			} catch (Exception e) {
				log.info("Task 3 error", e);
			}
		};

		var futures = List.of(
				CompletableFuture.runAsync(task1), CompletableFuture.runAsync(task2), CompletableFuture.runAsync(task3));

		// Wait for all tasks to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		// Verify all tasks completed successfully
		assertTrue(group1Success.get(), "Group 1 request should succeed after rate limit");
		assertTrue(group2Success.get(), "Group 2 request should not be blocked");
		assertTrue(noGroupSuccess.get(), "No-group request should not be blocked");

		// Group 1 should have multiple requests (initial 429 + retry)
		assertTrue(group1RequestCount.get() > 1, "Group 1 should have retry attempts");

		// Group 2 and no-group should only have one request each (not blocked)
		assertEquals(1, group2RequestCount.get(), "Group 2 should not be retried");
		assertEquals(1, noGroupRequestCount.get(), "No-group should not be retried");

		// Verify request order: wallet and assets should arrive BEFORE contacts-2 (the retry)
		// This proves they were NOT blocked by group1's rate limit
		var orderedRequests = new ArrayList<>(requestOrder);
		log.info("Request order: {}", orderedRequests);

		assertEquals("contacts-1", orderedRequests.get(0), "First request should be contacts (gets 429)");
		// Wallet and assets should come before the retry
		var walletIndex = orderedRequests.indexOf("wallet");
		var assetsIndex = orderedRequests.indexOf("assets");
		var contactsRetryIndex = orderedRequests.indexOf("contacts-2");

		assertTrue(walletIndex < contactsRetryIndex, "Wallet request should arrive before contacts retry (not blocked)");
		assertTrue(assetsIndex < contactsRetryIndex, "Assets request should arrive before contacts retry (not blocked)");

		// Verify total request count to mock server
		var totalRequests = mockEsi.getRequestCount();
		assertEquals(4, totalRequests, "Total requests to mock server should be 4 (2 for group1, 1 each for group2 and no-group)");
	}

	@Test
	@SneakyThrows
	void shouldStopMultipleRequestsToSameRateLimitGroup() {
		var contactsCount = new AtomicInteger(0);
		var calendarCount = new AtomicInteger(0);
		var requestOrder = new ConcurrentLinkedQueue<String>();

		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest request) {
				var path = request.getPath();
				log.info("Request to: {}", path);

				if (path.startsWith("/latest/characters/12345/contacts")) {
					var count = contactsCount.incrementAndGet();
					requestOrder.add("contacts-" + count);
					// Return 429 only on first request
					if (count == 1) {
						return new MockResponse()
								.setResponseCode(429)
								.setHeader("x-ratelimit-group", "char-social")
								.setHeader(ServiceRateLimitInterceptor.RETRY_AFTER, 2);
					}
					return new MockResponse().setResponseCode(200).setBody("contacts");
				} else if (path.startsWith("/latest/characters/12345/calendar")) {
					calendarCount.incrementAndGet();
					requestOrder.add("calendar");
					return new MockResponse().setResponseCode(200).setBody("calendar");
				}

				return new MockResponse().setResponseCode(404);
			}
		});

		var task1Success = new AtomicBoolean(false);
		var task2Success = new AtomicBoolean(false);

		// Both tasks request different URLs in the same group (char-social)
		var task1 = (Runnable) () -> {
			try {
				try (var response =
						TestHttpUtils.callProxy(client, proxy, "GET", "/latest/characters/12345/contacts")) {
					assertEquals(200, response.code());
					assertEquals("contacts", response.body().string());
					task1Success.set(true);
				}
			} catch (Exception e) {
				log.info("Task 1 error", e);
			}
		};

		var task2 = (Runnable) () -> {
			try {
				Thread.sleep(500); // Slight delay to ensure task1 triggers rate limit
				try (var response =
						TestHttpUtils.callProxy(client, proxy, "GET", "/latest/characters/12345/calendar")) {
					assertEquals(200, response.code());
					assertEquals("calendar", response.body().string());
					task2Success.set(true);
				}
			} catch (Exception e) {
				log.info("Task 2 error", e);
			}
		};

		var futures = List.of(CompletableFuture.runAsync(task1), CompletableFuture.runAsync(task2));

		// Wait for both tasks to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		assertTrue(task1Success.get(), "Task 1 should succeed");
		assertTrue(task2Success.get(), "Task 2 should be blocked and then succeed");

		// Both URLs are in the same group, so task2 should wait for task1's rate limit
		assertTrue(contactsCount.get() >= 2, "Contacts should have initial 429 + retry");
		assertEquals(1, calendarCount.get(), "Calendar should only be called once after waiting");

		// Verify request order: calendar should arrive AFTER contacts-2 (the retry)
		// This proves it WAS blocked by the same group's rate limit
		var orderedRequests = new ArrayList<>(requestOrder);
		log.info("Request order: {}", orderedRequests);

		assertEquals("contacts-1", orderedRequests.get(0), "First request should be contacts (gets 429)");
		assertEquals(
				"contacts-2",
				orderedRequests.get(1),
				"Second request should be contacts retry (after rate limit wait)");
		assertEquals("calendar", orderedRequests.get(2), "Third request should be calendar (blocked until retry completes)");

		// Verify total request count to mock server
		var totalRequests = mockEsi.getRequestCount();
		assertEquals(3, totalRequests, "Total requests to mock server should be 3 (2 for contacts, 1 for calendar)");
	}
}
