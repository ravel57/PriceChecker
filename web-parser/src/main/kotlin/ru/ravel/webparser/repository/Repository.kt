package ru.ravel.webparser.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.ravel.webparser.entity.Parser

@Repository
class Repository(
	@Autowired
	private val factory: EntityManagerFactory,

	) {

	private val entityManager: EntityManager = factory.createEntityManager()

	private val sessionFactory: SessionFactory = factory.unwrap(SessionFactory::class.java)


	fun isStoreParserExist(url: String): Boolean {
		return entityManager.createQuery("select count (*) > 0 from Parser where storeUrl = :url")
			.setParameter("url", url)
			.firstResult > 0
	}

	fun getParser(url: String) : Parser {
		return entityManager.createQuery("select Parser from Parser where storeUrl = :url")
			.setParameter("url", url)
			.firstResult as Parser
	}

	fun saveParser(parser: Parser) {
		val session = sessionFactory.openSession()
		session.beginTransaction()
		session.saveOrUpdate(parser)
		session.transaction.commit()
		session.close()
	}

}