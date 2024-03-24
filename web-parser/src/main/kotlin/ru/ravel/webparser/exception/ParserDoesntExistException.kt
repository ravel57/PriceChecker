package ru.ravel.webparser.exception

class ParserDoesntExistException(
	override val message: String,
	val payload: Any? = null,
) : Exception(message)