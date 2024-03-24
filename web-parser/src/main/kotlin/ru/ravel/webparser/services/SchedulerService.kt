package ru.ravel.webparser.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class SchedulerService {
	@Autowired
	private lateinit var webParser: WebParserService

	@Scheduled(cron = "* * */4 * * *")
	fun updatePrises() {
		val pool: ExecutorService = Executors.newFixedThreadPool(10)

//		val list = mutableListOf(
//			pool.submit(
//				WebParserCallable(
//					webParser,
//					"https://store77.net/telefony_samsung/telefon_samsung_galaxy_s24_8_128gb_zheltyy/",
//					"Телефон Samsung Galaxy S24 8/128Gb (Желтый)",
//					"66 600 Р",
//				)
//			),
//		)
//		list.add(pool.submit(myCallable2))
//		list.add(pool.submit(myCallable3))

//		for (future: Future<ParsedProduct> in list) {
//			future.get()
//		}

	}


//	class WebParserCallable(
//		private val webParser: WebParserService,
//		private val url: String,
//		private val searchName: String,
//		private val searchPrice: String,
//	) : Callable<ParsedProduct> {
//
//		override fun call(): ParsedProduct {
//			return webParser.getProduct(
//				url = url,
//				searchName = searchName,
//				searchPrice = searchPrice
//			)
//		}
//
//	}


}