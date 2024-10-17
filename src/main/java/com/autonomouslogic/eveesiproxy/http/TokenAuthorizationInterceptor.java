package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Moves the <code>&token=</code> query string to the <code>Authorization</code> header.
 */
@Singleton
@Log4j2
public class TokenAuthorizationInterceptor implements Interceptor {
	private static final String TOKEN = "token";

	@Inject
	protected AuthManager authManager;

	@Inject
	protected TokenAuthorizationInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var req = chain.request();
		var url = req.url();
		var token = url.queryParameter(TOKEN);
		if (token != null) {
			if (req.header(HeaderNames.AUTHORIZATION.lowerCase()) != null) {
				log.trace("Found token in query string, but Authorization header already set, not transferring auth");
			} else {
				log.trace("Found token in query string, moving it to Authorization header");
				req = req.newBuilder()
						.url(url.newBuilder().removeAllQueryParameters(TOKEN).build())
						.removeHeader(HeaderNames.AUTHORIZATION.lowerCase())
						.addHeader(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer " + token)
						.build();
			}
		}
		return chain.proceed(req);
	}
}
