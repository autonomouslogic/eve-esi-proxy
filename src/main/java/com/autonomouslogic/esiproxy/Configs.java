package com.autonomouslogic.esiproxy;

import com.autonomouslogic.commons.config.Config;

public class Configs {
	public static final Config<Integer> EVE_ESI_PROXY_PORT = Config.<Integer>builder()
			.name("EVE_ESI_PROXY_PORT")
			.type(Integer.class)
			.defaultValue(8182)
			.build();

	public static final Config<String> EVE_ESI_PROXY_HOST = Config.<String>builder()
			.name("EVE_ESI_PROXY_HOST")
			.type(String.class)
			.defaultValue("0.0.0.0")
			.build();

	public static final Config<String> EVE_ESI_PROXY_ESI_BASE_URL = Config.<String>builder()
			.name("EVE_ESI_PROXY_ESI_BASE_URL")
			.type(String.class)
			.defaultValue("https://esi.evetech.net/")
			.build();

	public static final Config<String> EVE_ESI_PROXY_HTTP_CACHE_DIR = Config.<String>builder()
			.name("EVE_ESI_PROXY_HTTP_CACHE_DIR")
			.type(String.class)
			.build();

	public static final Config<Long> EVE_ESI_PROXY_HTTP_CACHE_MAX_SIZE = Config.<Long>builder()
			.name("EVE_ESI_PROXY_HTTP_CACHE_MAX_SIZE")
			.defaultValue(1024L * 1024L * 1024L)
			.type(Long.class)
			.build();
}
