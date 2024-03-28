package ru.ravel.telegramservice.builder

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.response.SendResponse

class EditMessageBuilder extends MessageBuilder {

	private String text
	private Integer messageId
	private ParseMode parseMode
	private List<InlineKeyboardButton> buttons
	private Integer keyboardOffset


	EditMessageBuilder(TelegramBot bot) {
		super(bot)
	}

	EditMessageBuilder telegramId(Long telegramId) {
		this.telegramId = telegramId
		return this
	}

	EditMessageBuilder text(String text) {
		this.text = text
		return this
	}

	EditMessageBuilder messageId(Integer messageId) {
		this.messageId = messageId
		return this
	}

	EditMessageBuilder buttons(Integer keyboardOffset, List<InlineKeyboardButton> buttons) {
		this.keyboardOffset = keyboardOffset
		this.buttons = buttons
		return this
	}

	EditMessageBuilder buttons(Integer keyboardOffset, InlineKeyboardButton... buttons) {
		this.keyboardOffset = keyboardOffset
		this.buttons = buttons
		return this
	}

	/**
	 * callbackData = "back"
	 * @param text button text
	 * @return SendEditMessageBuilder
	 */
	EditMessageBuilder addBackButton(String text) {
		this.buttons.add(new InlineKeyboardButton(text).callbackData("back"))
		return this
	}

	EditMessageBuilder parseMode(ParseMode parseMode) {
		this.parseMode = parseMode
		return this
	}

	void execute() {
		if (telegramId == null || text == null || this.messageId == null) {
			throw new NoSuchFieldException()
		}
		EditMessageText message = new EditMessageText(telegramId, this.messageId, text)

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
			message.replyMarkup(inlineKeyboard)
		}
		if (parseMode != null) {
			message.parseMode(parseMode)
		}
		bot.execute(message)
	}
}