package ru.ravel.eurekaclient


import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.temporal.ChronoUnit
import java.util.stream.IntStream

@RestController
@RequestMapping("/client")
class TestController(
	@Value("\${eureka.instance.instance-id}")
	val test: String,
) {

	@GetMapping("/test")
	fun test(): String {
		return test
	}

}