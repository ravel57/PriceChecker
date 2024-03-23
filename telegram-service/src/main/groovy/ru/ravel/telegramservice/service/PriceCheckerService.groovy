package ru.ravel.telegramservice.service

import org.springframework.stereotype.Service

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Service
class PriceCheckerService {

	Result getInfo(String url) {
		return new Result(false, url)
	}

	class Result {
		boolean isHavingParser
		String result

		Result(boolean isHavingParser, String result) {
			this.isHavingParser = isHavingParser
			this.result = result
		}
	}

	static HttpResponse sendRequest(String url) {
		Map<String, String> formData = new HashMap<>();
		formData.put("url", url)

		HttpClient client = HttpClient.newHttpClient()
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:8080/parse-by-parse-info"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
				.build()
		HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString())
		return response
	}

	private static String getFormDataAsString(Map<String, String> formData) {
		StringBuilder formBodyBuilder = new StringBuilder();
		for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
			if (formBodyBuilder.length() > 0) {
				formBodyBuilder.append("&")
			}
			formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
			formBodyBuilder.append("=")
			formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
		}
		return formBodyBuilder.toString()
	}
}
