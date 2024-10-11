package com.autonomouslogic.eveesiproxy.handler;

import static com.autonomouslogic.eveesiproxy.http.ProxyHeaderNames.X_EVE_ESI_PROXY_VERSION;

import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class StandardHeaders {
	@Inject
	@Named("version")
	protected String version;

	@Inject
	protected StandardHeaders() {}

	public ServerResponse apply(ServerResponse res) {
		return res.header(X_EVE_ESI_PROXY_VERSION, version);
	}
}
