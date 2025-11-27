package com.autonomouslogic.eveesiproxy.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.eveesiproxy.test.DaggerTestComponent;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class EsiUrlGroupResolverTest {
	@Inject
	EsiUrlGroupResolver resolver;

	@BeforeEach
	void setUp() {
		DaggerTestComponent.create().inject(this);
	}

	@ParameterizedTest
	@MethodSource("urlsWithGroups")
	void shouldResolveUrlsWithGroups(String url, String expectedGroup) {
		var group = resolver.resolveGroup(url);
		assertTrue(group.isPresent(), "Expected group for URL: " + url);
		assertEquals(expectedGroup, group.get());
	}

	@ParameterizedTest
	@MethodSource("urlsWithoutGroups")
	void shouldResolveUrlsWithoutGroups(String url) {
		var group = resolver.resolveGroup(url);
		assertTrue(group.isEmpty(), "Expected no group for URL: " + url);
	}

	@Test
	void shouldHandleUnknownUrls() {
		var group = resolver.resolveGroup("/unknown/endpoint/path");
		assertTrue(group.isEmpty());
	}

	@Test
	void shouldHandleTrailingSlash() {
		var withoutSlash = resolver.resolveGroup("/characters/12345/assets");
		var withSlash = resolver.resolveGroup("/characters/12345/assets/");
		assertEquals(withoutSlash, withSlash);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/latest", "/v1", "/v2", "/v3"})
	void shouldHandleVersionPrefixes(String prefix) {
		assertEquals(Optional.of("alliance-social"), resolver.resolveGroup(prefix + "/alliances/1234/contacts"));
	}

	public static Stream<Arguments> urlsWithGroups() {
		return Stream.of(
				// Alliance groups
				Arguments.of("/alliances/123/contacts", "alliance-social"),
				Arguments.of("/alliances/456/contacts/labels", "alliance-social"),

				// Character groups
				Arguments.of("/characters/12345/agents_research", "char-industry"),
				Arguments.of("/characters/12345/attributes", "char-detail"),
				Arguments.of("/characters/12345/blueprints", "char-industry"),
				Arguments.of("/characters/12345/calendar", "char-social"),
				Arguments.of("/characters/12345/calendar/678", "char-social"),
				Arguments.of("/characters/12345/contacts", "char-social"),
				Arguments.of("/characters/12345/contracts", "char-contract"),
				Arguments.of("/characters/12345/contracts/789/bids", "char-contract"),
				Arguments.of("/characters/12345/fittings", "fitting"),
				Arguments.of("/characters/12345/fleet", "fleet"),
				Arguments.of("/characters/12345/fw/stats", "factional-warfare"),
				Arguments.of("/characters/12345/industry/jobs", "char-industry"),
				Arguments.of("/characters/12345/killmails/recent", "char-killmail"),
				Arguments.of("/characters/12345/location", "char-location"),
				Arguments.of("/characters/12345/wallet", "char-wallet"),
				Arguments.of("/characters/12345/wallet/journal", "char-wallet"),

				// Corporation groups
				Arguments.of("/corporation/12345/mining/extractions", "corp-industry"),
				Arguments.of("/corporations/12345/blueprints", "corp-industry"),
				Arguments.of("/corporations/12345/contacts", "corp-social"),
				Arguments.of("/corporations/12345/contracts", "corp-contract"),
				Arguments.of("/corporations/12345/fw/stats", "factional-warfare"),
				Arguments.of("/corporations/12345/industry/jobs", "corp-industry"),
				Arguments.of("/corporations/12345/killmails/recent", "corp-killmail"),
				Arguments.of("/corporations/12345/members", "corp-member"),
				Arguments.of("/corporations/12345/wallets", "corp-wallet"),

				// Fleet groups
				Arguments.of("/fleets/12345", "fleet"),
				Arguments.of("/fleets/12345/members", "fleet"),
				Arguments.of("/fleets/12345/wings", "fleet"),

				// Factional warfare
				Arguments.of("/fw/leaderboards", "factional-warfare"),
				Arguments.of("/fw/stats", "factional-warfare"),

				// Other groups
				Arguments.of("/incursions", "incursion"),
				Arguments.of("/industry/facilities", "industry"),
				Arguments.of("/insurance/prices", "insurance"),
				Arguments.of("/killmails/12345/hash123", "killmail"),
				Arguments.of("/route/12345/67890", "routes"),
				Arguments.of("/sovereignty/campaigns", "sovereignty"),
				Arguments.of("/status", "status"),
				Arguments.of("/ui/autopilot/waypoint", "ui"));
	}

	public static Stream<Arguments> urlsWithoutGroups() {
		return Stream.of(
				Arguments.of("/alliances"),
				Arguments.of("/alliances/123"),
				Arguments.of("/alliances/123/corporations"),
				Arguments.of("/characters/affiliation"),
				Arguments.of("/characters/12345"),
				Arguments.of("/characters/12345/assets"),
				Arguments.of("/characters/12345/corporationhistory"),
				Arguments.of("/characters/12345/search"),
				Arguments.of("/corporations/npccorps"),
				Arguments.of("/corporations/12345"),
				Arguments.of("/corporations/12345/facilities"),
				Arguments.of("/dogma/attributes"),
				Arguments.of("/markets/prices"),
				Arguments.of("/universe/regions"));
	}
}
