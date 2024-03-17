package ru.ravel.webparser.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class SchedulerService(
	@Autowired
	val webParser: WebParser
) {

	@Scheduled(cron = "0 0 */12 * * *")
	fun updatePrises() {
		val list = mutableListOf<Future<WebParser.ParsedProduct>>()
		val pool: ExecutorService = Executors.newFixedThreadPool(10)

//		list.add(pool.submit(myCallable1))
//		list.add(pool.submit(myCallable2))
//		list.add(pool.submit(myCallable3))

		for (future: Future<WebParser.ParsedProduct> in list) {
			println(future.get())
			println()
		}

	}


	class MyCallable(
		@Autowired
		private val webParser: WebParser,

		private val toCheck: ToCheck,
	) : Callable<WebParser.ParsedProduct> {

		override fun call(): WebParser.ParsedProduct {
			return webParser.getPrice(
				url = toCheck.url,
				searchName = toCheck.searchName,
				price = toCheck.price
			)
		}

	}


	data class ToCheck(
		val url: String,
		val searchName: String,
		val price: String,
	)
}