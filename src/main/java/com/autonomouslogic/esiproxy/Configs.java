package com.autonomouslogic.esiproxy;

import com.autonomouslogic.commons.config.Config;

public class Configs {
	public static final Config<Integer> PORT = Config.<Integer>builder()
			.name("PORT")
			.type(Integer.class)
			.defaultValue(8182)
			.build();
}
