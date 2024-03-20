package ru.ravel.telegramservice.service

import org.springframework.stereotype.Service

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
}
