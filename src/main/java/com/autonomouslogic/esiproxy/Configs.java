package com.autonomouslogic.esiproxy;

import com.autonomouslogic.commons.config.Config;

public class Configs {
	public static final Config<Integer> PORT = Config.<Integer>builder()
			.name("PORT")
			.type(Integer.class)
			.defaultValue(8182)
			.build();

	public static final Config<String> ESI_PROXY_VERSION = Config.<String>builder()
			.name("ESI_PROXY_VERSION")
			.type(String.class)
			.defaultValue("dev")
			.build();

	public static final Config<String> ESI_BASE_URL = Config.<String>builder()
			.name("ESI_BASE_URL")
			.type(String.class)
			.defaultValue("https://esi.evetech.net/")
			.build();
}
