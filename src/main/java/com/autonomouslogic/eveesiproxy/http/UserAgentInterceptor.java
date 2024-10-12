package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

/**
 * Trace logs calls
 */
@Singleton
@Log4j2
public class UserAgentInterceptor implements Interceptor {
	@Inject
	@Named("version")
	protected String version;

	private final Optional<String> configuredUserAgent =
			Configs.ESI_USER_AGENT.get().map(String::trim).filter(h -> !h.isEmpty());
	private String versionHeaderPart;

	@Inject
	protected UserAgentInterceptor() {}

	@Inject
	protected void init() {
		if (configuredUserAgent.isEmpty() || configuredUserAgent.get().indexOf('@') == -1) {
			log.warn(Configs.ESI_USER_AGENT.getName() + " should contain an email address");
		}
		versionHeaderPart = "eve-esi-proxy/" + version;
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var req = chain.request();
		var suppliedAgent = Optional.ofNullable(req.header(HeaderNames.USER_AGENT.lowerCase()))
				.filter(h -> !h.trim().isEmpty());
		if (suppliedAgent.isEmpty() && configuredUserAgent.isEmpty()) {
			return new Response.Builder()
					.code(400)
					.body(ResponseBody.create(
							"User agent must be configured or header supplied", MediaType.get("text/plain")))
					.protocol(Protocol.HTTP_1_1)
					.request(req)
					.message("Missing user-agent")
					.build();
		}
		var reqBuilder = req.newBuilder().removeHeader(HeaderNames.USER_AGENT.lowerCase());
		String userAgent;
		if (suppliedAgent.isPresent() && configuredUserAgent.isPresent()) {
			userAgent = suppliedAgent.get() + " " + configuredUserAgent.get() + " " + versionHeaderPart;
		} else if (suppliedAgent.isPresent()) {
			userAgent = suppliedAgent.get() + " " + versionHeaderPart;
		} else {
			userAgent = configuredUserAgent.get() + " " + versionHeaderPart;
		}
		reqBuilder.header(HeaderNames.USER_AGENT.lowerCase(), userAgent);
		return chain.proceed(reqBuilder.build());
	}
}
