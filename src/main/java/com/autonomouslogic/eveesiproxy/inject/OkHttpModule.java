package com.autonomouslogic.eveesiproxy.inject;

import com.autonomouslogic.eveesiproxy.configs.Configs;
import com.autonomouslogic.eveesiproxy.http.CacheStatusInterceptor;
import com.autonomouslogic.eveesiproxy.http.ErrorLimitInterceptor;
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
			UserAgentInterceptor userAgentInterceptor,
			ErrorLimitInterceptor errorLimitInterceptor) {
		log.trace("Creating HTTP client");
		return new OkHttpClient.Builder()
				.followRedirects(false)
				.followSslRedirects(false)
				.connectTimeout(Duration.ofSeconds(5))
				.readTimeout(Duration.ofSeconds(20))
				.writeTimeout(Duration.ofSeconds(5))
				.cache(cache)
				.addInterceptor(cacheStatusInterceptor)
				.addInterceptor(userAgentInterceptor)
				.addInterceptor(errorLimitInterceptor)
				.addInterceptor(loggingInterceptor)
				.addNetworkInterceptor(rateLimitInterceptor)
				.build();
	}

	@Provides
	@Singleton
	@SneakyThrows
	public Cache cache() {
		log.trace("Creating HTTP cache");
		final File cacheDir;
		var httpCacheDir = Configs.HTTP_CACHE_DIR.get();
		log.trace("httpCacheDir: {}", httpCacheDir);
		var httpCacheMaxSize = Configs.HTTP_CACHE_MAX_SIZE.getRequired();
		log.trace("httpCacheMaxSize: {}", httpCacheMaxSize);
		if (httpCacheDir.isPresent()) {
			cacheDir = new File(httpCacheDir.get());
			if (!cacheDir.exists()) {
				log.debug("Creating HTTP cache directory {}", cacheDir);
				if (!cacheDir.mkdirs()) {
					throw new IOException("Failed creating HTTP cache directory " + cacheDir);
				}
				lockDir(cacheDir);
				log.trace("HTTP Cache directory created");
			}
		} else {
			log.debug("Creating temporary HTTP cache directory");
			cacheDir = Files.createTempDirectory("eve-esi-proxy-http-cache").toFile();
			lockDir(cacheDir);
		}
		log.info("Using HTTP cache directory {}", cacheDir);
		return new Cache(cacheDir, httpCacheMaxSize);
	}

	private static void lockDir(File cacheDir) throws IOException {
		if (!cacheDir.setReadable(true, true)) {
			throw new IOException("Failed setting HTTP cache directory readable " + cacheDir);
		}
		if (!cacheDir.setWritable(true, true)) {
			throw new IOException("Failed setting HTTP cache directory writable " + cacheDir);
		}
		if (!cacheDir.setExecutable(true, true)) {
			throw new IOException("Failed setting HTTP cache directory executable " + cacheDir);
		}
	}
}
