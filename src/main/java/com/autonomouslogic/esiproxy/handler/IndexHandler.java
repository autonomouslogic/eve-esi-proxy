package com.autonomouslogic.esiproxy.handler;

import com.autonomouslogic.esiproxy.Configs;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndexHandler implements Handler {
	private final String version = Configs.EVE_ESI_PROXY_VERSION.getRequired();

	@Inject
	protected IndexHandler() {}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		res.send("EVE ESI Proxy " + version + "\n");
	}
}
