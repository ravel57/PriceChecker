package ru.ravel.webparser.exception

class ParserDoesntExistException(
	override val message: String
) : Exception(message)