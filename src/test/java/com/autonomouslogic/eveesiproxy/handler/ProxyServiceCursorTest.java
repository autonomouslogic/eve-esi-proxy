package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.Setter;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

/**
 * Tests automatic cursor-based pagination handling.
 * See <a href="https://developers.eveonline.com/docs/services/esi/pagination/cursor-based/">Cursor-based Pagination</a>
 */
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@Timeout(30)
@Log4j2
public class ProxyServiceCursorTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer mockEsi;
	CursorDispatcher dispatcher;

	@Inject
	protected ProxyServiceCursorTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);
		mockEsi = new MockWebServer();
		dispatcher = new CursorDispatcher();
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

	@Test
	@SneakyThrows
	@SuppressWarnings("unchecked")
	void shouldFollowBeforeCursors() {
		mockEsi.setDispatcher(dispatcher);
		dispatcher.setFirstResponse(new MockResponse()
				.setResponseCode(200)
				.setBody(createObjectWithCursor("before-1", "after-1", List.of("a", "b", "c"))
						.toString()));
		dispatcher.addBeforeResponse("before-1", createObjectWithCursor("before-2", "after-2", List.of("d", "e", "f")));
		dispatcher.addBeforeResponse("before-2", createObjectWithCursor(null, "after-2", List.of("g", "h", "i")));

		try (var proxyResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/cursor")) {
			assertEquals(200, proxyResponse.code());
			var responseBody = proxyResponse.body();
			assertNotNull(responseBody);
			var jsonString = responseBody.string();
			var json = (ObjectNode) objectMapper.readTree(jsonString);

			var records = (List<String>) objectMapper.convertValue(
					json.get("records"),
					objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
			assertEquals(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i"), records);

			// Preserve the original after cursor.
			assertEquals("after-1", json.get("cursor").get("after").asText());
			assertNull(json.get("cursor").get("before"));
		}

		var firstRequest = TestHttpUtils.takeRequest(mockEsi);
		assertNull(firstRequest.getRequestUrl().queryParameter("before"));
		assertNull(firstRequest.getRequestUrl().queryParameter("after"));

		var secondRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals("before-1", secondRequest.getRequestUrl().queryParameter("before"));
		assertNull(secondRequest.getRequestUrl().queryParameter("after"));

		var thirdRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals("before-2", thirdRequest.getRequestUrl().queryParameter("before"));
		assertNull(thirdRequest.getRequestUrl().queryParameter("after"));
	}

	@CartesianTest
	@ValueSource(strings = {"before", "after"})
	@SneakyThrows
	void shouldNotFollowCursorsIfSpecificallySupplied(
			@CartesianTest.Values(strings = {"before", "after"}) String cursorType,
			@CartesianTest.Values(strings = {"cursor-0", ""}) String cursorValue) {
		mockEsi.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody(createObjectWithCursor("before-1", "after-1", List.of("a", "b", "c"))
						.toString()));

		try (var proxyResponse =
				TestHttpUtils.callProxy(client, proxy, "GET", "/cursor?" + cursorType + "=" + cursorValue)) {
			assertEquals(200, proxyResponse.code());
		}

		var esiRequest = TestHttpUtils.takeRequest(mockEsi);
		assertEquals(cursorValue, esiRequest.getRequestUrl().queryParameter(cursorType));
	}

	@Test
	@SneakyThrows
	void shouldPassHeadersAndQueryStringArgsOnSubsequentRequests() {
		mockEsi.setDispatcher(dispatcher);
		dispatcher.setFirstResponse(new MockResponse()
				.setResponseCode(200)
				.setBody(createObjectWithCursor("before-1", "after-1", List.of("a"))
						.toString()));
		dispatcher.addBeforeResponse("before-1", createObjectWithCursor(null, null, List.of("b")));

		try (var proxyResponse =
				TestHttpUtils.callProxy(client, proxy, "GET", "/cursor?foo=bar", Map.of("X-Foo", "Bar"))) {
			assertEquals(200, proxyResponse.code());
		}

		var esiRequests = List.of(TestHttpUtils.takeRequest(mockEsi), TestHttpUtils.takeRequest(mockEsi));
		for (var esiRequest : esiRequests) {
			assertEquals("bar", esiRequest.getRequestUrl().queryParameter("foo"));
			assertEquals("Bar", esiRequest.getHeader("X-Foo"));
		}
	}

	private ObjectNode createObjectWithCursor(String beforeCursor, String afterCursor, List<String> records) {
		var pageJson = objectMapper.createObjectNode();
		pageJson.set("records", objectMapper.valueToTree(records));

		if (beforeCursor != null || afterCursor != null) {
			var cursor = objectMapper.createObjectNode();
			if (beforeCursor != null) {
				cursor.put("before", beforeCursor);
			}
			if (afterCursor != null) {
				cursor.put("after", afterCursor);
			}
			pageJson.set("cursor", cursor);
		}

		return pageJson;
	}

	private static class CursorDispatcher extends Dispatcher {
		@Setter
		private MockResponse firstResponse;

		private final Map<String, MockResponse> beforeResponses = new HashMap<>();

		void addBeforeResponse(@NonNull String cursor, @NonNull MockResponse response) {
			beforeResponses.put(cursor, response);
		}

		void addBeforeResponse(@NonNull String cursor, @NonNull ObjectNode node) {
			beforeResponses.put(cursor, new MockResponse().setResponseCode(200).setBody(node.toString()));
		}

		@NotNull
		@Override
		public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
			if (firstResponse != null) {
				var r = firstResponse;
				firstResponse = null;
				return r;
			}

			var url = recordedRequest.getRequestUrl();
			var cursor = url.queryParameter("before");

			var response = beforeResponses.get(cursor);
			if (response == null) {
				var msg = "No response configured for cursor: %s".formatted(cursor);
				log.error(msg);
				return new MockResponse().setResponseCode(404).setBody(msg);
			}
			return response;
		}
	}
}
