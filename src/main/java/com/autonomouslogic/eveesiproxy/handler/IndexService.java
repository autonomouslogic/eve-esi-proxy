package com.autonomouslogic.eveesiproxy.handler;

import com.autonomouslogic.eveesiproxy.oauth.AuthManager;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class IndexService implements HttpService, Handler {
	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	protected AuthManager authManager;

	@Inject
	@Named("version")
	protected String version;

	@Inject
	protected IndexService() {}

	@Override
	public void routing(HttpRules httpRules) {
		log.trace("Configuring routing for {}", this.getClass().getSimpleName());
		httpRules.get("/", this);
		httpRules.any("/", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		standardHeaders
				.apply(res)
				.status(Status.TEMPORARY_REDIRECT_307)
				.header(HeaderNames.LOCATION.lowerCase(), UiService.BASE_PATH)
				.send();
	}
}
