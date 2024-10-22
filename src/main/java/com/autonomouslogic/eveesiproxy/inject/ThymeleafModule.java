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
		return createResolver(TemplateMode.CSS, true, null);
	}

	private static ITemplateResolver htmlResolver() {
		return createResolver(TemplateMode.HTML, false, ".html");
	}

	private static ClassLoaderTemplateResolver createResolver(TemplateMode mode, boolean checkExistence, String suffix) {
		var resolver = new ClassLoaderTemplateResolver();
		resolver.setCheckExistence(checkExistence);
		resolver.setPrefix("/templates/");
		if (suffix != null) {
			resolver.setSuffix(suffix);
		}
		resolver.setTemplateMode(mode);
		resolver.setCacheable(true);
		resolver.setCacheTTLMs(Duration.ofDays(1000).toMillis());
		return resolver;
	}
}
