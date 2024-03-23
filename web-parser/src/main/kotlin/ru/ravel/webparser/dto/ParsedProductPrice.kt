package ru.ravel.webparser.dto

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Entity
@Table(name = "parsed_product_price")
data class ParsedProductPrice(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long = -1,

	var strValue: String = "0",

	var value: Double = strValue.replace(',', '.').replace("[^0-9.]".toRegex(), "").toDouble(),

	var idAtr: String = "",

	var classAtr: String = "",
)