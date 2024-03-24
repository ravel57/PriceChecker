package ru.ravel.webparser.entity

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "parser_users")
data class User(
	@Id
	var id: Long = -1,

	@OneToMany(
		cascade = [CascadeType.ALL],
		fetch = FetchType.EAGER,
		targetEntity = ParsedProduct::class
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "products")
	var products: List<ParsedProduct> = mutableListOf()
)