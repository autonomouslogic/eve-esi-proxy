package com.autonomouslogic.eveesiproxy.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EsiRouteClassifierTest {
	@ParameterizedTest
	@MethodSource("routeTests")
	void shouldClassifyRoutes(String url, EsiRouteType expectedType) {
		var actualType = EsiRouteClassifier.classifyRoute(url);
		assertEquals(expectedType, actualType);
	}

	public static Stream<Arguments> routeTests() {
		return Stream.of(marketHistoryRoute(), characterCorporationHistoryRoute(), otherRoutes())
				.flatMap(Function.identity())
				.flatMap(EsiRouteClassifierTest::routeVersions)
				.flatMap(EsiRouteClassifierTest::routeBasePaths);
	}

	private static Stream<Arguments> routeBasePaths(Arguments args) {
		var a = args.get();
		return Stream.of("", "/base", "/multi/base").map(prefix -> Arguments.of(prefix + a[0], a[1]));
	}

	private static Stream<Arguments> routeVersions(Arguments args) {
		var a = args.get();
		return Stream.of("/v1", "/latest").map(prefix -> Arguments.of(prefix + a[0], a[1]));
	}

	public static Stream<Arguments> marketHistoryRoute() {
		return Stream.of(Arguments.of("/markets/10000002/history/", EsiRouteType.MARKET_HISTORY));
	}

	public static Stream<Arguments> characterCorporationHistoryRoute() {
		return Stream.of(
				Arguments.of("/characters/1452072530/corporationhistory/", EsiRouteType.CHARACTER_CORPORATION_HISTORY));
	}

	public static Stream<Arguments> otherRoutes() {
		return Stream.of(
						"/characters/1452072530/blueprints/",
						"/characters/1452072530/orders/",
						"/sovereignty/campaigns/")
				.map(url -> Arguments.of(url, EsiRouteType.OTHER));
	}
}
