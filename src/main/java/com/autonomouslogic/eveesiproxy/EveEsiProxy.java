package com.autonomouslogic.eveesiproxy;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.inject.DaggerMainComponent;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.WebServer;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URL;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Entry point for the stand-alone application.
 */
@Log4j2
public class EveEsiProxy {
	public static final String BASE_PATH = "/esiproxy";

	@Inject
	@Named("version")
	protected String version;

	@Inject
	protected WebServer server;

	@Inject
	protected EveEsiProxy() {}

	public static void main(String[] args) {
		log.debug("Starting proxy");
		var component = DaggerMainComponent.create();
		component.createMain().start();
		testProtocol(component.createOkHttpClient());
	}

	public void start() {
		log.info("EVE ESI Proxy version {}", version);
		server.start();
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

	@SneakyThrows
	private static void testProtocol(OkHttpClient client) {
		var url = new URL(Configs.ESI_BASE_URL.getRequired()) + "/latest/status";
		var request = new Request.Builder()
				.url(url)
				.header(HeaderNames.USER_AGENT.lowerCase(), "test")
				.build();
		var response = client.newCall(request).execute();
		if (response.code() != 200) {
			log.warn(
					"Received {} from ESI status {}: {}",
					response.code(),
					url,
					response.body().string());
		}
		log.info("Connected to the ESI over {}", response.protocol());
	}
}
