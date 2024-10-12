package com.autonomouslogic.eveesiproxy.http;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
public class PageFetcher {
	@Inject
	protected OkHttpClient client;

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
		return esiResponse;
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
}
