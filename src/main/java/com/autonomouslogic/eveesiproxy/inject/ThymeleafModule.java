package com.autonomouslogic.eveesiproxy.inject;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.time.Duration;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Module
public class ThymeleafModule {
	@Singleton
	@Provides
	public TemplateEngine templateEngine() {
		var engine = new TemplateEngine();
		engine.addDialect(new LayoutDialect());
		engine.addTemplateResolver(cssResolver());
		engine.addTemplateResolver(htmlResolver());
		return engine;
	}

	private static ITemplateResolver cssResolver() {
		var resolver = new ClassLoaderTemplateResolver();
		resolver.setCheckExistence(true);
		resolver.setPrefix("/templates/");
		resolver.setSuffix(".css");
		resolver.setTemplateMode(TemplateMode.CSS);
		resolver.setCacheable(true);
		resolver.setCacheTTLMs(Duration.ofDays(1000).toMillis());
		return resolver;
	}

	private static ITemplateResolver htmlResolver() {
		var resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("/templates/");
		resolver.setSuffix(".html");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(true);
		resolver.setCacheTTLMs(Duration.ofDays(1000).toMillis());
		return resolver;
	}
}
