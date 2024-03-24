package ru.ravel.webparser.entity

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "parser")
data class Parser(
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	@Column(name = "id")
	var id: Long?  = null,

	@OneToMany(
		cascade = [CascadeType.ALL],
		orphanRemoval = true,
		fetch = FetchType.EAGER,
		targetEntity = ParsedProductName::class
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "names")
//	@Column(name = "all_names")
//	@Transient
	var allNames: List<ParsedProductName> = mutableListOf(),

	@OneToOne
//	@PrimaryKeyJoinColumn
//	@Transient
	var selectedName: ParsedProductName = ParsedProductName(),

	@OneToMany(
		cascade = [CascadeType.ALL],
		orphanRemoval = true,
		fetch = FetchType.EAGER,
		targetEntity = ParsedProductPrice::class
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "prices")
//	@Column(name = "price")
//	@Transient
	var allPrices: List<ParsedProductPrice> = mutableListOf(),

//	@Transient
	@OneToOne
//	@PrimaryKeyJoinColumn
	var selectedPrice: ParsedProductPrice = ParsedProductPrice(),

	@Column(name = "store_url")
	var storeUrl: String = ""
)