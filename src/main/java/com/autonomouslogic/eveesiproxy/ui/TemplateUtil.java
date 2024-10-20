package com.autonomouslogic.eveesiproxy.ui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import lombok.NonNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Singleton
public class TemplateUtil {
	@Inject
	protected TemplateEngine templateEngine;

	@Inject
	protected TemplateUtil() {}

	public String render(@NonNull String template, @NonNull Map<String, Object> model) {
		var context = prepareContext(model);
		return templateEngine.process(template, context);
	}

	private Context prepareContext(@NonNull Map<String, Object> model) {
		var context = new Context(Locale.ENGLISH);
		model.forEach(context::setVariable);
		return context;
	}
}
