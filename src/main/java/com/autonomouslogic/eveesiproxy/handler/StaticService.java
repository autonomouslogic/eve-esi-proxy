package com.autonomouslogic.eveesiproxy.handler;

import com.autonomouslogic.commons.ResourceUtil;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

@Singleton
@Log4j2
public class StaticService implements HttpService {
	private final byte[] favicon;

	@Inject
	protected StandardHeaders standardHeaders;

	@Inject
	@SneakyThrows
	protected StaticService() {
		try (var in = ResourceUtil.loadResource("/templates/favicon.ico")) {
			favicon = IOUtils.toByteArray(in);
		}
	}

	@Override
	public void routing(HttpRules httpRules) {
		log.trace("Configuring routing for {}", this.getClass().getSimpleName());
		httpRules.get("/favicon.ico", new FaviconHandler());
		httpRules.any("/favicon.ico", StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}

	class FaviconHandler implements Handler {
		@Override
		public void handle(ServerRequest req, ServerResponse res) throws Exception {
			standardHeaders
					.apply(res)
					.status(Status.OK_200)
					.header(HeaderNames.CONTENT_TYPE, "image/x-icon")
					.send(favicon);
		}
	}
}
