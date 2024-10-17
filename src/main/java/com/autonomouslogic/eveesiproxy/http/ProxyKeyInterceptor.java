package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import com.autonomouslogic.eveesiproxy.oauth.EsiAuthHelper;
import dagger.Lazy;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Handles translation between proxy API keys to ESI OAuth tokens.
 */
@Singleton
@Log4j2
public class ProxyKeyInterceptor implements Interceptor {
	@Inject
	protected Lazy<AuthManager> authManager;

	@Inject
	protected Lazy<EsiAuthHelper> esiAuthHelper;

	@Inject
	protected ProxyKeyInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var req = chain.request();
		var authorization = req.header(HeaderNames.AUTHORIZATION.lowerCase());
		if (authorization != null && authorization.toLowerCase().startsWith("bearer ")) {
			var token = authorization.substring(7);
			var character = authManager.get().getCharacterForProxyKey(token);
			if (character.isPresent()) {
				log.trace("Found proxy key, requesting ESI token");
				var accessToken =
						esiAuthHelper.get().getAccessToken(character.get()).getAccessToken();
				req = req.newBuilder()
						.removeHeader(HeaderNames.AUTHORIZATION.lowerCase())
						.addHeader(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer " + accessToken)
						.build();
			}
		}
		return chain.proceed(req);
	}
}
