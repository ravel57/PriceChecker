package ru.ravel.webparser.dto

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "parser")
data class Parser(
	@Id
	@Column(name = "id")
	var id: Long  = -1,

	@OneToMany(
		cascade = [CascadeType.ALL],
		orphanRemoval = true,
		fetch = FetchType.EAGER,
		targetEntity = ParsedProductName::class
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "parsed_product_name")
	@Column(name = "all_names")
	var allNames: List<ParsedProductName> = mutableListOf(),

	@OneToOne
	var selectedName: ParsedProductName = ParsedProductName(),

	@OneToMany(
		cascade = [CascadeType.ALL],
		orphanRemoval = true,
		fetch = FetchType.EAGER,
		targetEntity = ParsedProductPrice::class
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "parsed_product_price")
	@Column(name = "price")
	var allPrices: List<ParsedProductPrice> = mutableListOf(),

	@OneToOne
	var selectedPrice: ParsedProductPrice = ParsedProductPrice(),

	@Column(name = "store_url")
	var storeUrl: String = ""
)