package ru.ravel.webparser.services


import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.function.Predicate

@Service
class WebParser {

	private val logger = LoggerFactory.getLogger(WebParser::class.java)


	fun getPrice(url: String, searchName: String, price: String): ParsedProduct {
		return getByJSoup(url, price, searchName)
	}


	@Throws(NullPointerException::class)
	private fun getByJSoup(url: String, price: String, searchName: String): ParsedProduct {
		return try {
			val document = Jsoup.connect(url).followRedirects(true).timeout(60000).get()
			val allElements = document.body().allElements

			val nameFilter = { it: TextNode -> it.text().contains(searchName) }
			val names = getNodes(allElements, nameFilter).map {
				val nameElement = it as Element
				Name(nameElement.ownText(), nameElement.id(), nameElement.className())
			}

			val priceFilter = { it: TextNode ->
				val replace = it.text().replace(',', '.').replace("[^0-9.]".toRegex(), "")
				val replace1 = price.replace(',', '.').replace("[^0-9.]".toRegex(), "")
				replace == replace1
			}
			val prices = getNodes(allElements, priceFilter).map {
				val priceElement = it as Element
				Price(priceElement.ownText(), priceElement.id(), priceElement.className())
			}

			val host = URI(document.location()).host
			ParsedProduct(name = names.distinct(), price = prices.distinct(), shopUrl = host)
		} catch (e: IOException) {
			logger.error("parsing exception: ${e.message}")
			throw NullPointerException()
		}
	}


	private fun getNodes(allElements: Elements, predicate: Predicate<TextNode>): List<Node?> {
		return allElements.stream()
			.map { it.textNodes() }
			.flatMap { it.stream() }
			.filter(predicate)
			.map { it.parentNode() }
			.filter { Objects.nonNull(it) }
			.filter { it.attributes().hasKey("id") || it.attributes().hasKey("class") }
			.toList()
	}


	private fun getBySelenium(url: String, documentElement: String): Price? {
		return null
	}


	data class Price(
		var strValue: String,
		var idAtr: String,
		var classAtr: String,
		var value: Double = strValue.replace(',', '.').replace("[^0-9.]".toRegex(), "").toDouble()
	) {
	}


	data class Name(
		var value: String,
		var idAtr: String,
		var classAtr: String
	) {
		override fun toString(): String {
			return value
		}
	}


	data class ParsedProduct(
		var name: List<Name>,
		var price: List<Price>,
		var shopUrl: String
	) {
		override fun toString(): String {
			return "ParsedProduct{name=%s, price=%s, shopUrl='%s'}".format(name, price, shopUrl)
		}
	}

}