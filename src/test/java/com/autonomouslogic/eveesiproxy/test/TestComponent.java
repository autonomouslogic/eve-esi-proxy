package com.autonomouslogic.eveesiproxy.test;

import com.autonomouslogic.eveesiproxy.handler.IndexHandlerTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyHandlerRateLimitTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyHandlerTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyHandlerUserAgentTest;
import com.autonomouslogic.eveesiproxy.inject.VersionModule;
import dagger.Component;
import jakarta.inject.Singleton;

@Component(modules = {VersionModule.class, TestOkHttpClientProvider.class})
@Singleton
public interface TestComponent {
	void inject(IndexHandlerTest test);

	void inject(ProxyHandlerTest test);

	void inject(ProxyHandlerUserAgentTest test);

	void inject(ProxyHandlerRateLimitTest test);
}
