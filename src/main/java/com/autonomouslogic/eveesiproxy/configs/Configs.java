package com.autonomouslogic.eveesiproxy.configs;

import com.autonomouslogic.commons.config.Config;

public class Configs {
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
			.defaultValue("https://esi.evetech.net/")
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
}
