package ru.ravel.telegramservice.builder

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse

class SendMessageBuilder extends MessageBuilder {

	private String text
	private ParseMode parseMode
	private List<InlineKeyboardButton> buttons
	private Integer keyboardOffset


	SendMessageBuilder(TelegramBot bot) {
		super(bot)
	}

	SendMessageBuilder telegramId(Long telegramId) {
		this.telegramId = telegramId
		return this
	}

	SendMessageBuilder text(String text) {
		this.text = text
		return this
	}

	SendMessageBuilder buttons(Integer keyboardOffset, List<InlineKeyboardButton> buttons) {
		this.keyboardOffset = keyboardOffset
		this.buttons = buttons
		return this
	}

	SendMessageBuilder buttons(Integer keyboardOffset, InlineKeyboardButton... buttons) {
		this.keyboardOffset = keyboardOffset
		this.buttons = buttons
		return this
	}

	/**
	 * callbackData = "back"
	 * @param text button text
	 * @return SendMessageBuilder
	 */
	SendMessageBuilder addBackButton(String text) {
		this.buttons.add(new InlineKeyboardButton(text).callbackData("back"))
		return this
	}

	SendMessageBuilder parseMode(ParseMode parseMode) {
		this.parseMode = parseMode
		return this
	}

	Integer execute() {
		if (telegramId == null || text == null) {
			throw new NoSuchFieldException()
		}
		SendMessage message = new SendMessage(telegramId, text)
		if (buttons != null && keyboardOffset != null) {
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
			message.replyMarkup(inlineKeyboard)
		}
		if (parseMode != null) {
			message.parseMode(parseMode)
		}

		SendResponse response = bot.execute(message) as SendResponse
		if (response.ok) {
			return response.message().messageId()
		} else {
			throw new RuntimeException(response.description())
		}
	}
}
