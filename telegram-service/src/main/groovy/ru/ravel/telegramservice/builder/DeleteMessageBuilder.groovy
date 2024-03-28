package ru.ravel.telegramservice.builder

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.response.SendResponse

class DeleteMessageBuilder extends MessageBuilder {

	private Integer messageId


	DeleteMessageBuilder(TelegramBot bot) {
		super(bot)
	}

	DeleteMessageBuilder telegramId(Long telegramId) {
		this.telegramId = telegramId
		return this
	}

	DeleteMessageBuilder messageId(Integer messageId) {
		this.messageId = messageId
		return this
	}

	void execute() {
		if (telegramId == null || this.messageId == null) {
			throw new NoSuchFieldException()
		}
		var message = new DeleteMessage(telegramId, this.messageId)

		SendResponse response = bot.execute(message) as SendResponse
		if (!response.ok) {
			throw new RuntimeException(response.description())
		}
	}

}