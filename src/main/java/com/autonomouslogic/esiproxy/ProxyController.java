package com.autonomouslogic.esiproxy;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Deprecated
@Controller
@Singleton
public class ProxyController {
	@Inject
	protected EsiRelay esiRelay;

	@GET
	@Path("/")
	public void get(HttpRequest<byte[]> request) {
		//		return esiRelay.request(request);
	}

	@GET
	@Path("{path:.*}")
	public void get(HttpRequest<byte[]> request, @PathParam("path") String path) {
		//		return esiRelay.request(request);
	}
}
