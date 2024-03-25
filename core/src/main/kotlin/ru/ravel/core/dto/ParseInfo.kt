package ru.ravel.core.dto

data class ParseInfo(
	var url: String? = null,
	var name: String? = null,
	var price: String? = null,
	var userId: Long? = null,
	var priceClassArt: String? = null,
	var nameClassArt: String? = null,
)