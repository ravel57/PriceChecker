package ru.ravel.telegramservice.builder

import com.pengrad.telegrambot.TelegramBot

class MessageBuilder <T> {
	protected final TelegramBot bot
	protected Long telegramId

	MessageBuilder(TelegramBot bot) {
		this.bot = bot
	}

	MessageBuilder telegramId(Long telegramId) {
		this.telegramId = telegramId
		return this
	}

	SendMessageBuilder send() {
		return new SendMessageBuilder(bot)
	}

	EditMessageBuilder edit() {
		return new EditMessageBuilder(bot)
	}

	DeleteMessageBuilder delete() {
		return new DeleteMessageBuilder(bot)
	}

}
