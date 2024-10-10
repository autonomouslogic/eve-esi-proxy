package com.autonomouslogic.esiproxy.test;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autonomouslogic.esiproxy.EveEsiProxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TestHttpUtils {

	public static void enqueueResponse(MockWebServer mockWebServer, int status) {
		mockWebServer.enqueue(new MockResponse().setResponseCode(status));
	}

	public static void enqueueResponse(MockWebServer mockWebServer, int status, @NonNull String body) {
		mockWebServer.enqueue(new MockResponse().setResponseCode(status).setBody(body));
	}

	public static void enqueueResponse(MockWebServer mockWebServer, int status, @NonNull Map<String, String> headers) {
		var response = new MockResponse().setResponseCode(status);
		headers.forEach(response::addHeader);
		mockWebServer.enqueue(response);
	}

	public static void enqueueResponse(
			MockWebServer mockWebServer, int status, @NonNull String body, @NonNull Map<String, String> headers) {
		var response = new MockResponse().setResponseCode(status).setBody(body);
		headers.forEach(response::addHeader);
		mockWebServer.enqueue(response);
	}

	public static Request.Builder proxyRequest(EveEsiProxy proxy, String method, String path, String body) {
		var requestBody = body == null ? null : RequestBody.create(body.getBytes(StandardCharsets.UTF_8));
		return new Request.Builder().method(method, requestBody).url("http://localhost:" + proxy.port() + path);
	}

	public static Request.Builder proxyRequest(EveEsiProxy proxy, String method, String path) {
		return proxyRequest(proxy, method, path, (String) null);
	}

	public static Request.Builder proxyRequest(
			EveEsiProxy proxy, String method, String path, Map<String, String> headers) {
		var req = proxyRequest(proxy, method, path);
		headers.forEach(req::header);
		return req;
	}

	@SneakyThrows
	public static Response callProxy(OkHttpClient client, Request request) {
		return client.newCall(request).execute();
	}

	public static Response callProxy(
			OkHttpClient client, EveEsiProxy proxy, String method, String path, Map<String, String> headers) {
		return callProxy(client, proxyRequest(proxy, method, path, headers).build());
	}

	public static Response callProxy(OkHttpClient client, EveEsiProxy proxy, String method, String path) {
		return callProxy(client, proxyRequest(proxy, method, path).build());
	}

	public static Response callProxy(OkHttpClient client, EveEsiProxy proxy, String method, String path, String body) {
		return callProxy(client, proxyRequest(proxy, method, path, body).build());
	}

	@SneakyThrows
	public static RecordedRequest takeRequest(MockWebServer mockWebServer) {
		return mockWebServer.takeRequest(0, TimeUnit.SECONDS);
	}

	public static void assertNoMoreRequests(MockWebServer mockWebServer) {
		assertNull(takeRequest(mockWebServer), "unexpected request");
	}

	public static void assertResponse(Response proxyResponse, int status) {
		assertEquals(status, proxyResponse.code());
		assertEquals(0, proxyResponse.body().contentLength());
	}

	@SneakyThrows
	public static void assertResponse(Response proxyResponse, int status, String body, Map<String, String> headers) {
		assertEquals(status, proxyResponse.code());
		assertEquals(body == null ? "" : body, proxyResponse.body().string());
		var contentLength = body == null ? 0 : body.length();
		assertEquals(contentLength, proxyResponse.body().contentLength());
		assertEquals(contentLength, Integer.parseInt(proxyResponse.header("Content-Length")));
		if (headers != null) {
			headers.forEach((name, value) -> assertEquals(value, proxyResponse.header(name), name));
		}
	}

	public static void assertResponse(Response proxyResponse, int status, String body) {
		assertResponse(proxyResponse, status, body, null);
	}

	public static void assertResponse(Response proxyResponse, int status, Map<String, String> headers) {
		assertResponse(proxyResponse, status, null, headers);
	}

	public static void assertRequest(RecordedRequest esiRequest, String get, String path, Map<String, String> headers) {
		assertNotNull(esiRequest);
		assertEquals("localhost:" + MOCK_ESI_PORT, esiRequest.getHeader("Host"));
		assertEquals("Host", esiRequest.getHeaders().name(0));
		assertEquals(get, esiRequest.getMethod());
		assertEquals(path, esiRequest.getPath());
		headers.forEach((name, value) -> assertEquals(value, esiRequest.getHeader(name), name));
		assertEquals(0, esiRequest.getBody().size());
	}
}
