package com.autonomouslogic.eveesiproxy.http;

import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Replaces <code>cache-control: private</code> with <code>cache-control: no-store</code> in incoming responses.
 * The reason is that OkHttp's cache considers itself a private cache.
 * ESI will return a <code>private</code> cache header to authed requests, which OkHttp will cache and make available
 * later without proper auth.
 */
@Singleton
@Log4j2
public class PrivateCacheInterceptor implements Interceptor {
	@Inject
	protected PrivateCacheInterceptor() {}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var res = chain.proceed(chain.request());
		var cacheControl = CacheControl.parse(res.headers());
		if (cacheControl.isPrivate() && !cacheControl.noStore()) {
			log.trace("Forcing no-store on private cache response");
			var cacheControlString = res.header(HeaderNames.CACHE_CONTROL.lowerCase());
			cacheControlString = cacheControlString.replace("private", "private, no-store");
			res = res.newBuilder()
					.header(HeaderNames.CACHE_CONTROL.lowerCase(), cacheControlString)
					.build();
		}
		return res;
	}
}
