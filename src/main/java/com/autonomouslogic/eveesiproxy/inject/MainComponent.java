package com.autonomouslogic.eveesiproxy.inject;

import com.autonomouslogic.eveesiproxy.EveEsiProxy;
import dagger.Component;
import jakarta.inject.Singleton;

@Component(modules = {VersionModule.class})
@Singleton
public interface MainComponent {
	void inject(EveEsiProxy main);

	EveEsiProxy createMain();

	static MainComponent create() {
		return DaggerMainComponent.create();
	}
}
