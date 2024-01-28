package com.autonomouslogic.esiproxy;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Controller
@Singleton
public class ProxyController {
	@Inject
	protected EsiRelay esiRelay;

	@GET
	@Path("/")
	public Single<HttpResponse<byte[]>> get(HttpRequest<byte[]> request) {
		return esiRelay.request(request);
	}

	@GET
	@Path("{path:.*}")
	public Single<HttpResponse<byte[]>> get(HttpRequest<byte[]> request, @PathParam("path") String path) {
		return esiRelay.request(request);
	}
}
