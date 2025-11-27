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
}
