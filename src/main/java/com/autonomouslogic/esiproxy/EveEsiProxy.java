package com.autonomouslogic.esiproxy;

import com.autonomouslogic.esiproxy.handler.IndexHandler;
import com.autonomouslogic.esiproxy.handler.ProxyHandler;
import com.autonomouslogic.esiproxy.inject.DaggerMainComponent;
import io.helidon.webserver.ConnectionConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Entry point for the stand-alone application.
 */
@Log4j2
public class EveEsiProxy {
	@Inject
	protected IndexHandler indexHandler;

	@Inject
	protected ProxyHandler proxyHandler;

	private WebServer server;

	@Inject
	protected EveEsiProxy() {}

	public static void main(String[] args) {
		DaggerMainComponent.create().createMain().start();
	}

	public void start() {
		log.info("Starting EVE ESI Proxy");

		server = WebServer.builder()
				.host(Configs.ESI_PROXY_HOST.getRequired())
				.port(Configs.ESI_PROXY_PORT.getRequired())
				.connectionConfig(connectionConfig())
				.routing(this::routing)
				.build()
				.start();
	}

	public void stop() {
		server.stop();
	}

	public int port() {
		return server.port();
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	private ConnectionConfig connectionConfig() {
		return ConnectionConfig.builder().keepAlive(true).tcpNoDelay(true).build();
	}

	private void routing(HttpRouting.Builder routing) {
		routing.get("/", indexHandler)
				.any("/", (req, res) -> res.status(405).send())
				.any(proxyHandler)
				.error(Exception.class, (req, res, ex) -> {
					log.warn("Error processing request", ex);
					res.status(500).send();
				});
	}
}
