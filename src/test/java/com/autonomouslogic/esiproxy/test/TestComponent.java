package com.autonomouslogic.esiproxy.test;

import com.autonomouslogic.esiproxy.handler.IndexHandlerTest;
import dagger.Component;
import jakarta.inject.Singleton;

@Component(modules = {TestOkHttpClientProvider.class})
@Singleton
public interface TestComponent {
	void inject(IndexHandlerTest test);
}
