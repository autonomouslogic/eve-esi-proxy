package com.autonomouslogic.esiproxy;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Handles requests to the ESI API.
 */
@Singleton
@Log4j2
public class EsiRelay {
	private final URL esiBaseUrl = Optional.ofNullable(System.getenv("ESI_BASE_URL"))
			.or(() -> Optional.of("https://esi.evetech.net/"))
			.map(url -> url.trim())
			.map(url -> {
				try {
					return new URL(url);
				} catch (Exception e) {
					throw new RuntimeException("ESI_BASE_URL environment variable must be a valid URL", e);
				}
			})
			.orElseThrow(() -> new RuntimeException("ESI_BASE_URL environment variable must be set"));

	private final OkHttpClient client;

	public EsiRelay() {
		client = new OkHttpClient.Builder().build();
	}

	/**
	 * Relays the provided HTTP request to the ESI API.
	 * @param proxyRequest
	 * @return
	 */
	public Single<HttpResponse<byte[]>> request(HttpRequest<byte[]> proxyRequest) {
		return Single.defer(() -> {
			var url = new URL(esiBaseUrl, proxyRequest.getPath());
			var body = proxyRequest.getBody().orElse(null);
			var requestBody = body == null ? null : RequestBody.create(body);
			var requestBuilder = new Request.Builder()
					.url(url)
					.method(proxyRequest.getMethod().name(), requestBody);
			proxyRequest
					.getHeaders()
					.forEach((name, values) -> values.forEach(value -> requestBuilder.addHeader(name, value)));
			var esiRequest = requestBuilder.build();
			return Single.create(new SingleOnSubscribe<HttpResponse<byte[]>>() {
						@Override
						public void subscribe(@NonNull SingleEmitter<HttpResponse<byte[]>> emitter) throws Throwable {
							client.newCall(esiRequest).enqueue(new Callback() {
								@Override
								public void onFailure(@NotNull Call call, @NotNull IOException e) {
									emitter.onError(e);
								}

								@Override
								public void onResponse(@NotNull Call call, @NotNull Response esiResponse)
										throws IOException {
									var httpStatus = HttpStatus.valueOf(esiResponse.code());
									var proxyResponse = HttpResponse.<byte[]>status(httpStatus);
									esiResponse
											.headers()
											.forEach(pair -> proxyResponse.header(pair.getFirst(), pair.getSecond()));
									var esiBody = esiResponse.body();
									if (esiBody != null) {
										proxyResponse.body(esiBody.bytes());
									}
									emitter.onSuccess(proxyResponse);
								}
							});
						}
					})
					.observeOn(Schedulers.computation());
		});
	}
}
