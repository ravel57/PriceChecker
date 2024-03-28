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
import java.time.Duration
import java.time.temporal.ChronoUnit

@Service
class PriceCheckerService {

	private final Logger logger = LoggerFactory.getLogger(this.class)
	private final Gson gson = new Gson()


	Result getProduct(ParseInfo info) {
		String json = gson.toJson(info)
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/parse-by-info", json)
		logger.debug(response.toString())
		return responseHandler(response)
	}


	Result getProduct(String url) {
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/parse-by-url", url)
		logger.debug(response.toString())
		return responseHandler(response)
	}


	Result postProductName(ParseInfo info) {
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/set-name", gson.toJson(info))
		logger.debug(response.toString())
		return responseHandler(response)
	}


	Result postProductPrice(ParseInfo info) {
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/set-price", gson.toJson(info))
		logger.debug(response.toString())
		return responseHandler(response)
	}


	Result postNameClassAtr(ParseInfo info, String classAtr) {
		info.selectedNameClassAttribute = classAtr
		String json = gson.toJson(info)
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/set-name-atr", json)
		logger.debug(response.toString())
		return responseHandler(response)
	}


	Result postPriceClassAtr(ParseInfo info, String classAtr) {
		info.selectedPriceClassAttribute = classAtr
		String json = gson.toJson(info)
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/set-price-atr", json)
		logger.debug(response.toString())
		return responseHandler(response)
	}


	ArrayList<Result> getAllFollowedProducts() {
		logger.info("")
		return ["iPhone 11 Pro", "Samsung S24 Ultra", "Казантип 2009"]
	}


	private Result responseHandler(HttpResponse<String> response) {
		return switch (response.statusCode()) {
			case HttpStatus.SC_OK -> {
				ParseInfo parseInfo = gson.fromJson(response.body(), ParseInfo.class)
				new Result(true, parseInfo)
			}
			case HttpStatus.SC_NOT_FOUND -> {
				/*FIXME*/
				new Result(false, null)
			}
			case HttpStatus.SC_INTERNAL_SERVER_ERROR -> {
				logger.error("INTERNAL_SERVER_ERROR")
				throw new RuntimeException()
			}
		}
	}


	TelegramUser saveNewUser(TelegramUser telegramUser) {
		HttpResponse<String> response = sendRequest("http://localhost:8765/web-parser/save-new-user", "")
		/*telegramUser.id = */ gson.fromJson(response.body(), ParseInfo.class)
		return telegramUser
	}


	HttpResponse sendRequest(String url, String payload) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.timeout(Duration.of(3, ChronoUnit.MINUTES))
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build()
		HttpResponse response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
		return response
	}


	static class Result {
		Boolean isParserExist
		ParseInfo parseInfoResult

		Result(Boolean isParserExist, ParseInfo parseInfoResult) {
			this.isParserExist = isParserExist
			this.parseInfoResult = parseInfoResult
		}
	}

}
