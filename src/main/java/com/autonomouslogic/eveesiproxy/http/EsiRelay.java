package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import io.helidon.http.HttpPrologue;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Handles requests to the ESI API.
 */
@Singleton
@Log4j2
public class EsiRelay {
	@Inject
	protected OkHttpClient client;

	@Inject
	protected PageFetcher pageFetcher;

	private final URL esiBaseUrl;

	@Inject
	@SneakyThrows
	protected EsiRelay() {
		esiBaseUrl = new URL(Configs.ESI_BASE_URL.getRequired());
	}

	/**
	 * Relays the provided HTTP request to the ESI API.
	 *
	 * @param proxyRequest
	 * @param res
	 *
	 * @return
	 */
	@SneakyThrows
	public void relayRequest(ServerRequest proxyRequest, ServerResponse res) {
		var esiRequest = createEsiRequest(proxyRequest).build();
		var esiResponse = client.newCall(esiRequest).execute();
		esiResponse = pageFetcher.fetchSubPages(esiRequest, esiResponse);
		sendResponse(esiResponse, res);
	}

	private static void sendResponse(Response esiResponse, ServerResponse res) throws IOException {
		res.status(esiResponse.code());
		esiResponse.headers().forEach(pair -> res.header(pair.getFirst(), pair.getSecond()));
		res.send(esiResponse.body().bytes());
	}

	private Request.Builder createEsiRequest(ServerRequest proxyRequest) throws MalformedURLException {
		var prologue = proxyRequest.prologue();
		var esiUrl = createUrl(prologue);
		var esiRequestBody = createRequestBody(proxyRequest);
		var esiRequestBuilder =
				new Request.Builder().url(esiUrl).method(prologue.method().toString(), esiRequestBody);
		copyHeaders(proxyRequest, esiRequestBuilder, esiUrl);
		return esiRequestBuilder;
	}

	private @Nullable HttpUrl createUrl(HttpPrologue prologue) throws MalformedURLException {
		var url = URI.create(
				esiBaseUrl + prologue.uriPath().toString() + prologue.query().toString());
		var esiUrl = HttpUrl.get(url);
		esiUrl = pageFetcher.removeInvalidPageQueryString(esiUrl);
		return esiUrl;
	}

	private static void copyHeaders(ServerRequest proxyRequest, Request.Builder esiRequestBuilder, HttpUrl esiUrl) {
		esiRequestBuilder.header("Host", esiUrl.host() + ":" + esiUrl.port());
		proxyRequest.headers().forEach(header -> {
			if (header.name().equalsIgnoreCase("Host")) {
				return;
			}
			esiRequestBuilder.addHeader(header.name(), header.get());
		});
	}

	@SneakyThrows
	private static RequestBody createRequestBody(ServerRequest proxyRequest) {
		var content = proxyRequest.content();
		if (content == null || !content.hasEntity()) {
			return null;
		}
		byte[] bytes;
		try (var in = proxyRequest.content().inputStream()) {
			bytes = IOUtils.toByteArray(in);
		}
		if (bytes == null) {
			return null;
		}
		return RequestBody.create(bytes);
	}
}
