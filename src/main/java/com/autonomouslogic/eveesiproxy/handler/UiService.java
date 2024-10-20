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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

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
		httpRules.get("/characters/{character_id}", new CharacterHandler());
		httpRules.any("/characters/{character_id}", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
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
									.map(ac -> "<li><a href=\"%s/characters/%s\">%s</a> [%d]</li>"
											.formatted(
													UiService.BASE_PATH,
													ac.getCharacterId(),
													ac.getCharacterName(),
													ac.getCharacterId()))
									.collect(Collectors.joining("\n"))
							+ "</ul>";
			var html = """
			<h1>EVE ESI Proxy %s</h1>
			<p><a href="%s/login">Login</a></p>
			%s
			"""
					.formatted(version, BASE_PATH, authedCharactersHtml);
			standardHeaders
					.apply(res)
					.header(HeaderNames.CONTENT_TYPE.lowerCase(), "text/html")
					.send(html);
		}
	}

	class CharacterHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			var characterId = Long.parseLong(req.path().pathParameters().get("character_id"));
			var authedCharacter = authManager.getAuthedCharacter(characterId);
			if (authedCharacter == null) {
				standardHeaders
						.apply(res)
						.status(404)
						.header(HeaderNames.CONTENT_TYPE.lowerCase(), "text/plain")
						.send("Character %s not found".formatted(characterId));
				return;
			}
			var scopes = String.join(
					", ", Optional.ofNullable(authedCharacter.getScopes()).orElse(List.of()));
			var html =
					"""
			<h1>%s</h1>
			<p>Character ID: %s</p>
			<p>Proxy key: <pre>%s</pre></p>
			<p>Scopes: %s</p>
			<p><a href="/">Home</a></p>
			"""
							.formatted(
									authedCharacter.getCharacterName(),
									characterId,
									authedCharacter.getProxyKey(),
									scopes);
			standardHeaders
					.apply(res)
					.header(HeaderNames.CONTENT_TYPE.lowerCase(), "text/html")
					.send(html);
		}
	}
}
