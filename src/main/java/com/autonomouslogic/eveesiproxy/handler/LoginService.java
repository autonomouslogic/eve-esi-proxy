package com.autonomouslogic.eveesiproxy.handler;

import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import com.autonomouslogic.eveesiproxy.oauth.AuthedCharacter;
import com.autonomouslogic.eveesiproxy.oauth.EsiAuthHelper;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class LoginService implements HttpService {
	@Inject
	protected EsiAuthHelper esiAuthHelper;

	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	protected AuthManager authManager;

	@Inject
	protected LoginService() {}

	@Override
	public void routing(HttpRules httpRules) {
		log.trace("Configuring routing for {}", this.getClass().getSimpleName());
		httpRules.get("/", new LoginHandler());
		httpRules.get("/callback", new CallbackHandler());
	}

	private class LoginHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			var redirect = esiAuthHelper.getLoginUri();
			log.debug("Redirecting login to {}", redirect);
			standardHeaders
					.apply(res)
					.status(307)
					.header("Location", redirect.toString())
					.send();
		}
	}

	private class CallbackHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			log.trace("Received callback: {}", req.requestedUri());
			var query = req.query();
			var code = query.getRaw("code");
			var state = query.getRaw("state");
			log.debug("Callback code: {}, state: {}", code, state);
			var token = esiAuthHelper.getAccessToken(state, code);
			var verify = esiAuthHelper.verify(token.getAccessToken());

			authManager.addAuthedCharacter(AuthedCharacter.builder()
					.characterId(verify.getCharacterId())
					.characterName(verify.getCharacterName())
					.characterOwnerHash(verify.getCharacterOwnerHash())
					.refreshToken(token.getRefreshToken())
					.proxyKey(authManager.generateProxyKey())
					.scopes(verify.getScopes())
					.build());

			//			var characterLogin = CharacterLogin.builder()
			//				.characterId(verify.getCharacterId())
			//				.characterName(verify.getCharacterName())
			//				.characterOwnerHash(verify.getCharacterOwnerHash())
			//				.refreshToken(token.getRefreshToken())
			//				.scopes(verify.getScopes())
			//				.build();
			// esiAuthHelper.putCharacterLogin(characterLogin).blockingAwait();

			//			var body = String.format(
			//					"""
			//				<h1>EVE Ref Login</h1>
			//				<h2>OAuth2</h2>
			//				<ul>
			//					<li>accessToken: <code>%s</code></li>
			//					<li>tokenType: <code>%s</code></li>
			//					<li>expiresIn: <code>%s</code></li>
			//					<li>refreshToken: <code>%s</code></li>
			//					<li>scope: <code>%s</code></li>
			//				</ul>
			//				<h2>EVE Online</h2>
			//				<ul>
			//					<li>characterId: <code>%s</code></li>
			//					<li>characterName: <code>%s</code></li>
			//					<li>characterOwnerHash: <code>%s</code></li>
			//					<li>expiresOn: <code>%s</code></li>
			//					<li>scopes: <code>%s</code></li>
			//				</ul>
			//				""",
			//					token.getAccessToken(),
			//					token.getTokenType(),
			//					token.getExpiresIn(),
			//					token.getRefreshToken(),
			//					token.getScope(),
			//					verify.getCharacterId(),
			//					verify.getCharacterName(),
			//					verify.getCharacterOwnerHash(),
			//					verify.getExpiresOn(),
			//					verify.getScopes());

			standardHeaders
					.apply(res)
					.status(Status.TEMPORARY_REDIRECT_307)
					.header(
							HeaderNames.LOCATION.lowerCase(),
							UiService.BASE_PATH + "/characters/" + verify.getCharacterId())
					.send();
			//					.send(body.getBytes(StandardCharsets.UTF_8));
		}
	}
}
