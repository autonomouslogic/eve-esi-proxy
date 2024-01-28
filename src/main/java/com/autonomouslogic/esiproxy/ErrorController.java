package com.autonomouslogic.esiproxy;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
public class ErrorController {
	@Error(status = HttpStatus.BAD_REQUEST, global = true)
	public HttpResponse badRequest(HttpRequest request) {
		return HttpResponse.badRequest();
	}

	@Error(exception = Exception.class, global = true)
	public HttpResponse exception(HttpRequest request, Exception e) {
		log.error("Server error", e);
		return HttpResponse.serverError();
	}
}
