package com.autonomouslogic.eveesiproxy.inject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 */
@Module
public class JacksonModule {
	@Provides
	@Singleton
	public ObjectMapper objectMapper() {
		return JsonMapper.builder()
				.changeDefaultVisibility(vc -> vc.with(JsonAutoDetect.Visibility.NONE))
				.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
				.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
				.enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
				.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
				.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
				.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)
				.disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
				.enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
				.build();
	}
}
