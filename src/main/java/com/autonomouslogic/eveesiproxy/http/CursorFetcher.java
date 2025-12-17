package com.autonomouslogic.eveesiproxy.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
@Log4j2
public class CursorFetcher {
	@Inject
	protected OkHttpClient client;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected CursorFetcher() {}

	public Response fetchCursorPages(Request esiRequest, Response esiResponse) {
		if (esiResponse.code() != 200) {
			return esiResponse;
		}

		if (hasCursorParameter(esiRequest)) {
			return esiResponse;
		}

		byte[] bodyBytes;
		ObjectNode firstPage;
		try {
			var body = esiResponse.body();
			if (body == null) {
				return esiResponse;
			}
			bodyBytes = body.bytes();
			var json = objectMapper.readTree(bodyBytes);
			if (!json.isObject()) {
				return recreateResponse(esiResponse, bodyBytes);
			}
			firstPage = (ObjectNode) json;
		} catch (IOException e) {
			return esiResponse;
		}

		String beforeCursor = getBeforeCursor(firstPage);
		if (beforeCursor == null) {
			return recreateResponse(esiResponse, bodyBytes);
		}

		log.trace("Request for {} has cursor-based pagination, following 'before' tokens", esiRequest.url());
		return fetchPages(esiRequest, esiResponse, firstPage, beforeCursor);
	}

	private boolean hasCursorParameter(Request request) {
		var url = request.url();
		return url.queryParameter("before") != null || url.queryParameter("after") != null;
	}

	private Response recreateResponse(Response originalResponse, byte[] bodyBytes) {
		return new Response.Builder()
				.request(originalResponse.request())
				.protocol(originalResponse.protocol())
				.code(originalResponse.code())
				.message(originalResponse.message())
				.headers(originalResponse.headers())
				.body(ResponseBody.create(bodyBytes, originalResponse.body().contentType()))
				.build();
	}

	@SneakyThrows
	private ObjectNode parseResponse(Response response) {
		var body = response.body();
		if (body == null) {
			return null;
		}
		var bytes = body.bytes();
		var json = objectMapper.readTree(bytes);
		if (!json.isObject()) {
			return null;
		}
		return (ObjectNode) json;
	}

	private String getBeforeCursor(ObjectNode page) {
		var cursor = page.get("cursor");
		if (cursor == null || !cursor.isObject()) {
			return null;
		}
		var before = cursor.get("before");
		if (before == null || !before.isTextual()) {
			return null;
		}
		return before.asText();
	}

	@SneakyThrows
	private Response fetchPages(Request firstRequest, Response firstResponse, ObjectNode firstPage, String beforeCursor) {
		List<ObjectNode> pages = new ArrayList<>();
		pages.add(firstPage);

		String nextCursor = beforeCursor;
		while (nextCursor != null) {
			var nextRequest = buildCursorRequest(firstRequest, nextCursor);
			try (var nextResponse = OkHttpExec.execute(client.newCall(nextRequest))) {
				if (nextResponse.code() != 200) {
					return nextResponse;
				}

				var nextPage = parseResponse(nextResponse);
				if (nextPage == null) {
					return nextResponse;
				}

				pages.add(nextPage);
				nextCursor = getBeforeCursor(nextPage);
			}
		}

		var mergedPage = mergePages(pages);
		preserveAfterCursor(firstPage, mergedPage);

		return new Response.Builder()
				.request(firstRequest)
				.protocol(firstResponse.protocol())
				.message("merged cursor pages")
				.code(200)
				.body(ResponseBody.create(
						objectMapper.writeValueAsBytes(mergedPage), MediaType.get("application/json")))
				.build();
	}

	private Request buildCursorRequest(Request originalRequest, String cursor) {
		HttpUrl newUrl = originalRequest
				.url()
				.newBuilder()
				.setQueryParameter("before", cursor)
				.build();

		return originalRequest.newBuilder().url(newUrl).build();
	}

	private ObjectNode mergePages(List<ObjectNode> pages) {
		if (pages.isEmpty()) {
			return objectMapper.createObjectNode();
		}

		ObjectNode result = objectMapper.createObjectNode();
		ObjectNode firstPage = pages.get(0);
		Iterator<Map.Entry<String, JsonNode>> fields = firstPage.fields();

		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> field = fields.next();
			String fieldName = field.getKey();
			JsonNode fieldValue = field.getValue();

			if (fieldName.equals("cursor")) {
				continue;
			}

			if (fieldValue.isArray()) {
				ArrayNode mergedArray = objectMapper.createArrayNode();
				for (ObjectNode page : pages) {
					JsonNode pageField = page.get(fieldName);
					if (pageField != null && pageField.isArray()) {
						mergedArray.addAll((ArrayNode) pageField);
					}
				}
				result.set(fieldName, mergedArray);
			} else {
				result.set(fieldName, fieldValue);
			}
		}

		return result;
	}

	private void preserveAfterCursor(ObjectNode firstPage, ObjectNode mergedPage) {
		var cursor = firstPage.get("cursor");
		if (cursor == null || !cursor.isObject()) {
			return;
		}

		var after = cursor.get("after");
		if (after != null) {
			var newCursor = objectMapper.createObjectNode();
			newCursor.set("after", after);
			mergedPage.set("cursor", newCursor);
		}
	}
}
