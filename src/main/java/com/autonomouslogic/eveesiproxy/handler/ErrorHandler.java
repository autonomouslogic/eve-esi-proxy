package com.autonomouslogic.eveesiproxy.handler;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Singleton
@Log4j2
public class ErrorHandler implements io.helidon.webserver.http.ErrorHandler<Exception> {
	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	@Named("version")
	protected String version;

	@Inject
	protected ErrorHandler() {}

	@Override
	public void handle(ServerRequest serverRequest, ServerResponse serverResponse, Exception e) {
		log.warn("Error processing request", e);
		serverResponse.status(500).send(ExceptionUtils.getMessage(e));
	}
}
