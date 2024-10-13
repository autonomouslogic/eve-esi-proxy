package com.autonomouslogic.eveesiproxy.oauth;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class AuthManager {
	private final ObjectMapper objectMapper;
	private final ObjectWriter objectWriter;
	private final File configFile;
	private final JavaType javaType;
	private Map<Long, AuthedCharacter> authedCharacters;

	@Inject
	protected AuthManager(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
		configFile = new File(Configs.CONFIG_DIR.getRequired(), "auth.json");
		javaType =
				objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, Long.class, AuthedCharacter.class);
		loadConfig();
	}

	public void addAuthedCharacter(AuthedCharacter authedCharacter) {
		log.trace(
				"Saving authed character {} [{}]",
				authedCharacter.getCharacterName(),
				authedCharacter.getCharacterId());
		authedCharacters.put(authedCharacter.getCharacterId(), authedCharacter);
		saveConfig();
	}

	public AuthedCharacter getAuthedCharacter(long characterId) {
		return authedCharacters.get(characterId);
	}

	public List<AuthedCharacter> getAuthedCharacters() {
		return new ArrayList<>(authedCharacters.values());
	}

	@SneakyThrows
	private void loadConfig() {
		if (!configFile.exists()) {
			log.trace("No auth config file found at {}", configFile);
			authedCharacters = new LinkedHashMap<>();
		} else {
			log.trace("Loading auth config file from {}", configFile);
			authedCharacters = objectMapper.readValue(configFile, javaType);
		}
	}

	@SneakyThrows
	private void saveConfig() {
		log.trace("Saving auth config to {}", configFile);
		objectWriter.writeValue(configFile, authedCharacters);
	}
}
