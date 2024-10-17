package com.autonomouslogic.eveesiproxy.http;

import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Authed responses from ESI come back with <code>cache-control: private</code>.
 * For good measure, just in case things change on the ESI, we add a <code>no-store</code> directive to the cache control
 * when an <code>Authorization</code> header was present.
 */
@Singleton
@Log4j2
public class AuthorizationNoStoreCacheInterceptor implements Interceptor {
	@Inject
	protected AuthorizationNoStoreCacheInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var req = chain.request();
		if (req.header(HeaderNames.AUTHORIZATION.lowerCase()) == null) {
			return chain.proceed(req);
		}
		var res = chain.proceed(chain.request());
		var cacheControl = CacheControl.parse(res.headers());
		if (!cacheControl.noStore()) {
			log.trace("Forcing no-store on authed response");
			var cacheControlString = Optional.ofNullable(res.header(HeaderNames.CACHE_CONTROL.lowerCase()))
					.map(s -> s + ", no-store")
					.orElse("no-store");
			res = res.newBuilder()
					.header(HeaderNames.CACHE_CONTROL.lowerCase(), cacheControlString)
					.build();
		}
		return res;
	}
}
