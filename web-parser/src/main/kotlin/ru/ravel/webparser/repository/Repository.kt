package ru.ravel.webparser.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.ravel.webparser.dto.Parser

@Repository
class Repository(
	@Autowired
	private val factory: EntityManagerFactory,

	) {

	private val entityManager: EntityManager = factory.createEntityManager()

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

}