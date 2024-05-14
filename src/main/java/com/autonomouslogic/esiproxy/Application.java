package com.autonomouslogic.esiproxy;

import io.helidon.config.Config;
import io.helidon.webserver.ConnectionConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import lombok.extern.log4j.Log4j2;

/**
 * Entry point for the stand-alone application.
 */
@Log4j2
public class Application {
	public static void main(String[] args) {
		log.info("Starting ESI Proxy");

		//		Micronaut.build(args).banner(false).start();

		var connectionConfig =
				ConnectionConfig.builder().keepAlive(true).tcpNoDelay(true).build();
		Config.builder().build();
		WebServer.builder()
				.port(8182)
				.connectionConfig(connectionConfig)
				.routing(Application::routing)
				.build()
				.start();
	}

	private static void routing(HttpRouting.Builder routing) {
		routing.get("/greet", (req, res) -> {
			log.info("Got request: {}", req.path());
			Thread.sleep(1000);
			res.send("Hello World!\n");
		});
	}
}
