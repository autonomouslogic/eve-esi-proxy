package com.autonomouslogic.eveesiproxy;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.handler.IndexHandler;
import com.autonomouslogic.eveesiproxy.handler.ProxyHandler;
import com.autonomouslogic.eveesiproxy.http.EsiRelay;
import com.autonomouslogic.eveesiproxy.inject.DaggerMainComponent;
import io.helidon.webserver.ConnectionConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import jakarta.inject.Named;
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

	@Inject
	protected EsiRelay esiRelay;

	@Inject
	@Named("version")
	protected String version;

	private WebServer server;

	@Inject
	protected EveEsiProxy() {}

	public static void main(String[] args) {
		log.info("Starting EVE ESI Proxy");
		var relay = DaggerMainComponent.create().createMain().start();
		var protocol = relay.testProtocol();
		log.debug("Connected to the ESI over {}", protocol);
	}

	public EsiRelay start() {
		log.info("EVE ESI Proxy version {}", version);

		server = WebServer.builder()
				.host(Configs.PROXY_HOST.getRequired())
				.port(Configs.PROXY_PORT.getRequired())
				.connectionConfig(connectionConfig())
				.routing(this::routing)
				.build()
				.start();

		return esiRelay;
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
