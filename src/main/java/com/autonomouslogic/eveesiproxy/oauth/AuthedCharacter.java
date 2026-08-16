package com.autonomouslogic.eveesiproxy.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class AuthedCharacter {
	@JsonProperty
	long characterId;

	@JsonProperty
	String characterName;

	@JsonProperty
	String characterOwnerHash;

	@JsonProperty
	String refreshToken;

	@JsonProperty
	String proxyKey;

	@JsonProperty
	List<String> scopes;
}
