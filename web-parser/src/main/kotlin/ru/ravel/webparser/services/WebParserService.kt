package ru.ravel.webparser.services


import ru.ravel.core.dto.ParseInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import ru.ravel.webparser.exception.ParserDoesntExistException
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
			throw ParserDoesntExistException("parser not exist for selected store")
		}
		val parsedProduct: ParsedProduct = getByJSoup(url, parser)
		repository.saveParsedProduct(parsedProduct)
		kafka.send("product-price-changed-event", parsedProduct.id!!, parsedProduct).whenCompleteAsync { result, ex ->
			if (ex == null)
				logger.info(result.toString())
			else
				logger.error(ex.message, ex)
		}
		return parsedProduct
	}

	fun getProduct(parseInfo: ParseInfo): ParsedProduct {
		val host = URL(parseInfo.url!!).host
		val parser = if (repository.isStoreParserExist(host)) {
			repository.getParser(host)
		} else {
			val parser = getParserByJSoup(parseInfo)
			repository.saveParser(parser)
			val map = mapOf(
				Pair("names", parser.allNames.map { it.classAtr }),
				Pair("prices", parser.allPrices.map { it.classAtr })
			)
			throw ParserDoesntExistException("parser not specified for attributes", map)
		}
		val parsedProduct: ParsedProduct = getByJSoup(parseInfo.url!!, parser)
		repository.saveParsedProduct(parsedProduct)
		kafka.send("product-price-changed-event", parsedProduct.id!!, parsedProduct)
			.whenCompleteAsync { result, ex ->
				if (ex == null) {
					logger.info(result.toString())
				} else {
					logger.error(ex.message, ex)
				}
			}
		return parsedProduct
	}

	fun setNameAtr(nameAtr: String, userID: Long): Any? {
		return null
	}

	fun setPriceAtr(priceAtr: String, userID: Long): Any? {
		return null
	}


	private fun getByJSoup(url: String, parser: Parser): ParsedProduct {
		return try {
			val document = Jsoup.connect(url).followRedirects(true).timeout(60000).get()
			val allElements = document.body().allElements
			val price = allElements
				.map { it.getElementsByAttributeValue("class", parser.selectedPrice!!.classAtr) }
				.find { it.size > 0 }?.first()?.childNodes()?.first().toString().trim()
			val name = allElements
				.map { it.getElementsByAttributeValue("class", parser.selectedName!!.classAtr) }
				.find { it.size > 0 }?.first()?.childNodes()?.first().toString().trim()
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
		return try {
			val document = Jsoup.connect(parseInfo.url!!).followRedirects(true).timeout(60000).get()
			val allElements = document.body().allElements
			val nameFilter = { it: TextNode -> it.text().contains(parseInfo.name!!) }
			val parsedProductNames= getNodes(allElements, nameFilter).map {
				val nameElement = it as Element
				val parsedProductName = ParsedProductName(
					value = nameElement.ownText(),
					idAtr = nameElement.id(),
					classAtr = nameElement.className()
				)
				repository.saveParsedProductName(parsedProductName)
				parsedProductName
			}
			val priceFilter = { it: TextNode ->
				val replace = it.text().replace(',', '.').replace("[^0-9.]".toRegex(), "")
				val replace1 = parseInfo.price!!.replace(',', '.').replace("[^0-9.]".toRegex(), "")
				replace == replace1
			}
			val parsedProductPrices = getNodes(allElements, priceFilter).map {
				val priceElement = it as Element
				val parsedProductPrice = ParsedProductPrice(
					strValue = priceElement.ownText(),
					idAtr = priceElement.id(),
					classAtr = priceElement.className()
				)
				repository.saveParsedProductPrice(parsedProductPrice)
				parsedProductPrice
			}
			val host = URI(document.location()).host
			Parser(
				allNames = parsedProductNames.distinct(),
//				selectedName = parsedProductNames.distinct()[1],
				allPrices = parsedProductPrices.distinct(),
//				selectedPrice = parsedProductPrices.distinct()[1],
				storeUrl = host,
			)
		} catch (e: IOException) {
			logger.error("parsing exception: ${e.message}")
			throw NullPointerException()
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