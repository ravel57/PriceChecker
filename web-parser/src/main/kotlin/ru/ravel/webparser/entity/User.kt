package ru.ravel.webparser.entity

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "parser_users")
data class User(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@OneToMany(
		cascade = [CascadeType.ALL],
		fetch = FetchType.EAGER,
		targetEntity = ParsedProduct::class
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "user_id")
	var products: List<ParsedProduct> = mutableListOf()
)