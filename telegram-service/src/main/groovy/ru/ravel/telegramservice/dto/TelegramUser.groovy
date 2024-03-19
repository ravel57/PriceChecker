package ru.ravel.telegramservice.dto

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user", schema = "telegram_service")
class TelegramUser {

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	Long id

	@Column(name = "telegram_id")
	Long telegramId

	String username

	@Column(name = "current_state")
	State currentState

	TelegramUser() {
	}

	TelegramUser(Long telegramId, String username) {
		this.telegramId = telegramId
		this.username = username
		this.currentState = State.NONE
	}
}