package ru.ravel.core.dto

data class ParseInfo(
	var url: String? = null,
	var name: String? = null,
	var price: String? = null,
	var userId: Long? = null,
	var nameClassAttributes: List<String>? = null,
	var selectedNameClassAttribute: String? = null,
	var priceClassAttributes: List<String>? = null,
	var selectedPriceClassAttribute: String? = null,
)