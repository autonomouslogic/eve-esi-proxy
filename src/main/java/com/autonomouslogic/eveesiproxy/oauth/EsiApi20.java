package com.autonomouslogic.eveesiproxy.oauth;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;
import lombok.Getter;

public class EsiApi20 extends DefaultApi20 {
	@Getter
	private final String authorizationBaseUrl = Configs.EVE_OAUTH_AUTHORIZATION_URL.getRequired();

	@Getter
	private final String accessTokenEndpoint = Configs.EVE_OAUTH_TOKEN_URL.getRequired();

	@Override
	public ClientAuthentication getClientAuthentication() {
		return RequestBodyAuthenticationScheme.instance();
	}
}
