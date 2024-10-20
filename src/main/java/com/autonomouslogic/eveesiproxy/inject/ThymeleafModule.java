package com.autonomouslogic.eveesiproxy.inject;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.time.Duration;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Module
public class ThymeleafModule {
	@Singleton
	@Provides
	public TemplateEngine templateEngine() {
		var resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("/templates/");
		resolver.setSuffix(".html");
		resolver.setCacheable(true);
		resolver.setCacheTTLMs(Duration.ofDays(1000).toMillis());
		var engine = new TemplateEngine();
		engine.addDialect(new LayoutDialect());
		engine.addTemplateResolver(resolver);
		return engine;
	}
}
