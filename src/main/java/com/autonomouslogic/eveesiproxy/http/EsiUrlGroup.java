package com.autonomouslogic.eveesiproxy.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents an ESI URL pattern and its corresponding rate limit group.
 */
@Value
@Builder
@Jacksonized
public class EsiUrlGroup {
	@JsonProperty
	String url;

	@JsonProperty
	String group;

	@JsonProperty
	Integer maxTokens;

	@JsonProperty
	String windowSize;

	/**
	 * Calculate tokens per second based on maxTokens and windowSize.
	 * @return tokens per second, or null if maxTokens or windowSize is null
	 * @throws IllegalArgumentException if windowSize cannot be parsed
	 */
	public Double getTokensPerSecond() {
		if (maxTokens == null || windowSize == null) {
			return null;
		}

		long windowSeconds = parseWindowSizeToSeconds(windowSize);

		return (double) maxTokens / windowSeconds;
	}

	/**
	 * Parse window size string (e.g., "15m", "1h", "30s") to seconds.
	 * @param windowSize the window size string
	 * @return the window size in seconds
	 * @throws IllegalArgumentException if the window size format is invalid
	 */
	private static long parseWindowSizeToSeconds(String windowSize) {
		if (windowSize == null || windowSize.isEmpty()) {
			throw new IllegalArgumentException("Window size cannot be null or empty");
		}

		String trimmed = windowSize.trim();
		if (trimmed.length() < 2) {
			throw new IllegalArgumentException("Window size must be at least 2 characters (e.g., '1m')");
		}

		try {
			String numberPart = trimmed.substring(0, trimmed.length() - 1);
			char unit = trimmed.charAt(trimmed.length() - 1);

			long value = Long.parseLong(numberPart);

			long seconds =
					switch (unit) {
						case 's' -> value;
						case 'm' -> value * 60;
						case 'h' -> value * 60 * 60;
						case 'd' -> value * 60 * 60 * 24;
						default ->
							throw new IllegalArgumentException(
									"Invalid time unit '" + unit + "'. Must be one of: s, m, h, d");
					};

			if (seconds <= 0) {
				throw new IllegalArgumentException("Window size must be greater than 0");
			}

			return seconds;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"Invalid window size format: '" + windowSize + "'. Expected format: <number><unit> (e.g., '15m')",
					e);
		}
	}
}
