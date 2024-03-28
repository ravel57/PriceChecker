package ru.ravel.telegramservice.builder

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse

class MessageBuilder {
	private TelegramBot bot
	private Method method
	private Long id
	private String text
	private List<InlineKeyboardButton> buttons
	private ParseMode parseMode
	private Integer messageId
	private Integer keyboardOffset

	enum Method {
		SEND,
		EDIT,
		DELETE,
	}

	MessageBuilder(TelegramBot bot) {
		this.bot = bot
	}

	MessageBuilder method(Method method) {
		this.method = method
		return this
	}

	MessageBuilder id(Long telegramId) {
		this.id = telegramId
		return this
	}

	MessageBuilder text(String text) {
		this.text = text
		return this
	}

	MessageBuilder buttons(List<InlineKeyboardButton> buttons) {
		this.buttons = buttons
		return this
	}

	MessageBuilder buttons(InlineKeyboardButton... buttons) {
		this.buttons = buttons
		return this
	}

	MessageBuilder addBackButton(String text) {
		this.buttons.add(new InlineKeyboardButton(text).callbackData("back"))
		return this
	}

	MessageBuilder parseMode(ParseMode parseMode) {
		this.parseMode = parseMode
		return this
	}

	MessageBuilder messageId(Integer messageId) {
		this.messageId = messageId
		return this
	}

	MessageBuilder keyboardOffset(Integer keyboardOffset) {
		this.keyboardOffset = keyboardOffset
		return this
	}


	Integer send() {
		if (id == null || method == null || (text == null && method in [Method.SEND, Method.EDIT]) ||
				(messageId == null && method in [Method.EDIT, Method.DELETE])) {
			throw new NoSuchFieldException()
		}
		var message = switch (method) {
			case Method.SEND -> new SendMessage(id, text)
			case Method.EDIT -> new EditMessageText(id, messageId, text)
			case Method.DELETE -> new DeleteMessage(id, messageId)
		}
		if (buttons != null) {
			def inlineKeyboard = new InlineKeyboardMarkup()
			def row = []
			buttons.each { button ->
				row.add(button)
				if (row.size() == keyboardOffset) {
					inlineKeyboard.addRow(row as InlineKeyboardButton[])
					row = []
				}
			}
			if (!row.isEmpty()) {
				inlineKeyboard.addRow(row as InlineKeyboardButton[])
			}
			switch (method) {
				case Method.SEND -> (message as SendMessage).replyMarkup(inlineKeyboard)
				case Method.EDIT -> (message as EditMessageText).replyMarkup(inlineKeyboard)
			}
		}
		if (parseMode != null) {
			switch (method) {
				case Method.SEND -> (message as SendMessage).parseMode(parseMode)
				case Method.EDIT -> (message as EditMessageText).parseMode(parseMode)
			}
		}
		return (bot.execute(message) as SendResponse).message().messageId()
	}
}
