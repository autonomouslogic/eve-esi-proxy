package com.autonomouslogic.eveesiproxy.http;

import com.autonomouslogic.commons.ResourceUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class EsiUrlGroupResolver {
	private static final String GROUPS_FILE = "/esi-url-groups.json";
	private final List<CompiledUrlGroup> compiledGroups;

	@Inject
	protected EsiUrlGroupResolver(ObjectMapper objectMapper) {
		compiledGroups = loadAndCompileGroups(objectMapper);
		log.debug("Loaded {} URL group patterns", compiledGroups.size());
	}

	public Optional<String> resolveGroup(String urlPath) {
		String normalizedPath =
				urlPath.endsWith("/") && urlPath.length() > 1 ? urlPath.substring(0, urlPath.length() - 1) : urlPath;

		normalizedPath = stripVersionPrefix(normalizedPath);

		for (CompiledUrlGroup compiled : compiledGroups) {
			if (compiled.pattern.matcher(normalizedPath).matches()) {
				return Optional.ofNullable(compiled.group);
			}
		}

		log.debug("No matching URL pattern found for: {}", urlPath);
		return Optional.empty();
	}

	private String stripVersionPrefix(String path) {
		if (path.startsWith("/latest/")) {
			return path.substring("/latest".length());
		}
		if (path.startsWith("/v") && path.length() > 2) {
			int slashIndex = path.indexOf('/', 2);
			if (slashIndex > 0) {
				String versionPart = path.substring(2, slashIndex);
				if (versionPart.matches("\\d+")) {
					return path.substring(slashIndex);
				}
			}
		}
		return path;
	}

	private List<CompiledUrlGroup> loadAndCompileGroups(ObjectMapper objectMapper) {
		try (var in = ResourceUtil.loadResource(GROUPS_FILE)) {
			return objectMapper.readValue(in, new TypeReference<List<EsiUrlGroup>>() {}).stream()
					.map(group -> new CompiledUrlGroup(compileUrlPattern(group.getUrl()), group.getGroup()))
					.toList();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load URL groups from " + GROUPS_FILE, e);
		}
	}

	private Pattern compileUrlPattern(String urlPattern) {
		String escaped = urlPattern
				.replace("\\", "\\\\")
				.replace(".", "\\.")
				.replace("*", "\\*")
				.replace("+", "\\+")
				.replace("?", "\\?")
				.replace("|", "\\|")
				.replace("(", "\\(")
				.replace(")", "\\)")
				.replace("[", "\\[")
				.replace("]", "\\]")
				.replace("^", "\\^")
				.replace("$", "\\$");

		String regex = escaped.replaceAll("\\{[^}]+\\}", "[^/]+");

		return Pattern.compile("^" + regex + "$");
	}

	@Value
	private static class CompiledUrlGroup {
		Pattern pattern;
		String group;
	}
}
