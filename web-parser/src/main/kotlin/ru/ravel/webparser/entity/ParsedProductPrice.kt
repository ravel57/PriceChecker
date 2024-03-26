package ru.ravel.webparser.entity

import jakarta.persistence.*
import ru.ravel.core.util.stringToDouble

@Entity
@Table(name = "parsed_product_price")
data class ParsedProductPrice(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	var strValue: String = "0",

	@Transient
	var value: Double = stringToDouble(strValue),

	var idAtr: String = "",

	var classAtr: String = "",
)