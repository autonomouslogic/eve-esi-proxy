package com.autonomouslogic.esiproxy;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Controller
@Path("/")
@Singleton
public class BasicController {
	@GET
	public Single<HttpResponse<byte[]>> get(HttpRequest<byte[]> request) {
		return Single.just(HttpResponse.ok().body("Hello World!\n".getBytes()));
	}
}
