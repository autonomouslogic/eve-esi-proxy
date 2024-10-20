package com.autonomouslogic.eveesiproxy.inject;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.handler.ErrorHandler;
import com.autonomouslogic.eveesiproxy.handler.IndexService;
import com.autonomouslogic.eveesiproxy.handler.LoginService;
import com.autonomouslogic.eveesiproxy.handler.ProxyService;
import com.autonomouslogic.eveesiproxy.handler.StaticService;
import com.autonomouslogic.eveesiproxy.handler.UiService;
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
	public WebServer webServer(
			IndexService indexService,
			ProxyService proxyService,
			LoginService loginService,
			UiService uiService,
			StaticService staticService,
			ErrorHandler errorHandler) {
		log.trace("Creating Helidon server");
		return WebServer.builder()
				.host(Configs.PROXY_HOST.getRequired())
				.port(Configs.PROXY_PORT.getRequired())
				.connectionConfig(connectionConfig())
				.routing(routing -> routing(
						routing, indexService, proxyService, loginService, uiService, staticService, errorHandler))
				.build();
	}

	private ConnectionConfig connectionConfig() {

		return ConnectionConfig.builder().keepAlive(true).tcpNoDelay(true).build();
	}

	private void routing(
			HttpRouting.Builder routing,
			IndexService indexService,
			ProxyService proxyService,
			LoginService loginService,
			UiService uiService,
			StaticService staticService,
			ErrorHandler errorHandler) {
		routing.register(indexService)
				.register(UiService.BASE_PATH + "/login", loginService)
				.register(UiService.BASE_PATH, uiService)
				.register(staticService)
				.register(proxyService)
				.error(Exception.class, errorHandler);
	}
}
