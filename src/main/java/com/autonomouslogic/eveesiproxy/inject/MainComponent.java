package com.autonomouslogic.eveesiproxy.inject;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import dagger.Component;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;

@Component(modules = {VersionModule.class, OkHttpModule.class, JacksonModule.class})
@Singleton
public interface MainComponent {
	void inject(EveEsiProxy main);

	EveEsiProxy createMain();

	OkHttpClient createOkHttpClient();

	static MainComponent create() {
		return DaggerMainComponent.create();
	}
}
