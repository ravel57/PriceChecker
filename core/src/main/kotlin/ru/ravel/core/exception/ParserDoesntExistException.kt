package ru.ravel.core.exception

import ru.ravel.core.dto.ParseInfo

class
ParserDoesntExistException(
	override val message: String,
	val payload: ParseInfo? = null,
) : Exception(message)