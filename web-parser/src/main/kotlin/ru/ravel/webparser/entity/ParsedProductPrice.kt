package ru.ravel.webparser.entity

import jakarta.persistence.*

@Entity
@Table(name = "parsed_product_price")
data class ParsedProductPrice(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	var strValue: String = "0",

	var value: Double = strValue.replace(',', '.').replace("[^0-9.]".toRegex(), "").toDouble(),

	var idAtr: String = "",

	var classAtr: String = "",
)