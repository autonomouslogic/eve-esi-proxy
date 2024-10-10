package com.autonomouslogic.esiproxy;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

/**
 * Handles requests to the ESI API.
 */
@Singleton
@Log4j2
public class EsiRelay {
	private final URL esiBaseUrl;

	private final Cache cache;
	private final OkHttpClient client;

	@Inject
	@SneakyThrows
	public EsiRelay() {
		esiBaseUrl = new URL(Configs.ESI_BASE_URL.getRequired());
		final File tempDir;
		var httpCacheDir = Configs.ESI_PROXY_HTTP_CACHE_DIR.get();
		var httpCacheMaxSize = Configs.ESI_PROXY_HTTP_CACHE_MAX_SIZE.getRequired();
		if (httpCacheDir.isPresent()) {
			tempDir = new File(httpCacheDir.get());
		} else {
			tempDir = Files.createTempDirectory("esi-proxy-http-cache").toFile();
		}
		log.info("Using HTTP cache dir {}", tempDir);
		cache = new Cache(tempDir, httpCacheMaxSize);
		client = new OkHttpClient.Builder()
				.followRedirects(false)
				.followSslRedirects(false)
				.cache(cache)
				.addInterceptor(new CacheStatusInterceptor())
				.build();
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
		var esiRequest = createEsiRequest(proxyRequest);
		var esiResponse = client.newCall(esiRequest).execute();
		sendResponse(esiResponse, res);
	}

	private static void sendResponse(Response esiResponse, ServerResponse res) throws IOException {
		res.status(esiResponse.code());
		esiResponse.headers().forEach(pair -> res.header(pair.getFirst(), pair.getSecond()));
		res.send(esiResponse.body().bytes());
	}

	private Request createEsiRequest(ServerRequest proxyRequest) throws MalformedURLException {
		var esiUrl = new URL(esiBaseUrl, proxyRequest.path().path());
		var esiRequestBody = createRequestBody(proxyRequest);
		var esiRequestBuilder = new Request.Builder()
				.url(esiUrl)
				.method(proxyRequest.prologue().method().toString(), esiRequestBody);
		copyHeaders(proxyRequest, esiRequestBuilder, esiUrl);
		return esiRequestBuilder.build();
	}

	private static void copyHeaders(ServerRequest proxyRequest, Request.Builder esiRequestBuilder, URL esiUrl) {
		esiRequestBuilder.header("Host", esiUrl.getHost() + ":" + esiUrl.getPort());
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

	@SneakyThrows
	public void clearCache() {
		cache.evictAll();
	}
}
