package com.autonomouslogic.eveesiproxy.ui;

import com.autonomouslogic.commons.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Singleton
public class TemplateUtil {

	static {
	}

	@Inject
	protected TemplateEngine templateEngine;

	@Inject
	@SneakyThrows
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

	@SneakyThrows
	private static String loadString(String path) {
		try (var in = ResourceUtil.loadResource("/templates/" + path)) {
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		}
	}

	@SneakyThrows
	private static String loadBase64(String path) {
		try (var in = ResourceUtil.loadResource("/templates/" + path)) {
			return Base64.encodeBase64String(IOUtils.toByteArray(in));
		}
	}
}
