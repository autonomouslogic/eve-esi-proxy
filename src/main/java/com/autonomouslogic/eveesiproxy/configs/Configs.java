package com.autonomouslogic.eveesiproxy.configs;

import com.autonomouslogic.commons.config.Config;
import java.time.Duration;

public class Configs {
	public static final Config<String> LOG_LEVEL = Config.<String>builder()
			.name("LOG_LEVEL")
			.type(String.class)
			.defaultValue("INFO")
			.build();

	public static final Config<Integer> PROXY_PORT = Config.<Integer>builder()
			.name("PROXY_PORT")
			.type(Integer.class)
			.defaultValue(8182)
			.build();

	public static final Config<String> PROXY_HOST = Config.<String>builder()
			.name("PROXY_HOST")
			.type(String.class)
			.defaultValue("0.0.0.0")
			.build();

	public static final Config<String> ESI_BASE_URL = Config.<String>builder()
			.name("ESI_BASE_URL")
			.type(String.class)
			.defaultValue("https://esi.evetech.net")
			.build();

	public static final Config<String> ESI_USER_AGENT =
			Config.<String>builder().name("ESI_USER_AGENT").type(String.class).build();

	public static final Config<String> HTTP_CACHE_DIR =
			Config.<String>builder().name("HTTP_CACHE_DIR").type(String.class).build();

	public static final Config<Long> HTTP_CACHE_MAX_SIZE = Config.<Long>builder()
			.name("HTTP_CACHE_MAX_SIZE")
			.defaultValue(1024L * 1024L * 1024L)
			.type(Long.class)
			.build();

	public static final Config<Double> ESI_RATE_LIMIT_PER_S = Config.<Double>builder()
			.name("ESI_RATE_LIMIT_PER_S")
			.defaultValue(100.0)
			.type(Double.class)
			.build();

	public static final Config<Double> ESI_MARKET_HISTORY_RATE_LIMIT_PER_S = Config.<Double>builder()
			.name("ESI_MARKET_HISTORY_RATE_LIMIT_PER_S")
			.defaultValue(300.0 / 60.0)
			.type(Double.class)
			.build();

	public static final Config<Double> ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S = Config.<Double>builder()
			.name("ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S")
			.defaultValue(300.0 / 60.0)
			.type(Double.class)
			.build();

	public static final Config<String> CONFIG_DIR =
			Config.<String>builder().name("CONFIG_DIR").type(String.class).build();

	/**
	 * Client ID for OAuth2 against EVE Online.
	 */
	public static final Config<String> EVE_OAUTH_CLIENT_ID = Config.<String>builder()
			.name("EVE_OAUTH_CLIENT_ID")
			.defaultValue("89f0127100b74c28a5247d75e5f31d5e")
			.type(String.class)
			.build();

	/**
	 * Secret key for OAuth2 against EVE Online.
	 */
	public static final Config<String> EVE_OAUTH_SECRET_KEY = Config.<String>builder()
			.name("EVE_OAUTH_SECRET_KEY")
			.type(String.class)
			.build();

	public static final Config<String> EVE_OAUTH_CALLBACK_URL = Config.<String>builder()
			.name("EVE_OAUTH_CALLBACK_URL")
			.defaultValue("http://localhost:8182/esiproxy/login/callback")
			.type(String.class)
			.build();

	/**
	 * Authorization URL for OAuth2 against EVE Online.
	 */
	public static final Config<String> EVE_OAUTH_AUTHORIZATION_URL = Config.<String>builder()
			.name("EVE_OAUTH_AUTHORIZATION_URL")
			.defaultValue("https://login.eveonline.com/v2/oauth/authorize")
			.type(String.class)
			.build();

	/**
	 * Token URL for OAuth2 against EVE Online.
	 */
	public static final Config<String> EVE_OAUTH_TOKEN_URL = Config.<String>builder()
			.name("EVE_OAUTH_TOKEN_URL")
			.defaultValue("https://login.eveonline.com/v2/oauth/token")
			.type(String.class)
			.build();

	public static final Config<Duration> HTTP_CONNECT_TIMEOUT = Config.<Duration>builder()
			.name("HTTP_CONNECT_TIMEOUT")
			.defaultValue(Duration.parse("PT5S"))
			.type(Duration.class)
			.build();

	public static final Config<Duration> HTTP_READ_TIMEOUT = Config.<Duration>builder()
			.name("HTTP_READ_TIMEOUT")
			.defaultValue(Duration.parse("PT60S"))
			.type(Duration.class)
			.build();

	public static final Config<Duration> HTTP_WRITE_TIMEOUT = Config.<Duration>builder()
			.name("HTTP_WRITE_TIMEOUT")
			.defaultValue(Duration.parse("PT60S"))
			.type(Duration.class)
			.build();

	public static final Config<Duration> HTTP_CALL_TIMEOUT = Config.<Duration>builder()
			.name("HTTP_CALL_TIMEOUT")
			.defaultValue(Duration.parse("PT60S"))
			.type(Duration.class)
			.build();

	public static final Config<Integer> HTTP_MAX_TRIES = Config.<Integer>builder()
			.name("HTTP_MAX_TRIES")
			.defaultValue(3)
			.type(Integer.class)
			.build();

	public static final Config<Duration> HTTP_RETRY_DELAY = Config.<Duration>builder()
			.name("HTTP_RETRY_DELAY")
			.defaultValue(Duration.parse("PT2S"))
			.type(Duration.class)
			.build();

	public static final Config<Integer> HTTP_MAX_CONCURRENT_PAGES = Config.<Integer>builder()
			.name("HTTP_MAX_CONCURRENT_PAGES")
			.defaultValue(8)
			.type(Integer.class)
			.build();
}
