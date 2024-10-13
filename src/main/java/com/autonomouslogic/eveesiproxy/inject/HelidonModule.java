package com.autonomouslogic.eveesiproxy.inject;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.handler.ErrorHandler;
import com.autonomouslogic.eveesiproxy.handler.IndexHandler;
import com.autonomouslogic.eveesiproxy.handler.ProxyHandler;
import dagger.Module;
import dagger.Provides;
import io.helidon.webserver.ConnectionConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Module
@Log4j2
public class HelidonModule {
	@Provides
	@Singleton
	public WebServer webServer(IndexHandler indexHandler, ProxyHandler proxyHandler, ErrorHandler errorHandler) {
		log.trace("Creating Helidon server");
		return WebServer.builder()
				.host(Configs.PROXY_HOST.getRequired())
				.port(Configs.PROXY_PORT.getRequired())
				.connectionConfig(connectionConfig())
				.routing(routing -> routing(routing, indexHandler, proxyHandler, errorHandler))
				.build();
	}

	private ConnectionConfig connectionConfig() {

		return ConnectionConfig.builder().keepAlive(true).tcpNoDelay(true).build();
	}

	private void routing(
			HttpRouting.Builder routing,
			IndexHandler indexHandler,
			ProxyHandler proxyHandler,
			ErrorHandler errorHandler) {
		routing.register(indexHandler).register(proxyHandler).error(Exception.class, errorHandler);
	}
}
