package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
public class PageFetcher {
	@Inject
	protected OkHttpClient client;

	@Inject
	protected ObjectMapper objectMapper;

	private final int maxConcurrent = Configs.HTTP_MAX_CONCURRENT_PAGES.getRequired();

	@Inject
	protected PageFetcher() {}

	public Response fetchSubPages(Request esiRequest, Response esiResponse) {
		if (esiResponse.code() != 200) {
			return esiResponse;
		}
		var requestedPage = getRequestedPage(esiRequest);
		if (requestedPage.isPresent()) {
			return esiResponse;
		}
		var responsePages = getResponsePages(esiResponse);
		if (responsePages == 1) {
			return esiResponse;
		}
		return fetch(esiRequest, esiResponse, responsePages);
	}

	private static Optional<Integer> getRequestedPage(Request esiRequest) {
		return getRequestedPage(esiRequest.url());
	}

	private static Optional<Integer> getRequestedPage(HttpUrl esiUrl) {
		return Optional.ofNullable(esiUrl.queryParameter("page"))
				.filter(s -> !s.isEmpty())
				.map(Integer::parseInt)
		//			.filter(p -> p >= 1)
		;
	}

	private static int getResponsePages(Response esiResponse) {
		return Optional.ofNullable(esiResponse.header(ProxyHeaderNames.X_PAGES))
				.filter(s -> !s.isEmpty())
				.map(Integer::parseInt)
				.orElse(1);
	}

	@SneakyThrows
	private Response fetch(Request esiRequest, Response firstResponse, int responsePages) {
		var url = esiRequest.url();
		var pageResults = new ConcurrentHashMap<Integer, ArrayNode>(responsePages + 1);
		try (var in = firstResponse.body().byteStream()) {
			var array = (ArrayNode) objectMapper.readTree(in);
			pageResults.put(0, array);
		}
		for (int i = 1; i < responsePages; i++) {
			var nextRequest = esiRequest
					.newBuilder()
					.url(url.newBuilder()
							.setQueryParameter("page", Integer.toString(i + 1))
							.build());
			var nextResponse = client.newCall(nextRequest.build()).execute();
			if (nextResponse.code() != 200) {
				return nextResponse;
			}
			try (var in = nextResponse.body().byteStream()) {
				var array = (ArrayNode) objectMapper.readTree(in);
				pageResults.put(i, array);
			}
		}
		var result = objectMapper.createArrayNode();
		for (int i = 0; i < responsePages; i++) {
			result.addAll(pageResults.get(i));
		}
		return new Response.Builder()
				.request(esiRequest)
				.protocol(firstResponse.protocol())
				.message("merged pages")
				.code(200)
				.header(ProxyHeaderNames.X_PAGES, Integer.toString(responsePages))
				.body(ResponseBody.create(objectMapper.writeValueAsBytes(result), MediaType.get("application/json")))
				.build();
	}

	public HttpUrl removeInvalidPageQueryString(HttpUrl esiUrl) {
		var page = getRequestedPage(esiUrl);
		if (page.isPresent() && page.get() < 1) {
			esiUrl = esiUrl.newBuilder().removeAllQueryParameters("page").build();
		}
		return esiUrl;
	}
}
