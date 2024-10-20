package com.autonomouslogic.eveesiproxy.test;

import com.autonomouslogic.eveesiproxy.handler.IndexServiceTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServiceAuthTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServiceCacheTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServiceErrorLimitTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServicePagesTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServiceRateLimitTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServiceTest;
import com.autonomouslogic.eveesiproxy.handler.ProxyServiceUserAgentTest;
import com.autonomouslogic.eveesiproxy.inject.HelidonModule;
import com.autonomouslogic.eveesiproxy.inject.JacksonModule;
import com.autonomouslogic.eveesiproxy.inject.OkHttpModule;
import com.autonomouslogic.eveesiproxy.inject.VersionModule;
import dagger.Component;
import jakarta.inject.Singleton;

@Component(
		modules = {
			VersionModule.class,
			TestOkHttpClientProvider.class,
			OkHttpModule.class,
			JacksonModule.class,
			HelidonModule.class
		})
@Singleton
public interface TestComponent {
	void inject(IndexServiceTest test);

	void inject(ProxyServiceTest test);

	void inject(ProxyServiceCacheTest test);

	void inject(ProxyServiceUserAgentTest test);

	void inject(ProxyServiceRateLimitTest test);

	void inject(ProxyServicePagesTest test);

	void inject(ProxyServiceErrorLimitTest test);

	void inject(ProxyServiceAuthTest test);
}
