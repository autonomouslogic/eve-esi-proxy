package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;
import static com.autonomouslogic.eveesiproxy.test.TestHttpUtils.assertRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderValues;
import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import com.autonomouslogic.eveesiproxy.oauth.AuthedCharacter;
import com.autonomouslogic.eveesiproxy.oauth.EsiAuthHelper;
import com.autonomouslogic.eveesiproxy.oauth.EsiVerifyResponse;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
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
public class ProxyHandlerAuthTest {
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
	protected ProxyHandlerAuthTest() {}

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
		fail();
	}

	@Test
	@SneakyThrows
	void shouldHandlePkceFlowLogins() {
		var characterId = 283764238;
		// /login redirect:
		// https://login.eveonline.com/v2/oauth/authorize?code_challenge=OWuqPWZI_ZNf9wDzj5hrIVYEjsQ4MoX5cd42DfXUcCM&code_challenge_method=S256&response_type=code&client_id=89f0127100b74c28a5247d75e5f31d5e&redirect_uri=http%3A%2F%2Flocalhost%3A8182%2Flogin%2Fcallback&scope=publicData%20esi-characters.read_agents_research.v1%20esi-characters.read_blueprints.v1%20esi-characters.read_chat_channels.v1%20esi-characters.read_contacts.v1%20esi-characters.read_corporation_roles.v1%20esi-characters.read_fatigue.v1%20esi-characters.read_fw_stats.v1%20esi-characters.read_loyalty.v1%20esi-characters.read_medals.v1%20esi-characters.read_notifications.v1%20esi-characters.read_opportunities.v1%20esi-characters.read_standings.v1%20esi-characters.read_titles.v1%20esi-characters.write_contacts.v1&state=0c788750a26e0e345c22d9e2ce3e1419
		// Callback:
		// http://localhost:8182/login/callback?code=wxqZfYyJJ06pt0EJ0XNmJg&state=0c788750a26e0e345c22d9e2ce3e1419

		// Login redirect to EVE auth.
		var loginResponse = TestHttpUtils.callProxy(client, proxy, "GET", "/login");
		assertEquals(307, loginResponse.code());
		var loginRedirect = HttpUrl.parse(loginResponse.header("location"));
		var codeChallenge = loginRedirect.queryParameter("code_challenge");
		var state = loginRedirect.queryParameter("state");
		assertEquals("localhost", loginRedirect.host());
		assertEquals(MOCK_ESI_PORT, loginRedirect.port());
		assertEquals("/v2/oauth/authorize", loginRedirect.encodedPath());
		assertEquals("S256", loginRedirect.queryParameter("code_challenge_method"));
		assertEquals("client-id-1", loginRedirect.queryParameter("client_id"));
		assertEquals("http://localhost:8182/login/callback", loginRedirect.queryParameter("redirect_uri"));
		assertEquals(
				EsiAuthHelper.SCOPES,
				List.of(loginRedirect.queryParameter("scope").split(" ")));
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
				objectMapper.writeValueAsString(EsiVerifyResponse.builder()
						.characterId(characterId)
						.characterName("Test Character")
						.characterOwnerHash("owner-hash-1")
						.scopes(EsiAuthHelper.SCOPES).build()));

		// Execute callback.
		var code = "auth-code-1";
		var callbackResponse =
				TestHttpUtils.callProxy(client, proxy, "GET", "/login/callback?code=" + code + "&state=" + state);
		//		assertEquals(307, callbackResponse.code()); @todo
		//		var callbackRedirect = HttpUrl.parse(callbackResponse.header("location"));
		//		assertEquals("/", callbackRedirect.encodedPath());

		// Token request.
		var tokenRequest = TestHttpUtils.takeRequest(mockEsi);
		var tokenRequestBody = tokenRequest.getBody().readUtf8();
		var codeVerifier = Stream.of(tokenRequestBody.split("&"))
				.filter(e -> e.startsWith("code_verifier="))
				.map(e -> e.substring("code_verifier=".length()))
				.findFirst()
				.orElseThrow();
		assertRequest(
				tokenRequest,
				"POST",
				"/v2/oauth/token",
				Map.of(HeaderNames.CONTENT_TYPE.lowerCase(), "application/x-www-form-urlencoded"));
		assertEquals(
				String.join(
						"&",
						List.of(
								"client_id=client-id-1",
								//								"client_secret=client-secret-1",
								"code=" + code,
								"redirect_uri=http%3A%2F%2Flocalhost%3A8182%2Flogin%2Fcallback",
								"scope=" + String.join("%20", EsiAuthHelper.SCOPES),
								"grant_type=authorization_code",
								"code_verifier=" + codeVerifier)),
				tokenRequestBody);

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
		assertEquals(EsiAuthHelper.SCOPES, authedCharacter.getScopes());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "EVE_OAUTH_SECRET_KEY", value = "client-secret-1")
	void shouldRequestAndTranslateOAuthTokensForProxyKeysUsingCodeFlow() {
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

		assertRequest(
				oauthRequest,
				"POST",
				"/v2/oauth/token",
				Map.of(HeaderNames.CONTENT_TYPE.lowerCase(), "application/x-www-form-urlencoded"),
				String.join(
						"&",
						List.of(
								"client_id=client-id-1",
								"client_secret=client-secret-1",
								"scope=" + String.join("%20", EsiAuthHelper.SCOPES),
								"refresh_token=refresh-token-1",
								"grant_type=refresh_token")));

		assertRequest(
				esiRequest, "GET", "/esi", Map.of(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer access-token-1"));

		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				"Test response",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));
	}
}
