package com.autonomouslogic.eveesiproxy;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
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
	@Inject
	@Named("version")
	protected String version;

	private final URL esiBaseUrl;
	private final Cache cache;
	private final OkHttpClient client;

	private final Optional<String> configuredUserAgent =
			Configs.ESI_USER_AGENT.get().map(String::trim).filter(h -> !h.isEmpty());
	private String versionHeaderPart;

	@Inject
	@SneakyThrows
	protected EsiRelay() {
		esiBaseUrl = new URL(Configs.ESI_BASE_URL.getRequired());
		final File tempDir;
		var httpCacheDir = Configs.HTTP_CACHE_DIR.get();
		var httpCacheMaxSize = Configs.HTTP_CACHE_MAX_SIZE.getRequired();
		if (httpCacheDir.isPresent()) {
			tempDir = new File(httpCacheDir.get());
		} else {
			tempDir = Files.createTempDirectory("eve-esi-proxy-http-cache").toFile();
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

	@Inject
	protected void init() {
		if (configuredUserAgent.isPresent()) {
			if (configuredUserAgent.get().indexOf('@') == -1) {
				log.warn(Configs.ESI_USER_AGENT.getName() + " should contain an email address");
			}
		}
		versionHeaderPart = "eve-esi-proxy/" + version;
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
		var esiRequestBuilder = createEsiRequest(proxyRequest);
		if (!handleUserAgent(proxyRequest, res, esiRequestBuilder)) {
			return;
		}
		var esiRequest = esiRequestBuilder.build();
		var esiResponse = client.newCall(esiRequest).execute();
		sendResponse(esiResponse, res);
	}

	private static void sendResponse(Response esiResponse, ServerResponse res) throws IOException {
		res.status(esiResponse.code());
		esiResponse.headers().forEach(pair -> res.header(pair.getFirst(), pair.getSecond()));
		res.send(esiResponse.body().bytes());
	}

	private Request.Builder createEsiRequest(ServerRequest proxyRequest) throws MalformedURLException {
		var esiUrl = new URL(esiBaseUrl, proxyRequest.path().path());
		var esiRequestBody = createRequestBody(proxyRequest);
		var esiRequestBuilder = new Request.Builder()
				.url(esiUrl)
				.method(proxyRequest.prologue().method().toString(), esiRequestBody);
		copyHeaders(proxyRequest, esiRequestBuilder, esiUrl);
		return esiRequestBuilder;
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

	private boolean handleUserAgent(ServerRequest proxyRequest, ServerResponse res, Request.Builder esiRequestBuilder) {
		var suppliedAgent = Optional.ofNullable(proxyRequest.headers().get(HeaderNames.USER_AGENT))
				.flatMap(h -> Optional.ofNullable(h.values()))
				.filter(h -> !h.trim().isEmpty());
		if (suppliedAgent.isEmpty() && configuredUserAgent.isEmpty()) {
			res.status(400).send("User agent must be configured or header supplied");
			return false;
		}
		esiRequestBuilder.removeHeader(HeaderNames.USER_AGENT.lowerCase());
		String userAgent;
		if (suppliedAgent.isPresent() && configuredUserAgent.isPresent()) {
			userAgent = suppliedAgent.get() + " " + configuredUserAgent.get() + " " + versionHeaderPart;
		} else if (suppliedAgent.isPresent()) {
			userAgent = suppliedAgent.get() + " " + versionHeaderPart;
		} else {
			userAgent = configuredUserAgent.get() + " " + versionHeaderPart;
		}
		esiRequestBuilder.addHeader(HeaderNames.USER_AGENT.lowerCase(), userAgent);
		return true;
	}

	@SneakyThrows
	public void clearCache() {
		cache.evictAll();
	}
}
