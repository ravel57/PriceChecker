package ru.ravel.webparser.services


import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
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
import java.util.function.Predicate

@Service
class WebParserService(
	@Autowired
	private val repository: Repository,

	@Autowired
	private val kafka: KafkaTemplate<Long, ParsedProduct>,
) {

	private val logger = LoggerFactory.getLogger(this.javaClass)


	fun getProduct(url: String): ParsedProduct {
		val host = URL(url).host
		val parser = if (repository.isStoreParserExist(host)) {
			repository.getParser(host)
		} else {
			repository.saveParser(getParserByJSoup(url))
			throw ParserDoesntExistException("the parser is not configured for the selected store: $host")
		}
		val parsedProduct: ParsedProduct = getProductByJSoup(url, parser)
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

	fun getParser(parseInfo: ParseInfo): Parser {
		val host = URL(parseInfo.url!!).host
		if (!repository.isStoreParserExist(host)) {
			throw ParserDoesntExistException("the parser is not configured for the selected store: $host")
		}
		val parser = repository.getParser(host)
		val tmpParser = getParserByJSoup(parseInfo)
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
		return try {
			val document = Jsoup.connect(url).followRedirects(true).timeout(60_000).get()
			val allElements = document.body().allElements
			val message = "the parser is not configured for the selected store: $url"
			val price = if (parser.selectedPrice != null) {
				val str = allElements.map { it.getElementsByAttributeValue("class", parser.selectedPrice!!.classAtr) }
					.find { it.size > 0 }?.first()?.childNodes().toString()
				stringToDouble(str)
			} else {
				throw ParserDoesntExistException(message)
			}
			val name = if (parser.selectedName != null) {
				allElements.map { it.getElementsByAttributeValue("class", parser.selectedName!!.classAtr) }
					.find { it.size > 0 }?.first()?.childNodes()?.first().toString().trim()
			} else {
				throw ParserDoesntExistException(message)
			}
			ParsedProduct(
				name = name,
				price = price,
			)
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
		} catch (e: IOException) {
			logger.error("parsing exception: ${e.message}")
			throw RuntimeException(e.message)
		} catch (e: Exception) {
			logger.error("another exception: ${e.message}")
			throw RuntimeException(e.message)
		}
	}


	@Throws(ParserDoesntExistException::class)
	private fun getParserByJSoup(url: String): Parser {
		try {
			val document = Jsoup.connect(url).followRedirects(true).timeout(60_000).get()
			val host = URI(document.location()).host
			return Parser(storeUrl = host)
		} catch (e: IOException) {
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
			.filter { it.attributes().hasKey("id") || it.attributes().hasKey("class") }
			.toList()
	}


	private fun getBySelenium(url: String, documentElement: String): ParsedProductPrice? {
		return null
	}

}