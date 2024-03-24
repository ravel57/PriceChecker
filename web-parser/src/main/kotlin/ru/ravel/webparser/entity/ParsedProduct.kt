package ru.ravel.webparser.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "parsed_product")
data class ParsedProduct(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	var id: Long? = null,

	var name: String = "",

	var price: String = "",
)