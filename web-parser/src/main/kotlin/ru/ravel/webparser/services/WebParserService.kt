package ru.ravel.webparser.services


import com.google.common.base.Suppliers
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import ru.ravel.core.dto.ParseInfo
import ru.ravel.core.exception.ParserDoesntExistException
import ru.ravel.core.util.stringToDouble
import ru.ravel.webparser.entity.*
import ru.ravel.webparser.repository.Repository
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Predicate


@Service
class WebParserService(
	@Autowired
	private val repository: Repository,

	@Autowired
	private val kafka: KafkaTemplate<Long, ParsedProduct>,
) {

	private val logger = LoggerFactory.getLogger(this.javaClass)

	private val suppliers: MutableMap<String, Any> = mutableMapOf()


	fun getProduct(url: String): ParsedProduct {
		val host = URL(url).host
		val parser = if (repository.isStoreParserExist(host)) {
			repository.getParser(host)
		} else {
			repository.saveParser(getParser(url))
			throw ParserDoesntExistException("the parser is not configured for the selected store: $host")
		}
		val supplier = Suppliers.memoizeWithExpiration({ getProductBySelenium(url, parser) }, 30, TimeUnit.MINUTES)
		suppliers.computeIfAbsent(url, { supplier })
		val parsedProduct: ParsedProduct = supplier.get()
		saveParsedProduct(parsedProduct)
		return parsedProduct
	}


//	fun getProduct(parseInfo: ParseInfo): ParsedProduct {
//		val host = URL(parseInfo.url!!).host
//		val parser = if (repository.isStoreParserExist(host)) {
//			repository.getParser(host)
//		} else {
//			repository.saveParser(getParserByJSoup(parseInfo))
//			throw ParserDoesntExistException("the parser is not configured for the selected store: $host")
//		}
//		val parsedProduct: ParsedProduct = getProductByJSoup(parseInfo.url!!, parser)
//		saveParsedProduct(parsedProduct)
//		return parsedProduct
//	}


	@Throws(ParserDoesntExistException::class)
	private fun getParser(url: String): Parser {
		try {
			val host = URI(url).host
			return Parser(storeUrl = host)
		} catch (e: IOException) {
			logger.error("parsing exception: ${e.message}")
			throw RuntimeException(e.message)
		} catch (e: Exception) {
			logger.error("another exception: ${e.message}")
			throw RuntimeException(e.message)
		}
	}

	private fun getParser(parseInfo: ParseInfo): Parser {
		val host = URL(parseInfo.url!!).host
		if (!repository.isStoreParserExist(host)) {
			throw ParserDoesntExistException("the parser is not configured for the selected store: $host")
		}
		val parser = repository.getParser(host)
		val tmpParser = getParserBySelenium(parseInfo)
		parser.apply {
			this.allNames = tmpParser.allNames
			this.allPrices = tmpParser.allPrices
			this.storeUrl = tmpParser.storeUrl
		}
		repository.saveParser(parser)
		return parser
	}


	fun postName(parseInfo: ParseInfo): ParseInfo {
		parseInfo.nameClassAttributes = getParser(parseInfo).allNames?.map { it.classAtr }
		return parseInfo
	}


	fun postPrice(parseInfo: ParseInfo): ParseInfo {
		parseInfo.priceClassAttributes = getParser(parseInfo).allPrices?.map { it.classAtr }
		return parseInfo
	}


	fun setNameAtr(parseInfo: ParseInfo): ParseInfo {
		val parser = getParser(parseInfo)
		val productName = parser.allNames?.find { it.classAtr == parseInfo.selectedNameClassAttribute }
		parser.selectedName = productName
		repository.saveParser(parser)
		return parseInfo
	}


	fun setPriceAtr(parseInfo: ParseInfo): ParseInfo {
		val parser = getParser(parseInfo)
		val productPrice = parser.allPrices?.find { it.classAtr == parseInfo.selectedPriceClassAttribute }
		parser.selectedPrice = productPrice
		repository.saveParser(parser)
		return parseInfo
	}


	private fun saveParsedProduct(parsedProduct: ParsedProduct) {
		repository.saveParsedProduct(parsedProduct)
		kafka.send("product-price-changed-event", parsedProduct.id!!, parsedProduct)
			.whenCompleteAsync { result, ex ->
				if (ex == null) {
					logger.info(result.toString())
				} else {
					logger.error(ex.message, ex)
				}
			}
	}


	private fun getProductByJSoup(url: String, parser: Parser): ParsedProduct {
		try {
			val document = Jsoup.connect(url).followRedirects(true).timeout(60_000).get()
			val allElements = document.body().allElements
			val message = "the parser is not configured for the selected store: $url"
			val name = if (parser.selectedName != null) {
				allElements.map { it.getElementsByAttributeValue("class", parser.selectedName!!.classAtr) }
					.find { it.size > 0 }?.first()?.childNodes()?.first().toString().trim()
			} else {
				throw ParserDoesntExistException(message)
			}
			val price = if (parser.selectedPrice != null) {
				val str = allElements.map { it.getElementsByAttributeValue("class", parser.selectedPrice!!.classAtr) }
					.find { it.size > 0 }?.first()?.childNodes()?.joinToString(" ") ?: ""
				stringToDouble(str)
			} else {
				throw ParserDoesntExistException(message)
			}
			return ParsedProduct(name = name, price = price)
		} catch (e: IOException) {
			logger.error("parsing exception: ${e.message}")
			throw NullPointerException()
		}
	}


	private fun getProductBySelenium(url: String, parser: Parser): ParsedProduct {
		try {
			val driver: WebDriver = FirefoxDriver()
			driver.manage().timeouts().implicitWaitTimeout
			driver.get(url)
			Thread.sleep(5_000)
			val message = "the parser is not configured for the selected store: $url"
			val name = if (parser.selectedName != null) {
				val classAtr = '.' + parser.selectedName!!.classAtr.replace(' ', '.')
				driver.findElement(By.cssSelector(classAtr)).getAttribute("innerText")
			} else {
				driver.quit()
				throw ParserDoesntExistException(message)
			}
			val price = if (parser.selectedPrice != null) {
				val priceAtr = '.' + parser.selectedPrice!!.classAtr.replace(' ', '.')
				stringToDouble(driver.findElement(By.cssSelector(priceAtr)).getAttribute("innerText"))
			} else {
				driver.quit()
				throw ParserDoesntExistException(message)
			}
			return ParsedProduct(name = name, price = price)
		} catch (e: IOException) {
			logger.error("parsing exception: ${e.message}")
			throw NullPointerException()
		}
	}


	@Throws(ParserDoesntExistException::class)
	private fun getParserByJSoup(parseInfo: ParseInfo): Parser {
		try {
			val document = Jsoup.connect(parseInfo.url!!).followRedirects(true).timeout(60_000).get()
			val allElements = document.body().allElements
			val parsedProductNames = if (parseInfo.name != null) {
				val nameFilter = { it: TextNode -> it.text().lowercase().contains(parseInfo.name!!.lowercase()) }
				getNodes(allElements, nameFilter).map {
					it as Element
					ParsedProductName(value = it.ownText(), idAtr = it.id(), classAtr = it.className())
				}
			} else {
				null
			}
			val parsedProductPrices = if (parseInfo.price != null) {
				val priceFilter = { it: TextNode -> stringToDouble(it.text()) == stringToDouble(parseInfo.price!!) }
				getNodes(allElements, priceFilter).map {
					it as Element
					ParsedProductPrice(strValue = it.ownText(), idAtr = it.id(), classAtr = it.className())
				}
			} else {
				null
			}
			val host = URI(document.location()).host
			return Parser(
				allNames = parsedProductNames?.distinct(),
				allPrices = parsedProductPrices?.distinct(),
				storeUrl = host,
			)
		} catch (e: Exception) {
//			return getParserBySelenium(parseInfo)
			logger.error("parsing exception: ${e.message}")
			throw RuntimeException(e.message)
		} catch (e: Exception) {
			logger.error("another exception: ${e.message}")
			throw RuntimeException(e.message)
		}
	}


	private fun getNodes(allElements: Elements, predicate: Predicate<TextNode>): MutableList<Node?> {
		return allElements.stream()
			.map { it.textNodes() }
			.flatMap { it.stream() }
			.filter(predicate)
			.map { it.parentNode() }
			.filter { Objects.nonNull(it) }
			.filter {
				it.attributes().hasKey("id") || it.attributes().hasKey("class") || it.attributes().hasKey("itemprop")
			}
			.toList()
	}


	private fun getParserBySelenium(parseInfo: ParseInfo): Parser {
		val driver: WebDriver = FirefoxDriver()
		driver.manage().timeouts().implicitWaitTimeout
		driver.get(parseInfo.url)
//		WebDriverWait(driver, Duration.of(1, ChronoUnit.MINUTES)).until { webDriver: WebDriver ->
//			(webDriver as JavascriptExecutor).executeScript(
//				"return document.readyState"
//			) == "complete"
//		}
		Thread.sleep(5_000)
		val parsedProductNames = if (parseInfo.name != null) {
			driver.findElements(By.xpath("//*[contains(text(), '${parseInfo.name}')]"))
				.map {
					ParsedProductName(
						value = it.getAttribute("innerText"),
						idAtr = it.getAttribute("id"),
						classAtr = it.getAttribute("class"),
						itemprop = it.getAttribute("itemprop") ?: "",
					)
				}
		} else {
			null
		}
		val parsedProductPrices = if (parseInfo.price != null) {
			val price = parseInfo.price!!.replace(" ", " ")
			val map = driver.findElements(By.xpath("//*[contains(text(), '${price}')]"))
				.map {
					ParsedProductPrice(
						strValue = it.getAttribute("innerText"),
						idAtr = it.getAttribute("id"),
						classAtr = it.getAttribute("class"),
						itemprop = it.getAttribute("itemprop") ?: "",
					)
				}
			val map1 = driver.findElements(By.xpath("//*[contains(text(), '${parseInfo.price!!}')]"))
				.map {
					ParsedProductPrice(
						strValue = it.getAttribute("innerText"),
						idAtr = it.getAttribute("id"),
						classAtr = it.getAttribute("class"),
						itemprop = it.getAttribute("itemprop") ?: "",
					)
				}
			map + map1
		} else {
			null
		}
		val host = URI(parseInfo.url!!).host
		driver.quit()
		return Parser(
			allNames = parsedProductNames,
			allPrices = parsedProductPrices,
			storeUrl = host,
		)
	}

}