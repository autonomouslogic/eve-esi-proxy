package com.autonomouslogic.eveesiproxy.oauth;

import static com.autonomouslogic.eveesiproxy.configs.Configs.LOG_LEVEL;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.http.UserAgentInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.AuthorizationUrlBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.pkce.PKCE;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @link <a href="https://docs.esi.evetech.net/docs/sso/native_sso_flow.html">OAuth 2.0 for Mobile or Desktop Applications</a>
 * @link <a href="https://docs.esi.evetech.net/docs/sso/web_based_sso_flow.html">OAuth 2.0 for Web Based Applications</a>
 * @link <a href="https://auth0.com/docs/get-started/authentication-and-authorization-flow/which-oauth-2-0-flow-should-i-use">Which OAuth 2.0 Flow Should I Use?</a>
 * @link <a href="https://docs.esi.evetech.net/docs/sso/revoking_refresh_tokens.html">Revoking Refresh Tokens</a>
 */
@Log4j2
public class EsiAuthHelper {
	private static final Duration EXPIRATION_BUFFER = Duration.ofMinutes(1);

	// @todo should be configurable when logging in
	public static final List<String> SCOPES;

	static {
		try (var in = ResourceUtil.loadResource("/esi-scopes")) {
			var scopes = new ArrayList<>(IOUtils.readLines(in, StandardCharsets.UTF_8).stream()
					.filter(e -> !e.isEmpty())
					.limit(66)
					.toList());
			if (!scopes.contains("publicData")) {
				scopes.add("publicData");
			}
			SCOPES = Collections.unmodifiableList(scopes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Inject
	protected OkHttpClient client;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UserAgentInterceptor userAgentInterceptor;

	private final String clientId = Configs.EVE_OAUTH_CLIENT_ID.getRequired();
	private final String esiBaseUrl = Configs.ESI_BASE_URL.getRequired();
	private final String callbackUrl = Configs.EVE_OAUTH_CALLBACK_URL.getRequired();

	private final Cache<String, Pair<OAuth2AccessToken, Instant>> tokenCache =
			CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).build();

	private final Map<String, LoginState> stateMemory = new ConcurrentHashMap<>();

	private final OAuth20Service service;
	private final AuthFlow authFlow;

	@Inject
	protected EsiAuthHelper() {
		var secretKey = Configs.EVE_OAUTH_SECRET_KEY.get();
		var serviceBuilder = new ServiceBuilder(clientId)
				.defaultScope(String.join(" ", SCOPES))
				.callback(callbackUrl);
		var logLevel = LOG_LEVEL.getRequired().toUpperCase();
		if (logLevel.equals("TRACE") || logLevel.equals("DEBUG")) {
			serviceBuilder.debug();
		}
		if (secretKey.isPresent()) {
			serviceBuilder.apiSecret(secretKey.get());
			authFlow = AuthFlow.CODE;
		} else {
			authFlow = AuthFlow.PKCE;
		}
		service = serviceBuilder.build(new EsiApi20());
	}

	@SneakyThrows
	public URI getLoginUri() {
		var state = createState();
		var builder = service.createAuthorizationUrlBuilder().state(state);
		if (authFlow == AuthFlow.PKCE) {
			builder = builder.initPKCE();
		}
		var url = builder.build();
		log.trace("Creating login URI with flow {}, state: {} - {}", authFlow, state, url);
		var pkce = builder.getPkce();
		if (pkce != null) {
			logPkceForState(state, pkce);
		}
		stateMemory.put(state, new LoginState(Instant.now(), builder));
		return new URI(url);
	}

	@SneakyThrows
	public OAuth2AccessToken getAccessToken(String state, String code) {
		var loginState = stateMemory.remove(state);
		if (loginState == null) {
			log.trace("State {} not found", state);
			throw new RuntimeException("Invalid state");
		}
		var params = AccessTokenRequestParams.create(code);
		log.trace("Getting access token with flow {} for state: {}, code {}", authFlow, state, code);
		if (authFlow == AuthFlow.PKCE) {
			var pkce = loginState.getAuthorizationUrlBuilder().getPkce();
			logPkceForState(state, pkce);
			params.pkceCodeVerifier(pkce.getCodeVerifier());
		}
		return service.getAccessToken(params);
	}

	@SneakyThrows
	public OAuth2AccessToken refreshAccessToken(long characterId, @NonNull String refreshToken) {
		log.trace("Refreshing access token for characterId {}", characterId);
		return service.refreshAccessToken(refreshToken);
	}

	@SneakyThrows
	public EsiVerifyResponse verify(@NonNull String token) {
		log.trace("Verifying token: {}", token);
		var url = new URL(new URL(esiBaseUrl), "/verify/");
		var request = new Request.Builder()
				.get()
				.url(url)
				.header(HeaderNames.USER_AGENT.lowerCase(), userAgentInterceptor.getVersionHeaderPart())
				.header("Authorization", "Bearer " + token)
				.build();
		EsiVerifyResponse verify;
		try (var response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.warn("Failed to verify token: {}", response);
				throw new RuntimeException("Failed to verify token");
			}
			var b = response.body().string();
			try {
				verify = objectMapper.readValue(b, EsiVerifyResponse.class);
			} catch (Exception e) {
				log.warn("Failed to parse verify response: {}", b, e);
				throw e;
			}
		}
		return verify;
	}

	private String createState() {
		var state = new byte[128 / 8];
		new SecureRandom().nextBytes(state);
		return Hex.encodeHexString(state);
	}

	public OAuth2AccessToken getAccessToken(AuthedCharacter authedCharacter) {
		var cached = tokenCache.getIfPresent(authedCharacter.getCharacterOwnerHash());
		if (cached != null) {
			var issued = cached.getRight();
			var expiresIn = cached.getLeft().getExpiresIn();
			var expiration = issued.plusSeconds(expiresIn).minus(EXPIRATION_BUFFER);
			if (Instant.now().isBefore(expiration)) {
				log.trace("Using cached token for characterId {}", authedCharacter.getCharacterId());
				return cached.getLeft();
			} else {
				log.trace("Cached token expired for characterId {}", authedCharacter.getCharacterId());
			}
		}
		var token = refreshAccessToken(authedCharacter.getCharacterId(), authedCharacter.getRefreshToken());
		tokenCache.put(authedCharacter.getCharacterOwnerHash(), Pair.of(token, Instant.now()));
		return token;
	}

	private static void logPkceForState(String state, PKCE pkce) {
		log.trace(
				"PKCE for state {} - challenge method: {}, code challenge: {}, code verifier: {}",
				state,
				pkce.getCodeChallengeMethod(),
				pkce.getCodeChallenge(),
				pkce.getCodeVerifier());
	}

	@Value
	private static class LoginState {
		Instant created;
		AuthorizationUrlBuilder authorizationUrlBuilder;
	}
}
