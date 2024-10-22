package com.autonomouslogic.eveesiproxy.handler;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import com.autonomouslogic.eveesiproxy.oauth.AuthedCharacter;
import com.autonomouslogic.eveesiproxy.oauth.EsiAuthHelper;
import com.autonomouslogic.eveesiproxy.ui.TemplateUtil;
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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
	protected TemplateUtil templateUtil;

	private final int port = Configs.PROXY_PORT.getRequired();

	@Inject
	protected UiService() {}

	@Override
	public void routing(HttpRules httpRules) {
		log.trace("Configuring routing for {}", this.getClass().getSimpleName());
		httpRules.get("/", new RootHandler());
		httpRules.any("/", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);

		httpRules.get("/login", new LoginHandler());
		httpRules.any("/login", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);

		httpRules.get("/characters/{character_id}", new CharacterHandler());
		httpRules.any("/characters/{character_id}", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}

	class RootHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			var authedCharacters = authManager.getAuthedCharacters();
			var html = templateUtil.render("index", Map.of("authedCharacters", authedCharacters));
			standardHeaders
					.apply(res)
					.header(HeaderNames.CONTENT_TYPE.lowerCase(), "text/html")
					.send(html);
		}
	}

	class LoginHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			var characterId = Optional.of(req.requestedUri().query())
				.filter(q -> q.contains("characterId"))
				.map(q -> q.get("characterId"))
				.map(Long::parseLong);
			var character = characterId.flatMap(authManager::getCharacterForCharacterId);
			var currentScopes = character.map(AuthedCharacter::getScopes);

			if (characterId.isPresent() && character.isEmpty()) {
				standardHeaders.apply(res).status(404).send("Character not found");
			}

			var scopeGroups = EsiAuthHelper.ALL_SCOPES.stream()
					.filter(s -> !s.equals("publicData"))
					.collect(Collectors.groupingBy(
							s -> {
								var split = s.split("\\.");
								if (split.length == 1) {
									throw new IllegalStateException("Scope %s has no group".formatted(s));
								}
								return split[0];
							},
							TreeMap::new,
							Collectors.toList()));
			for (var group : scopeGroups.keySet()) {
				var groups = scopeGroups.get(group);
				groups.sort(String::compareTo);
			}

			var html = templateUtil.render("login", Map.of(
				"scopeGroups", scopeGroups,
				"character", character,
				"currentScopes", currentScopes.orElse(List.of())));
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
			var exampleCurl = "curl \"http://localhost:%s/latest/characters/%s/blueprints?token=%s\""
					.formatted(port, characterId, authedCharacter.getProxyKey());
			var html =
					templateUtil.render("character", Map.of("character", authedCharacter, "exampleCurl", exampleCurl));
			standardHeaders
					.apply(res)
					.header(HeaderNames.CONTENT_TYPE.lowerCase(), "text/html")
					.send(html);
		}
	}
}
