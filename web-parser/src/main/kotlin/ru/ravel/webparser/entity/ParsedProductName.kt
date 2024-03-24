package ru.ravel.webparser.entity

import jakarta.persistence.*

@Entity
@Table(name = "parsed_product_name")
data class ParsedProductName(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	var value: String = "",

	var idAtr: String = "",

	var classAtr: String = ""
)