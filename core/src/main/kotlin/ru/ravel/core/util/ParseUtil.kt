package ru.ravel.core.util

class ParseUtil

fun stringToDouble(str: String): Double {
	return try {
		return str.replace(',', '.')
			.replace("[^0-9.]".toRegex(), "")
//			.replace("^.".toRegex(), "")
//			.replace(".$".toRegex(), "")
			.toDouble()
	} catch (e: Exception) {
		0.0
	}
}