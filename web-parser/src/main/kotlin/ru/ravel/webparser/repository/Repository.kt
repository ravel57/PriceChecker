package ru.ravel.webparser.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.ravel.webparser.entity.ParsedProduct
import ru.ravel.webparser.entity.ParsedProductName
import ru.ravel.webparser.entity.ParsedProductPrice
import ru.ravel.webparser.entity.Parser

@Repository
class Repository(
	@Autowired
	private val factory: EntityManagerFactory,

	) {

	private val entityManager: EntityManager = factory.createEntityManager()

	private val sessionFactory: SessionFactory = factory.unwrap(SessionFactory::class.java)


	fun isStoreParserExist(url: String): Boolean {
		return entityManager.createQuery("select count (storeUrl) > 0 from Parser where storeUrl = :url")
			.setParameter("url", url)
			.resultList.first() as Boolean
	}

	fun getParser(storeUrl: String) : Parser {
		return entityManager.createQuery("from Parser where storeUrl = :storeUrl")
			.setParameter("storeUrl", storeUrl)
			.resultList.first() as Parser
	}

	fun saveParser(parser: Parser) {
		val session = sessionFactory.openSession()
		session.beginTransaction()
		session.saveOrUpdate(parser)
		session.transaction.commit()
		session.close()
	}

	fun saveParsedProductName(parsedProductName: ParsedProductName) {
		val session = sessionFactory.openSession()
		session.beginTransaction()
		session.saveOrUpdate(parsedProductName)
		session.transaction.commit()
		session.close()
	}

	fun saveParsedProductPrice(parsedProductPrice: ParsedProductPrice) {
		val session = sessionFactory.openSession()
		session.beginTransaction()
		session.saveOrUpdate(parsedProductPrice)
		session.transaction.commit()
		session.close()
	}

	fun saveParsedProduct(parsedProduct: ParsedProduct) {
		val session = sessionFactory.openSession()
		session.beginTransaction()
		session.saveOrUpdate(parsedProduct)
		session.transaction.commit()
		session.close()
	}

}