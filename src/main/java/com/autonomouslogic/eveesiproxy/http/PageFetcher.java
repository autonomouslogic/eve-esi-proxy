package com.autonomouslogic.eveesiproxy.http;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;

@Singleton
public class PageFetcher {
	@Inject
	protected OkHttpClient client;

	@Inject
	protected PageFetcher() {}
}
