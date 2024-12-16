package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static com.autonomouslogic.eveesiproxy.test.TestHttpUtils.assertRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderValues;
import com.autonomouslogic.eveesiproxy.oauth.AuthFlow;
import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import com.autonomouslogic.eveesiproxy.oauth.AuthedCharacter;
import com.autonomouslogic.eveesiproxy.oauth.EsiVerifyResponse;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.scribejava.core.base64.Base64;
import com.google.common.hash.Hashing;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
@SetEnvironmentVariable(key = "ESI_USER_AGENT", value = "test@example.com")
@SetEnvironmentVariable(key = "EVE_OAUTH_CLIENT_ID", value = "client-id-1")
@SetEnvironmentVariable(
		key = "EVE_OAUTH_AUTHORIZATION_URL",
		value = "http://localhost:" + MOCK_ESI_PORT + "/v2/oauth/authorize")
@SetEnvironmentVariable(key = "EVE_OAUTH_TOKEN_URL", value = "http://localhost:" + MOCK_ESI_PORT + "/v2/oauth/token")
@Timeout(30)
@Log4j2
public class LoginServiceTest {
	@Inject
	EveEsiProxy proxy;

	@Inject
	@Named("test")
	OkHttpClient client;

	@Inject
	AuthManager authManager;

	@Inject
	ObjectMapper objectMapper;

	MockWebServer mockEsi;

	@Inject
	protected LoginServiceTest() {}

	@BeforeEach
	@SneakyThrows
	void setup() {
		DaggerTestComponent.builder().build().inject(this);

		authManager.addAuthedCharacter(AuthedCharacter.builder()
				.characterId(123456)
				.characterOwnerHash("owner-hash-1")
				.proxyKey("proxy-key-1")
				.refreshToken("refresh-token-1")
				.scopes(List.of("publicData", "esi-characters.read_blueprints.v1"))
				.build());

		mockEsi = new MockWebServer();
		mockEsi.start(MOCK_ESI_PORT);
		proxy.start();
	}

	@AfterEach
	@SneakyThrows
	void stop() {
		try {
			TestHttpUtils.assertNoMoreRequests(mockEsi);
		} finally {
			proxy.stop();
			mockEsi.shutdown();
		}
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "EVE_OAUTH_SECRET_KEY", value = "client-secret-1")
	void shouldHandleCodeFlowLogins() {
		testLoginFlow(AuthFlow.CODE);
	}

	@Test
	@SneakyThrows
	void shouldHandlePkceFlowLogins() {
		testLoginFlow(AuthFlow.PKCE);
	}

	@SneakyThrows
	void testLoginFlow(AuthFlow authFlow) {
		var characterId = 283764238;
		var scopes = List.of("publicData", "esi-alliances.read_contacts.v1", "esi-assets.read_assets.v1");

		// Login redirect to EVE auth.
		var loginResponse = TestHttpUtils.callProxy(
				client,
				proxy,
				"POST",
				"/esiproxy/login/redirect",
				scopes.stream()
						.filter(s -> !s.equals("publicData"))
						.map(s -> s + "=on")
						.collect(Collectors.joining("&")));
		assertEquals(307, loginResponse.code());
		var loginRedirect = HttpUrl.parse(loginResponse.header("location"));
		var codeChallenge = loginRedirect.queryParameter("code_challenge");
		var state = loginRedirect.queryParameter("state");
		assertEquals("localhost", loginRedirect.host());
		assertEquals(MOCK_ESI_PORT, loginRedirect.port());
		assertEquals("/v2/oauth/authorize", loginRedirect.encodedPath());
		if (authFlow == AuthFlow.PKCE) {
			assertEquals("S256", loginRedirect.queryParameter("code_challenge_method"));
		}
		assertEquals("client-id-1", loginRedirect.queryParameter("client_id"));
		assertEquals("http://localhost:8182/esiproxy/login/callback", loginRedirect.queryParameter("redirect_uri"));
		assertEquals(scopes, List.of(loginRedirect.queryParameter("scope").split(" ")));
		TestHttpUtils.assertNoMoreRequests(mockEsi);

		// Token response.
		TestHttpUtils.enqueueResponse(
				mockEsi,
				200,
				"""
		{
			"access_token": "access-token-1",
			"expires_in": 1199,
			"token_type": "Bearer",
			"refresh_token": "refresh-token-1"
		}
		""");
		// Verify response.
		TestHttpUtils.enqueueResponse(
				mockEsi,
				200,
				((ObjectNode) objectMapper.valueToTree(EsiVerifyResponse.builder()
								.characterId(characterId)
								.characterName("Test Character")
								.characterOwnerHash("owner-hash-1")
								.build()))
						.put("ExpiresOn", "2021-01-01T00:00:00")
						.put("Scopes", String.join(" ", scopes))
						.toString());

		// Execute callback.
		var callbackResponse = TestHttpUtils.callProxy(
				client, proxy, "GET", "/esiproxy/login/callback?code=auth-code-1&state=" + state);
		assertEquals(307, callbackResponse.code());
		assertEquals("/esiproxy/characters/" + characterId, callbackResponse.header(HeaderNames.LOCATION.lowerCase()));

		// Token request.
		var tokenRequest = TestHttpUtils.takeRequest(mockEsi);
		var tokenRequestBody = tokenRequest.getBody().readUtf8();
		var codeVerifier = Stream.of(tokenRequestBody.split("&"))
				.filter(e -> e.startsWith("code_verifier="))
				.map(e -> e.substring("code_verifier=".length()))
				.findFirst();
		if (authFlow == AuthFlow.PKCE) {
			codeVerifier.orElseThrow();
		}
		assertRequest(
				tokenRequest,
				"POST",
				"/v2/oauth/token",
				Map.of(HeaderNames.CONTENT_TYPE.lowerCase(), "application/x-www-form-urlencoded"));
		var tokenRequestParameters = new ArrayList<String>();
		tokenRequestParameters.add("client_id=client-id-1");
		if (authFlow == AuthFlow.CODE) {
			tokenRequestParameters.add("client_secret=client-secret-1");
		}
		tokenRequestParameters.addAll(List.of(
				"code=auth-code-1",
				"redirect_uri=http%3A%2F%2Flocalhost%3A8182%2Fesiproxy%2Flogin%2Fcallback",
				"grant_type=authorization_code"));
		if (authFlow == AuthFlow.PKCE) {
			tokenRequestParameters.add("code_verifier=" + codeVerifier.get());
		}
		assertEquals(String.join("&", tokenRequestParameters), tokenRequestBody);
		if (authFlow == AuthFlow.PKCE) {
			var hash = Hashing.sha256().hashString(codeVerifier.get(), StandardCharsets.UTF_8);
			var encoded = Base64.encodeUrlWithoutPadding(hash.asBytes());
			assertEquals(codeChallenge, encoded);
		}

		// Verify request.
		TestHttpUtils.assertRequest(
				TestHttpUtils.takeRequest(mockEsi),
				"GET",
				"/verify/",
				Map.of(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer access-token-1"));

		// Authed character.
		var authedCharacter = authManager.getAuthedCharacter(characterId);
		assertEquals(characterId, authedCharacter.getCharacterId());
		assertEquals("Test Character", authedCharacter.getCharacterName());
		assertEquals("owner-hash-1", authedCharacter.getCharacterOwnerHash());
		assertNotNull(authedCharacter.getProxyKey());
		assertEquals(scopes, authedCharacter.getScopes());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "EVE_OAUTH_SECRET_KEY", value = "client-secret-1")
	void shouldRequestAndTranslateOAuthTokensForProxyKeysForCodeFlow() {
		testProxyKeyTranslation(AuthFlow.CODE);
	}

	@Test
	@SneakyThrows
	void shouldRequestAndTranslateOAuthTokensForProxyKeysForPkceFlow() {
		testProxyKeyTranslation(AuthFlow.PKCE);
	}

	void testProxyKeyTranslation(AuthFlow authFlow) {
		// Token response.
		TestHttpUtils.enqueueResponse(
				mockEsi,
				200,
				"""
			{
				"access_token": "access-token-1",
				"expires_in": 1199,
				"token_type": "Bearer",
				"refresh_token": "refresh-token-1"
			}
			""");

		// ESI response.
		TestHttpUtils.enqueueResponse(mockEsi, 200, "Test response");

		var proxyResponse = TestHttpUtils.callProxy(
				client, proxy, "GET", "/esi", Map.of(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer proxy-key-1"));

		var oauthRequest = TestHttpUtils.takeRequest(mockEsi);
		var esiRequest = TestHttpUtils.takeRequest(mockEsi);

		var tokenRequestParameters = new ArrayList<String>();
		tokenRequestParameters.add("client_id=client-id-1");
		if (authFlow == AuthFlow.CODE) {
			tokenRequestParameters.add("client_secret=client-secret-1");
		}
		tokenRequestParameters.addAll(List.of("refresh_token=refresh-token-1", "grant_type=refresh_token"));
		assertRequest(
				oauthRequest,
				"POST",
				"/v2/oauth/token",
				Map.of(HeaderNames.CONTENT_TYPE.lowerCase(), "application/x-www-form-urlencoded"),
				String.join("&", tokenRequestParameters));

		assertRequest(
				esiRequest, "GET", "/esi", Map.of(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer access-token-1"));

		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				"Test response",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));
	}
}
