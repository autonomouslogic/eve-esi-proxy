package com.autonomouslogic.esiproxy.inject;

import com.autonomouslogic.esiproxy.EveEsiProxy;
import dagger.Component;
import jakarta.inject.Singleton;

@Component(modules = {})
@Singleton
public interface MainComponent {
	void inject(EveEsiProxy main);

	EveEsiProxy createMain();

	static MainComponent create() {
		return DaggerMainComponent.create();
	}
}
