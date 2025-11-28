package com.autonomouslogic.eveesiproxy.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class EsiUrlGroupTest {

	@ParameterizedTest
	@CsvSource({
		"300, 15m, 0.3333333333333333",
		"600, 15m, 0.6666666666666666",
		"150, 15m, 0.16666666666666666",
		"60, 1m, 1.0",
		"120, 2m, 1.0",
		"3600, 1h, 1.0",
		"7200, 2h, 1.0",
		"86400, 1d, 1.0",
		"100, 60s, 1.6666666666666667"
	})
	void shouldCalculateTokensPerSecond(int maxTokens, String windowSize, double expectedTokensPerSecond) {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(maxTokens)
				.windowSize(windowSize)
				.build();

		assertEquals(expectedTokensPerSecond, group.getTokensPerSecond(), 0.0000001);
	}

	@Test
	void shouldReturnNullWhenMaxTokensIsNull() {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(null)
				.windowSize("15m")
				.build();

		assertNull(group.getTokensPerSecond());
	}

	@Test
	void shouldReturnNullWhenWindowSizeIsNull() {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(300)
				.windowSize(null)
				.build();

		assertNull(group.getTokensPerSecond());
	}

	@Test
	void shouldThrowExceptionForInvalidWindowSizeFormat() {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(300)
				.windowSize("invalid")
				.build();

		assertThrows(IllegalArgumentException.class, group::getTokensPerSecond);
	}

	@Test
	void shouldThrowExceptionForEmptyWindowSize() {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(300)
				.windowSize("")
				.build();

		assertThrows(IllegalArgumentException.class, group::getTokensPerSecond);
	}

	@Test
	void shouldThrowExceptionForInvalidUnit() {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(300)
				.windowSize("15x")
				.build();

		assertThrows(IllegalArgumentException.class, group::getTokensPerSecond);
	}

	@Test
	void shouldThrowExceptionForNonNumericValue() {
		var group = EsiUrlGroup.builder()
				.url("/test")
				.group("test-group")
				.maxTokens(300)
				.windowSize("abcm")
				.build();

		assertThrows(IllegalArgumentException.class, group::getTokensPerSecond);
	}
}
