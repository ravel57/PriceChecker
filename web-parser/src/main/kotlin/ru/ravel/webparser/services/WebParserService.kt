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
import ru.ravel.webparser.repository.WebRepository
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.function.Predicate

@Service
class WebParserService {

	@Autowired
	private lateinit var repository: Repository
	@Autowired
	private lateinit var repository2: WebRepository

	@Autowired
	private lateinit var kafka: KafkaTemplate<Long, ParsedProduct>

	private val logger = LoggerFactory.getLogger(this.javaClass)


	fun getProduct(url: String): ParsedProduct {
		val parser = if (repository.isStoreParserExist(url)) {
			repository.getParser(url)
		} else {
			throw ParserDoesntExistException("parser not exist for selected store")
		}
		val parsedProduct: ParsedProduct = getByJSoup(url, parser)
		kafka.send("product-price-changed-event", parsedProduct.id!!, parsedProduct).whenCompleteAsync { result, ex ->
			if (ex == null)
				logger.info(result.toString())
			else
				logger.error(ex.message, ex)
		}
		return parsedProduct
	}

	fun getProduct(parseInfo: ParseInfo): ParsedProduct {
		val parser = if (!repository.isStoreParserExist(parseInfo.url!!)) {
			val parser = getParserByJSoup(parseInfo)
			repository2.save(parser)
			parser
		} else {
			repository.getParser(parseInfo.url!!)
		}

		val parsedProduct: ParsedProduct = getByJSoup(parseInfo.url!!, parser)
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


	private fun getByJSoup(url: String, parser: Parser): ParsedProduct {
		return try {
			val document = Jsoup.connect(url).followRedirects(true).timeout(60000).get()
			val allElements = document.body().allElements
			val price = allElements
				.map { node -> node.getElementsByAttributeValue("class", parser.selectedPrice.classAtr) }
				.find { it.size > 0 }?.first()?.childNodes()?.first().toString()

			val name = allElements
				.map { node -> node.getElementsByAttributeValue("class", parser.selectedName.classAtr) }
				.find { it.size > 0 }?.first()?.childNodes()?.first().toString()

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
			val parsedProductNames: List<ParsedProductName> = getNodes(allElements, nameFilter).map {
				val nameElement = it as Element
				ParsedProductName(0, nameElement.ownText(), nameElement.id(), nameElement.className())
			}

			val priceFilter = { it: TextNode ->
				val replace = it.text().replace(',', '.').replace("[^0-9.]".toRegex(), "")
				val replace1 = parseInfo.price!!.replace(',', '.').replace("[^0-9.]".toRegex(), "")
				replace == replace1
			}
			val parsedProductPrices = getNodes(allElements, priceFilter).map {
				val priceElement = it as Element
				ParsedProductPrice(
					strValue = priceElement.ownText(),
					idAtr = priceElement.id(),
					classAtr = priceElement.className()
				)
			}

			val host = URI(document.location()).host
			Parser(
				allNames = parsedProductNames.distinct(),
				selectedName = parsedProductNames.distinct()[0],
				allPrices = parsedProductPrices.distinct(),
				selectedPrice = parsedProductPrices.distinct()[0],
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