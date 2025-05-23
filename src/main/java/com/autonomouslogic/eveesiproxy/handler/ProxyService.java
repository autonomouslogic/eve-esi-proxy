package com.autonomouslogic.eveesiproxy.handler;

import com.autonomouslogic.eveesiproxy.http.EsiRelay;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

/**
 * Handles the actual relay of request to the ESI server.
 */
@Singleton
@Log4j2
public class ProxyService implements HttpService, Handler {
	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	protected EsiRelay esiRelay;

	@Inject
	protected ProxyService() {}

	@Override
	public void routing(HttpRules httpRules) {
		log.trace("Configuring routing for {}", this.getClass().getSimpleName());
		httpRules.options(this);
		httpRules.head(this);
		httpRules.get(this);
		httpRules.put(this);
		httpRules.post(this);
		httpRules.delete(this);
		httpRules.any(StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) {
		esiRelay.relayRequest(req, standardHeaders.apply(res));
	}
}
