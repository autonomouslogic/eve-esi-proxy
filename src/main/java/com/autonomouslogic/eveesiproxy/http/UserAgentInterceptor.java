package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
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

	private final Set<String> seenUserAgents = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final Optional<String> configuredUserAgent =
			Configs.ESI_USER_AGENT.get().map(String::trim).filter(h -> !h.isEmpty());
	private String defaultUserAgent;

	@Inject
	protected UserAgentInterceptor() {}

	@Inject
	protected void init() {
		if (configuredUserAgent.isEmpty() || configuredUserAgent.get().indexOf('@') == -1) {
			log.warn(Configs.ESI_USER_AGENT.getName() + " should contain an email address");
		}
		defaultUserAgent = getDefaultUserAgent();
	}

	public String getDefaultUserAgent() {
		return "EveEsiProxy/%s (+https://github.com/autonomouslogic/eve-esi-proxy)".formatted(version);
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		var req = chain.request();
		var suppliedAgent = getSuppliedAgent(req);
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
		String userAgent = resolveUserAgent(suppliedAgent);
		reqBuilder.header(HeaderNames.USER_AGENT.lowerCase(), userAgent);
		logUserAgent(userAgent);
		return chain.proceed(reqBuilder.build());
	}

	private static Optional<String> getSuppliedAgent(Request req) {
		var suppliedAgent = tryHeader(req, HeaderNames.USER_AGENT.lowerCase())
				.or(() -> tryHeader(req, "X-User-Agent"))
				.or(() -> tryQuery(req, "user_agent"));
		return suppliedAgent;
	}

	private static Optional<String> tryHeader(Request req, String header) {
		return cleanString(Optional.ofNullable(req.header(header)));
	}

	private static Optional<String> tryQuery(Request req, String query) {
		return cleanString(Optional.ofNullable(req.url().queryParameter(query)));
	}

	private static Optional<String> cleanString(Optional<String> opt) {
		return opt.map(String::trim).filter(s -> !s.isEmpty());
	}

	private String resolveUserAgent(Optional<String> suppliedAgent) {
		String userAgent;
		if (suppliedAgent.isPresent() && configuredUserAgent.isPresent()) {
			userAgent = suppliedAgent.get() + " " + configuredUserAgent.get() + " " + defaultUserAgent;
		} else if (suppliedAgent.isPresent()) {
			userAgent = suppliedAgent.get() + " " + defaultUserAgent;
		} else {
			userAgent = configuredUserAgent.get() + " " + defaultUserAgent;
		}
		return userAgent;
	}

	private void logUserAgent(String userAgent) {
		if (seenUserAgents.add(userAgent)) {
			log.debug("New user agent seen: {}", userAgent);
		}
	}
}
