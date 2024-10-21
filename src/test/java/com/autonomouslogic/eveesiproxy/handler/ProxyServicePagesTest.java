package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.HttpDate;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
@Timeout(30)
@Log4j2
public class ProxyServicePagesTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer mockEsi;

	@Inject
	protected ProxyServicePagesTest() {}

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
		try {
			TestHttpUtils.assertNoMoreRequests(mockEsi);
		} finally {
			try {
				proxy.stop();
			} finally {
				mockEsi.shutdown();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "", "0", "-1"})
	@SneakyThrows
	void shouldFetchAllSubPages(String page) {
		List<ArrayNode> pagesJson = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			pagesJson.add(createPage(i));
		}
		var expectedArray = objectMapper.createArrayNode();
		for (ArrayNode entries : pagesJson) {
			expectedArray.addAll(entries);
		}

		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				var url = recordedRequest.getRequestUrl();
				var page = Optional.ofNullable(url.queryParameter("page"))
						.filter(s -> !s.isEmpty())
						.map(Integer::parseInt)
						.orElse(1);
				if (page < 1 || page > pagesJson.size()) {
					log.error("Bad page: {}", page);
					return new MockResponse().setResponseCode(404).setBody("Page does not exist");
				}
				return new MockResponse()
						.setResponseCode(200)
						.setBody(pagesJson.get(page - 1).toString())
						.addHeader(HeaderNames.CONTENT_TYPE.lowerCase(), "application/json")
						.addHeader(ProxyHeaderNames.X_PAGES, Integer.toString(pagesJson.size()))
						.addHeader(HeaderNames.LAST_MODIFIED.lowerCase(), HttpDate.format(ZonedDateTime.now()))
						.addHeader(
								HeaderNames.EXPIRES.lowerCase(),
								HttpDate.format(ZonedDateTime.now().plusHours(1)))
						.addHeader(HeaderNames.ETAG.lowerCase(), "hash-" + page);
			}
		});

		var initialPageQuery = page.equals("null") ? "" : "&page=" + page;
		var proxyResponse = TestHttpUtils.callProxy(
				client,
				proxy,
				"GET",
				"/latest/markets/10000002/orders/?datasource=tranquility" + initialPageQuery
						+ "&order_type=all&type_id=37");
		assertEquals(200, proxyResponse.code());
		var responseBody = proxyResponse.body();
		assertNotNull(responseBody);
		var suppliedJson = responseBody.string();
		var suppliedArray = (ArrayNode) objectMapper.readTree(suppliedJson);
		assertEquals(expectedArray.size(), suppliedArray.size());
		assertEquals(expectedArray, suppliedArray);

		assertNull(proxyResponse.header(HeaderNames.LAST_MODIFIED.lowerCase()));
		assertNull(proxyResponse.header(HeaderNames.ETAG.lowerCase()));
		assertNull(proxyResponse.header(HeaderNames.EXPIRES.lowerCase()));
		assertEquals(Integer.toString(pagesJson.size()), proxyResponse.header(ProxyHeaderNames.X_PAGES));
		assertNull(proxyResponse.header(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS));

		for (int i = 0; i < pagesJson.size(); i++) {
			assertNotNull(TestHttpUtils.takeRequest(mockEsi));
		}
	}

	private ArrayNode createPage(int page) {
		var array = objectMapper.createArrayNode();
		for (int i = 0; i < 2; i++) {
			array.add(createEntry(page, i));
		}
		return array;
	}

	@SneakyThrows
	private ObjectNode createEntry(int page, int entry) {
		var json = objectMapper.createObjectNode().put("order_id", (page + 1) * 100 + entry);
		// Re-encode the JSON to ensure the types in the tree are as the client would see them.
		return (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsBytes(json));
	}

	@Test
	@SneakyThrows
	void shouldNotFetchPagesIfAPageIsRequested() {
		TestHttpUtils.enqueueResponse(
				mockEsi,
				200,
				"[{\"order_id\":1},{\"order_id\":2}]",
				Map.of(ProxyHeaderNames.X_PAGES, "10", "x-server-header", "single"));

		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi?page=1");
		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				"[{\"order_id\":1},{\"order_id\":2}]",
				Map.of(ProxyHeaderNames.X_PAGES, "10", "x-server-header", "single"));

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		TestHttpUtils.assertRequest(esiRequest, "GET", "/esi?page=1");
	}

	@Test
	@SneakyThrows
	void shouldNotFetchPagesIfOnlyOnePageIsAvailable() {
		TestHttpUtils.enqueueResponse(
				mockEsi,
				200,
				"[{\"order_id\":1},{\"order_id\":2}]",
				Map.of(ProxyHeaderNames.X_PAGES, "1", "x-server-header", "single"));

		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				"[{\"order_id\":1},{\"order_id\":2}]",
				Map.of(ProxyHeaderNames.X_PAGES, "1", "x-server-header", "single"));

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		TestHttpUtils.assertRequest(esiRequest, "GET", "/esi");
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
	@SneakyThrows
	@SetEnvironmentVariable(key = "HTTP_MAX_CONCURRENT_PAGES", value = "4")
	void shouldNotFetchPagesIfThereAreErrors(int errorPage) {
		var permanentErrorPage = 5; // To test errors on multiple pages during concurrent fetches.
		mockEsi.setDispatcher(new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
				var url = recordedRequest.getRequestUrl();
				var page = Optional.ofNullable(url.queryParameter("page"))
						.map(Integer::parseInt)
						.orElse(1);
				if (page == errorPage || page == permanentErrorPage) {
					return new MockResponse().setResponseCode(400).setHeader("x-server-header", "error");
				} else {
					return new MockResponse()
							.setHeadersDelay(100, TimeUnit.MILLISECONDS)
							.setResponseCode(200)
							.addHeader(ProxyHeaderNames.X_PAGES, "10")
							.setBody("[{\"order_id\":1}]");
				}
			}
		});

		var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/esi");
		TestHttpUtils.assertResponse(proxyResponse, 400, Map.of("x-server-header", "error"));

		RecordedRequest esiRequest;
		int i = 0;
		while ((esiRequest = TestHttpUtils.takeRequest(mockEsi)) != null) {
			// TestHttpUtils.assertRequest(esiRequest, "GET", "/esi");
			i++;
		}
		assertTrue(i <= 10, "i:" + i);
	}
}
