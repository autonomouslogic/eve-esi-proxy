package com.autonomouslogic.eveesiproxy.util;

import io.helidon.common.parameters.Parameters;
import java.util.Optional;

public class HelidonUtil {
	public static Optional<String> getParameter(String name, Parameters params) {
		if (params == null) {
			return Optional.empty();
		}
		if (params.contains(name)) {
			return Optional.ofNullable(params.get(name));
		}
		return Optional.empty();
	}
}
