package com.autonomouslogic.eveesiproxy.ui;

import com.autonomouslogic.commons.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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
	private static final String logoImageBase64 = loadBase64("logo-small.webp");
	private static final String evePartnerImageBase64 = loadBase64("PartnerBadge2-small.webp");
	private static final String loginImageBase64 = loadBase64("eve-sso-login-white-large.png");

	@Inject
	protected TemplateEngine templateEngine;

	@Inject
	@Named("version")
	protected String proxyVersion;

	@Inject
	@SneakyThrows
	protected TemplateUtil() {}

	public String render(@NonNull String template, @NonNull Map<String, Object> model) {
		var context = prepareContext(model);
		return templateEngine.process(template, context);
	}

	private Context prepareContext(@NonNull Map<String, Object> model) {
		var context = new Context(Locale.ENGLISH);
		context.setVariable("proxyVersion", proxyVersion);
		context.setVariable("logoImageBase64", logoImageBase64);
		context.setVariable("evePartnerImageBase64", evePartnerImageBase64);
		context.setVariable("loginImageBase64", loginImageBase64);
		model.forEach(context::setVariable);
		return context;
	}

	@SneakyThrows
	private static String loadBase64(String path) {
		try (var in = ResourceUtil.loadResource("/templates/" + path)) {
			return Base64.encodeBase64String(IOUtils.toByteArray(in));
		}
	}
}
