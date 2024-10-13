package com.autonomouslogic.eveesiproxy.inject;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.http.CacheStatusInterceptor;
import com.autonomouslogic.eveesiproxy.http.LoggingInterceptor;
import com.autonomouslogic.eveesiproxy.http.RateLimitInterceptor;
import com.autonomouslogic.eveesiproxy.http.UserAgentInterceptor;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

@Module
@Log4j2
public class OkHttpModule {
	@Provides
	@Singleton
	public OkHttpClient okHttpClient(
			Cache cache,
			CacheStatusInterceptor cacheStatusInterceptor,
			RateLimitInterceptor rateLimitInterceptor,
			LoggingInterceptor loggingInterceptor,
			UserAgentInterceptor userAgentInterceptor) {
		return new OkHttpClient.Builder()
				.followRedirects(false)
				.followSslRedirects(false)
				.connectTimeout(Duration.ofSeconds(5))
				.readTimeout(Duration.ofSeconds(20))
				.writeTimeout(Duration.ofSeconds(5))
				.cache(cache)
				.addInterceptor(cacheStatusInterceptor)
				.addInterceptor(userAgentInterceptor)
				.addInterceptor(loggingInterceptor)
				.addNetworkInterceptor(rateLimitInterceptor)
				.build();
	}

	@Provides
	@Singleton
	@SneakyThrows
	public Cache cache() {
		final File cacheDir;
		var httpCacheDir = Configs.HTTP_CACHE_DIR.get();
		var httpCacheMaxSize = Configs.HTTP_CACHE_MAX_SIZE.getRequired();
		if (httpCacheDir.isPresent()) {
			cacheDir = new File(httpCacheDir.get());
			if (!cacheDir.exists()) {
				log.debug("Creating HTTP cache directory {}", cacheDir);
				if (!cacheDir.mkdirs()) {
					throw new IOException("Failed creating HTTP cache directory " + cacheDir);
				}
				log.debug("HTTP Cache directory created");
			}
		} else {
			log.debug("Creating temporary HTTP cache directory");
			cacheDir = Files.createTempDirectory("eve-esi-proxy-http-cache").toFile();
		}
		if (!cacheDir.setReadable(true, true)) {
			throw new IOException("Failed setting HTTP cache directory readable " + cacheDir);
		}
		if (!cacheDir.setWritable(true, true)) {
			throw new IOException("Failed setting HTTP cache directory writable " + cacheDir);
		}
		if (!cacheDir.setExecutable(true, true)) {
			throw new IOException("Failed setting HTTP cache directory executable " + cacheDir);
		}
		log.info("Using HTTP cache directory {}", cacheDir);
		return new Cache(cacheDir, httpCacheMaxSize);
	}
}
