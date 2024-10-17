package com.autonomouslogic.eveesiproxy.http;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HttpDate {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
					"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
			.withZone(ZoneId.of("GMT"));

	public static String format(ZonedDateTime t) {
		return formatter.format(t);
	}
}
