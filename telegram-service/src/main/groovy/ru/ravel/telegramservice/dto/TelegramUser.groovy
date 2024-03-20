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

	@Column(name = "current_state")
	State currentState

	@Column(name = "last_user_message_id")
	Long lastUserMessageId

	@Column(name = "last_bot_message_id")
	Long lastBotMessageId

	TelegramUser() {
	}

	TelegramUser(Long telegramId) {
		this.telegramId = telegramId
		this.currentState = State.NONE
	}
}