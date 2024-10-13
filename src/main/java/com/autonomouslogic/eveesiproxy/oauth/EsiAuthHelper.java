package com.autonomouslogic.eveesiproxy.oauth;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.http.UserAgentInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.helidon.http.HeaderNames;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Hex;

@Log4j2
public class EsiAuthHelper {
	private static final Duration EXPIRATION_BUFFER = Duration.ofMinutes(1);
	private static final List<String> SCOPES = List.of( // @todo should be configurable when logging in
			"publicData");

	@Inject
	protected OkHttpClient client;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UserAgentInterceptor userAgentInterceptor;

	private final String esiBaseUrl = Configs.ESI_BASE_URL.getRequired();

	private final String callbackUrl = Configs.EVE_OAUTH_CALLBACK_URL.getRequired();

	//	private final Cache<String, Pair<OAuth2AccessToken, Instant>> tokenCache =
	//			CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(1)).build();

	private final OAuth20Service service;

	@Inject
	protected EsiAuthHelper() {
		var clientId = Configs.EVE_OAUTH_CLIENT_ID.getRequired();
		var secretKey = Configs.EVE_OAUTH_SECRET_KEY.get();
		var serviceBuilder = new ServiceBuilder(clientId)
				.defaultScope(String.join(" ", SCOPES))
				.callback(callbackUrl);
		secretKey.ifPresent(serviceBuilder::apiSecret);
		service = serviceBuilder.build(new EsiApi20());
	}

	@SneakyThrows
	public URI getLoginUri() {
		var state = new byte[128 / 8];
		new SecureRandom().nextBytes(state);
		return new URI(service.getAuthorizationUrl(Hex.encodeHexString(state)));
	}

	@SneakyThrows
	public OAuth2AccessToken getAccessToken(@NonNull String code) {
		return service.getAccessToken(code);
	}

	@SneakyThrows
	public OAuth2AccessToken refreshAccessToken(@NonNull String refreshToken) {
		return service.refreshAccessToken(refreshToken);
	}

	@SneakyThrows
	public EsiVerifyResponse verify(@NonNull String token) {
		var url = new URL(new URL(esiBaseUrl), "/verify/");
		var request = new Request.Builder()
				.get()
				.url(url)
				.header(HeaderNames.USER_AGENT.lowerCase(), userAgentInterceptor.getVersionHeaderPart())
				.header("Authorization", "Bearer " + token)
				.build();
		EsiVerifyResponse verify;
		try (var response = client.newCall(request).execute()) {
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

	//	@SneakyThrows
	//	public Completable putCharacterLogin(CharacterLogin characterLogin) {
	//		return Completable.defer(() -> Rx3Util.toSingle(dynamoAsyncMapper.putItemFromKeyObject(characterLogin))
	//				.ignoreElement());
	//	}

	//	@SneakyThrows
	//	public Maybe<CharacterLogin> getCharacterLogin(String ownerHash) {
	//		return Rx3Util.toMaybe(dynamoAsyncMapper.getItemFromPrimaryKey(ownerHash, CharacterLogin.class))
	//				.flatMap(r -> Maybe.fromOptional(Optional.ofNullable(r.item())));
	//	}

	//	public Maybe<OAuth2AccessToken> getTokenForOwnerHash(String ownerHash) {
	//		return Maybe.defer(() -> {
	//			var cached = tokenCache.getIfPresent(ownerHash);
	//			if (cached != null) {
	//				var issued = cached.getRight();
	//				var expiresIn = cached.getLeft().getExpiresIn();
	//				var expiration = issued.plusSeconds(expiresIn).minus(EXPIRATION_BUFFER);
	//				if (Instant.now().isBefore(expiration)) {
	//					return Maybe.just(cached.getLeft());
	//				}
	//			}
	//			log.debug("Refreshing token for ownerHash {}", ownerHash);
	//			return getCharacterLogin(ownerHash)
	//					.flatMapSingle(login -> refreshAccessToken(login.getRefreshToken()))
	//					.doOnSuccess(token -> {
	//						tokenCache.put(ownerHash, Pair.of(token, Instant.now()));
	//					});
	//		});
	//	}

	//	public Single<String> getTokenStringForOwnerHash(String ownerHash) {
	//		return getTokenForOwnerHash(ownerHash)
	//				.map(token -> token.getAccessToken())
	//				.switchIfEmpty((Maybe.defer(() -> Maybe.error(
	//						new RuntimeException(String.format("Login not found for owner hash: %s", ownerHash))))))
	//				.toSingle();
	//	}
}
