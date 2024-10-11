package com.autonomouslogic.eveesiproxy.handler;

import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndexHandler implements Handler {
	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	@Named("version")
	protected String version;

	@Inject
	protected IndexHandler() {}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		standardHeaders.apply(res).send("EVE ESI Proxy " + version + "\n");
	}
}
