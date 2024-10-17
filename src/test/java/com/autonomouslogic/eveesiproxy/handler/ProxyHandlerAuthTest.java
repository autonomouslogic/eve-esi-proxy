package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.test.TestConstants.MOCK_ESI_PORT;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames;
import com.autonomouslogic.eveesiproxy.http.ProxyHeaderValues;
import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import com.autonomouslogic.eveesiproxy.oauth.AuthedCharacter;
import com.autonomouslogic.eveesiproxy.oauth.EsiAuthHelper;
import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import com.autonomouslogic.eveesiproxy.test.TestHttpUtils;
import io.helidon.http.HeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
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
@SetEnvironmentVariable(key = "EVE_OAUTH_SECRET_KEY", value = "client-secret-1")
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
	void shouldRequestAndTranslateOAuthTokensForProxyKeys() {
		// OAuth response.
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

		TestHttpUtils.assertRequest(
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

		TestHttpUtils.assertRequest(
				esiRequest, "GET", "/esi", Map.of(HeaderNames.AUTHORIZATION.lowerCase(), "Bearer access-token-1"));

		TestHttpUtils.assertResponse(
				proxyResponse,
				200,
				"Test response",
				Map.of(ProxyHeaderNames.X_EVE_ESI_PROXY_CACHE_STATUS, ProxyHeaderValues.CACHE_STATUS_MISS));
	}
}
