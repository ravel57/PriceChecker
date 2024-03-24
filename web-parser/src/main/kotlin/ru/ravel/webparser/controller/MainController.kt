package ru.ravel.webparser.controller

import com.sun.jdi.InternalException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.ravel.core.dto.ParseInfo
import ru.ravel.webparser.exception.ParserDoesntExistException
import ru.ravel.webparser.services.WebParserService

@RestController("")
class MainController(
	var parserService: WebParserService,
) {

	val logger = LoggerFactory.getLogger(this::class.java)

	@PostMapping("/parse-by-url")
	fun getProductByUrl(@RequestBody url: String): ResponseEntity<Any> {
		return try {
			ResponseEntity.ok().body(parserService.getProduct(url))
		} catch (e: ParserDoesntExistException) {
			logger.error(e.message, e)
			ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
		} catch (e: Exception) {
			logger.error(e.message, e)
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	@PostMapping("/parse-by-info")
	fun getProductByInfo(@RequestBody parseInfo: ParseInfo): ResponseEntity<Any> {
		return try {
			ResponseEntity.ok().body(parserService.getProduct(parseInfo))
		} catch (e: ParserDoesntExistException) {
			logger.error(e.message, e)
			ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.payload)
		} catch (e: Exception) {
			logger.error(e.message, e)
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}


	@PostMapping("/set-name-atr")
	fun setNameAtr(nameAtr: String, userID: Long) {
		ResponseEntity.ok().body(parserService.setNameAtr(nameAtr, userID))
	}


	@PostMapping("/set-price-atr")
	fun setPriceAtr(priceAtr: String, userID: Long) {
		ResponseEntity.ok().body(parserService.setPriceAtr(priceAtr, userID))
	}

}