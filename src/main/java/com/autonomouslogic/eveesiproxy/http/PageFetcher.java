package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.util.VirtualThreads;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

@Singleton
@Log4j2
public class PageFetcher {
	@Inject
	protected OkHttpClient client;

	@Inject
	protected ObjectMapper objectMapper;

	private final int maxConcurrentPages = Configs.HTTP_MAX_CONCURRENT_PAGES.getRequired();

	@Inject
	protected PageFetcher() {}

	public boolean shouldFetchPages(Request esiRequest, Response esiResponse) {
		if (esiResponse.code() != 200) {
			return false;
		}
		if (getRequestedPage(esiRequest).isPresent()) {
			return false;
		}
		return getResponsePages(esiResponse) > 1;
	}

	public Response fetchSubPages(Request esiRequest, Response esiResponse) {
		log.debug(
				"Request for {} did not contain a page query parameter, {} pages seen",
				esiRequest.url(),
				getResponsePages(esiResponse));
		return fetch(esiRequest, esiResponse, getResponsePages(esiResponse));
	}

	private static Optional<Integer> getRequestedPage(Request esiRequest) {
		return getRequestedPage(esiRequest.url());
	}

	private static Optional<Integer> getRequestedPage(HttpUrl esiUrl) {
		return Optional.ofNullable(esiUrl.queryParameter("page"))
				.filter(s -> !s.isEmpty())
				.map(Integer::parseInt);
	}

	private static int getResponsePages(Response esiResponse) {
		return Optional.ofNullable(esiResponse.header(ProxyHeaderNames.X_PAGES))
				.filter(s -> !s.isEmpty())
				.map(Integer::parseInt)
				.orElse(1);
	}

	@SneakyThrows
	private Response fetch(Request firstRequest, Response firstResponse, int pages) {
		final var pageResults = new ConcurrentHashMap<Integer, ArrayNode>(pages);
		readResponse(firstResponse, 1, pageResults);

		var failure = new AtomicBoolean(false);
		var failedResponses = Flowable.range(2, pages - 1)
				.takeWhile(i -> !failure.get())
				.parallel(maxConcurrentPages, 1)
				.runOn(VirtualThreads.SCHEDULER)
				.flatMapIterable(page -> {
					if (failure.get()) {
						return List.of();
					}
					var nextResponse = nextRequest(firstRequest, page);
					if (nextResponse.code() != 200) {
						failure.set(true);
						return List.of(nextResponse);
					}
					readResponse(nextResponse, page, pageResults);
					return List.of();
				})
				.sequential()
				.toList()
				.blockingGet();

		var failedResponse = getFailedResponse(failedResponses);
		if (failedResponse.isPresent()) {
			return failedResponse.get();
		}

		var result = mergePages(pages, pageResults);

		return new Response.Builder()
				.request(firstRequest)
				.protocol(firstResponse.protocol())
				.message("merged pages")
				.code(200)
				.header(ProxyHeaderNames.X_EVE_ESI_PAGES_FETCHED, Integer.toString(pages))
				.body(ResponseBody.create(objectMapper.writeValueAsBytes(result), MediaType.get("application/json")))
				.build();
	}

	private ArrayNode mergePages(int responsePages, Map<Integer, ArrayNode> pageResults) {
		var result = objectMapper.createArrayNode();
		for (int i = 0; i < responsePages; i++) {
			result.addAll(pageResults.get(i));
		}
		return result;
	}

	private static Optional<Response> getFailedResponse(List<Response> failedResponses) {
		if (!failedResponses.isEmpty()) {
			if (failedResponses.size() > 1) {
				for (int i = 1; i < failedResponses.size(); i++) {
					failedResponses.get(i).close();
				}
			}
			return Optional.of(failedResponses.getFirst());
		}
		return Optional.empty();
	}

	@SneakyThrows
	private @NotNull Response nextRequest(Request firstRequest, Integer page) {
		var nextRequest = firstRequest
				.newBuilder()
				.url(firstRequest
						.url()
						.newBuilder()
						.setQueryParameter("page", Integer.toString(page))
						.build());
		try (var nextResponse = OkHttpExec.execute(client.newCall(nextRequest.build()))) {
			return nextResponse;
		}
	}

	@SneakyThrows
	private void readResponse(Response response, int page, Map<Integer, ArrayNode> pageResults) {
		try (var in = response.body().byteStream()) {
			var array = (ArrayNode) objectMapper.readTree(in);
			pageResults.put(page - 1, array);
		}
	}

	public HttpUrl removeInvalidPageQueryString(HttpUrl esiUrl) {
		var page = getRequestedPage(esiUrl);
		if (page.isPresent() && page.get() < 1) {
			esiUrl = esiUrl.newBuilder().removeAllQueryParameters("page").build();
		}
		return esiUrl;
	}
}
