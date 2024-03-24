package ru.ravel.telegramservice.service

import com.google.gson.Gson
import jakarta.ws.rs.core.MediaType
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ravel.core.dto.ParseInfo
import ru.ravel.telegramservice.entity.TelegramUser

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class PriceCheckerService {

	private final static Logger logger = LoggerFactory.getLogger(this.class)

	Result getInfo(ParseInfo info) {
		String json = new Gson().toJson(info)
		HttpResponse<String> response = sendRequest(json, "http://localhost:8765/web-parser/parse-by-info")
		return handleResponse(response)
	}

	Result getInfo(String url) {
		HttpResponse<String> response = sendRequest(url, "http://localhost:8765/web-parser/parse-by-url")
		return handleResponse(response)
	}

	private Result handleResponse(HttpResponse<String> response) {
		return switch (response.statusCode()) {
			case HttpStatus.SC_OK -> {
				ParseInfo parseInfo = new Gson().fromJson(response.body(), ParseInfo.class)
				new Result(true, parseInfo)
			}
			case HttpStatus.SC_NOT_FOUND -> {
				Map map = new Gson().fromJson(response.body(), Map.class)
				new Result(false, map)
			}
			case HttpStatus.SC_INTERNAL_SERVER_ERROR -> {
				logger.error("INTERNAL_SERVER_ERROR")
				throw new RuntimeException()
			}
		}
	}

	TelegramUser saveNewUser(TelegramUser telegramUser) {
		HttpResponse<String> response = sendRequest("", "http://localhost:8765/web-parser/parse-by-url")
		/*telegramUser.id = */ new Gson().fromJson(response.body(), ParseInfo.class)
		return telegramUser
	}

	class Result {
		boolean isHavingParser
		ParseInfo parseInfoResult
		Map mapResult

		Result(boolean isHavingParser, ParseInfo parseInfoResult) {
			this.isHavingParser = isHavingParser
			this.parseInfoResult = parseInfoResult
		}

		Result(boolean isHavingParser, Map mapResult) {
			this.isHavingParser = isHavingParser
			this.mapResult = mapResult
		}
	}

	static HttpResponse sendRequest(String payload, String url) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build()
		HttpResponse response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
		return response
	}

}
