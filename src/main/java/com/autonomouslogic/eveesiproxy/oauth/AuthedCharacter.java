package com.autonomouslogic.eveesiproxy.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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
