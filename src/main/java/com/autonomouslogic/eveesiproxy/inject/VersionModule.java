package com.autonomouslogic.eveesiproxy.inject;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

@Module
public class VersionModule {
	@Provides
	@Named("version")
	@Singleton
	@SneakyThrows
	public String version() {
		try (var in = VersionModule.class.getClassLoader().getResourceAsStream("version")) {
			if (in == null) {
				throw new FileNotFoundException("version");
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8).trim();
		}
	}
}
