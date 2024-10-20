package com.autonomouslogic.eveesiproxy.handler;

import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Collectors;

@Singleton
@Log4j2
public class UiService implements HttpService {
	public static final String BASE_PATH = "/esiproxy";

	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	protected AuthManager authManager;

	@Inject
	@Named("version")
	protected String version;

	@Inject
	protected UiService() {}

	@Override
	public void routing(HttpRules httpRules) {
		log.trace("Configuring routing for {}", this.getClass().getSimpleName());
		httpRules.get("/", new RootHandler());
		httpRules.any("/", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}

	class RootHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			// https://images.evetech.net/characters/1338057886/portrait
			var authedCharacters = authManager.getAuthedCharacters();
			var authedCharactersHtml = authedCharacters.isEmpty()
				? "<i>No characters logged in</i>"
				: "<ul>"
				+ authedCharacters.stream()
				.map(ac -> "<li>%s [%d] - proxy key: <pre>%s</pre></li>"
					.formatted(ac.getCharacterName(), ac.getCharacterId(), ac.getProxyKey()))
				.collect(Collectors.joining("\n"))
				+ "</ul>";
			var html = """
			<h1>EVE ESI Proxy %s</h1>
			<p><a href="%s/login">Login</a></p>
			%s
			""".formatted(version, BASE_PATH,  authedCharactersHtml);
			standardHeaders
				.apply(res)
				.header(HeaderNames.CONTENT_TYPE.lowerCase(), "text/html")
				.send(html);
		}
	}
}
