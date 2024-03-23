package ru.ravel.webparser.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "parsed_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
data class ParsedProduct(

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	var id: Long = -1,

	var name: String,

	var price: String,
)