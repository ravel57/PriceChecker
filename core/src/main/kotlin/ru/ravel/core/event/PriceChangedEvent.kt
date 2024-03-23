package ru.ravel.core.event


class PriceChangedEvent(

	var id: Long = -1,

	var name: String,

	var price: String,
)