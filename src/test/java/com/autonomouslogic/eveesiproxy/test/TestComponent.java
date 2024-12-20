package com.autonomouslogic.eveesiproxy.test;

import com.autonomouslogic.eveesiproxy.handler.*;
import com.autonomouslogic.eveesiproxy.handler.LoginServiceTest;
import com.autonomouslogic.eveesiproxy.inject.HelidonModule;
import com.autonomouslogic.eveesiproxy.inject.JacksonModule;
import com.autonomouslogic.eveesiproxy.inject.OkHttpModule;
import com.autonomouslogic.eveesiproxy.inject.ThymeleafModule;
import com.autonomouslogic.eveesiproxy.inject.VersionModule;
import dagger.Component;
import jakarta.inject.Singleton;

@Component(
		modules = {
			VersionModule.class,
			TestOkHttpClientProvider.class,
			OkHttpModule.class,
			JacksonModule.class,
			HelidonModule.class,
			ThymeleafModule.class
		})
@Singleton
public interface TestComponent {
	void inject(IndexServiceTest test);

	void inject(LoginServiceTest test);

	void inject(ProxyServiceCacheTest test);

	void inject(ProxyServiceErrorLimitTest test);

	void inject(ProxyServicePagesTest test);

	void inject(ProxyServiceRateLimitTest test);

	void inject(ProxyServiceRetryTest test);

	void inject(ProxyServiceProxyTest test);

	void inject(ProxyServiceUserAgentTest test);
}
