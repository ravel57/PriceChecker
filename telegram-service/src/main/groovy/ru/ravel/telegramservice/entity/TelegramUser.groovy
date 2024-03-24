package ru.ravel.telegramservice.entity


import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import ru.ravel.core.dto.ParseInfo
import ru.ravel.telegramservice.dto.State

import javax.persistence.CascadeType
import javax.persistence.FetchType
import javax.persistence.OneToOne

@Entity
@Table(name = "telegram_user")
class TelegramUser {

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	Long id

	@Column(name = "telegram_id")
	Long telegramId

	@Column(name = "current_state")
	State currentState

	@Column(name = "last_user_message_id")
	Integer lastUserMessageId

	@Column(name = "last_bot_message_id")
	Integer lastBotMessageId

	@Column(name = "callback_query_id")
	String callbackQueryId

	@Column(name = "parse_info")
	@JdbcTypeCode(SqlTypes.JSON)
	ParseInfo parseInfo = new ParseInfo()

	TelegramUser() {
	}

	TelegramUser(Long telegramId) {
		this.telegramId = telegramId
		this.currentState = State.NONE
	}
}