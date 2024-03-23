package ru.ravel.webparser.dto

import jakarta.persistence.*

@Entity
@Table(name = "parsed_product_name")
data class ParsedProductName(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long = -1,

	var value: String = "",

	var idAtr: String = "",

	var classAtr: String = ""
)