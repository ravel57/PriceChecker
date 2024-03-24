package ru.ravel.telegramservice.service

import com.google.gson.Gson
import jakarta.ws.rs.core.MediaType
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ravel.core.dto.ParseInfo

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Service
class PriceCheckerService {

	private final static Logger logger = LoggerFactory.getLogger(this.class)

	Result getInfo(ParseInfo info) {
		HttpResponse<String> response = sendRequest(info)
		return handleResponse(response)
	}

	Result getInfo(String url) {
		HttpResponse<String> response = sendRequest(url)
		return handleResponse(response)
	}

	private Result handleResponse(HttpResponse<String> response) {
		ParseInfo parseInfo = new Gson().fromJson(response.body(), ParseInfo.class)
		return switch (response.statusCode()) {
			case HttpStatus.SC_OK -> {
				new Result(true, parseInfo)
			}
			case HttpStatus.SC_NOT_FOUND -> {
				new Result(false, parseInfo)
			}
			default -> {
				throw new NullPointerException()
			}
		}
	}

	class Result {
		boolean isHavingParser
		ParseInfo result

		Result(boolean isHavingParser, ParseInfo result) {
			this.isHavingParser = isHavingParser
			this.result = result
		}
	}

	static HttpResponse sendRequest(String payload) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:8765/web-parser/parse"))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build()
		HttpResponse response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
		response.body()
		return response
	}

	static HttpResponse sendRequest(ParseInfo info) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:8765/web-parser/parse-by-parse-info"))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(info)))
				.build()
		HttpResponse response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
		response.body()
		return response
	}

	private static String getFormDataAsString(Map<String, String> formData) {
		StringBuilder formBodyBuilder = new StringBuilder();
		for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
			if (formBodyBuilder.length() > 0) {
				formBodyBuilder.append("&")
			}
			formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8))
			formBodyBuilder.append("=")
			formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8))
		}
		return formBodyBuilder.toString()
	}
}
