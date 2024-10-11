package com.autonomouslogic.eveesiproxy.http;

import java.util.regex.Pattern;
import lombok.SneakyThrows;

public class EsiRouteClassifier {
	private static final Pattern marketHistory = Pattern.compile(".+\\/markets\\/\\d+\\/history\\/");
	private static final Pattern characterCorporationHistory =
			Pattern.compile(".+\\/characters\\/\\d+\\/corporationhistory\\/");

	@SneakyThrows
	public static EsiRouteType classifyRoute(String url) {
		if (marketHistory.matcher(url).matches()) {
			return EsiRouteType.MARKET_HISTORY;
		} else if (characterCorporationHistory.matcher(url).matches()) {
			return EsiRouteType.CHARACTER_CORPORATION_HISTORY;
		} else {
			return EsiRouteType.OTHER;
		}
	}
}
