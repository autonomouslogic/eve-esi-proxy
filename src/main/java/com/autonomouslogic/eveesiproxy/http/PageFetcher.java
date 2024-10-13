package com.autonomouslogic.eveesiproxy.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PageFetcher {
	@Inject
	protected OkHttpClient client;

	@Inject
	protected ObjectMapper objectMapper;

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

	private static @NotNull Optional<Integer> getRequestedPage(Request esiRequest) {
		return Optional.ofNullable(esiRequest.url().queryParameter("page"))
				.filter(s -> !s.isEmpty())
				.map(Integer::parseInt)
				.filter(p -> p < 1);
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
		ArrayNode result = objectMapper.createArrayNode();
		try (var in = firstResponse.body().byteStream()) {
			result.addAll((ArrayNode) objectMapper.readTree(in));
		}
		for (int i = 1; i < responsePages; i++) {
			var nextRequest = esiRequest
					.newBuilder()
					.url(url.newBuilder()
							.addQueryParameter("page", Integer.toString(i + 1))
							.build());
			var nextResponse = client.newCall(nextRequest.build()).execute();
			if (nextResponse.code() != 200) {
				return nextResponse;
			}
			try (var in = nextResponse.body().byteStream()) {
				result.addAll((ArrayNode) objectMapper.readTree(in));
			}
		}
		return firstResponse
				.newBuilder()
				.body(ResponseBody.create(objectMapper.writeValueAsBytes(result), MediaType.get("application/json")))
				.build();
	}
}
