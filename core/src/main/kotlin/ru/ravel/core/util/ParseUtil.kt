package ru.ravel.core.util

class ParseUtil

fun stringToDouble(str: String): Double {
	return try {
		str.replace(',', '.').replace("[^0-9.]".toRegex(), "").toDouble()
	} catch (e: Exception) {
		0.0
	}
}