package ru.ravel.webparser.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.ravel.core.dto.ParseInfo
import ru.ravel.core.exception.ParserDoesntExistException
import ru.ravel.webparser.services.WebParserService

@RestController("")
class MainController(
	val parserService: WebParserService,
) {

	val logger: Logger = LoggerFactory.getLogger(this::class.java)

	@PostMapping("/parse-by-url")
	fun getProductByUrl(@RequestBody url: String): ResponseEntity<Any> {
		try {
			return ResponseEntity.ok().body(parserService.getProduct(url))
		} catch (e: ParserDoesntExistException) {
			logger.info(e.message)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
		} catch (e: Exception) {
			logger.error(e.message, e)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	@PostMapping("/set-price")
	fun postPrice(@RequestBody parseInfo: ParseInfo): ResponseEntity<Any> {
		try {
			return ResponseEntity.ok().body(parserService.postPrice(parseInfo))
		} catch (e: Exception) {
			logger.error(e.message, e)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	@PostMapping("/set-name")
	fun postName(@RequestBody parseInfo: ParseInfo): ResponseEntity<Any> {
		try {
			return ResponseEntity.ok().body(parserService.postName(parseInfo))
		} catch (e: Exception) {
			logger.error(e.message, e)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}


	@PostMapping("/set-name-atr")
	fun postNameAtr(parseInfo: ParseInfo): ResponseEntity<Any> {
		try {
			return ResponseEntity.ok().body(parserService.setNameAtr(parseInfo))
		} catch (e: Exception) {
			logger.error(e.message, e)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}


	@PostMapping("/set-price-atr")
	fun postPriceAtr(parseInfo: ParseInfo): ResponseEntity<Any> {
		try {
			return ResponseEntity.ok().body(parserService.setPriceAtr(parseInfo))
		} catch (e: Exception) {
			logger.error(e.message, e)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}


//	@PostMapping("get-name-class-atr")
//	fun getNameClassAtr(@RequestBody parseInfo: ParseInfo): ResponseEntity<Any> {
//		return try {
//			ResponseEntity.ok().body(parserService.getNameClassAtr(parseInfo))
//		} catch (e: ParserDoesntExistException) {
//			logger.error(e.message, e)
//			ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.payload)
//		} catch (e: Exception) {
//			logger.error(e.message, e)
//			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
//		}
//	}

}