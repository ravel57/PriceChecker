package ru.ravel.telegramservice.service

import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.ravel.core.dto.ParseInfo
import ru.ravel.telegramservice.dto.Command
import ru.ravel.telegramservice.dto.State
import ru.ravel.telegramservice.entity.TelegramUser
import ru.ravel.telegramservice.repository.TelegramUserRepository

@Service
class TelegramService {

	private Logger logger = LoggerFactory.getLogger(this.class)

	private TelegramBot bot

	@Autowired
	private TelegramUserRepository repository

	@Autowired
	private PriceCheckerService checkerService

	TelegramService() {
		bot = new TelegramBot(System.getenv("bot_token"))
		bot.setUpdatesListener(listener, exceptionHandler)
	}


	private UpdatesListener listener = new UpdatesListener() {
		@Override
		int process(List<Update> updates) {
			try {
				updates.each { update ->
					Long telegramId
					TelegramUser telegramUser

					if (update?.message()) {
						telegramId = update.message().from().id()
						telegramUser = repository.getByTelegramId(telegramId)

						if (telegramUser == null) {
							telegramUser = checkerService.saveNewUser(new TelegramUser(telegramId))
							repository.save(telegramUser)
						}
						String messageText = update.message().text()
						telegramUser.lastUserMessageId = update.message().messageId()
						repository.save(telegramUser)

						if (update?.message()?.text() && messageText.startsWith('/')) {
							switch (Command.getByText(messageText)) {
								case Command.START -> {
									sendGreetingMessage(telegramId)
								}
							}
						} else {
							if (telegramUser.parseInfo == null) {
								telegramUser.parseInfo = new ParseInfo()
							}
							stateHandler(telegramUser, messageText)
						}
					} else if (update?.callbackQuery()) {
						telegramId = update.callbackQuery().from().id()
						telegramUser = repository.getByTelegramId(telegramId)
						callbackHandler(update, telegramUser)
					}
				}
			} catch (e) {
				logger.error(e.message, e)
			}
			return UpdatesListener.CONFIRMED_UPDATES_ALL
		}
	}


	static SendMessage messageAskToUser(String enterText, TelegramUser telegramUser, List<String> variables, String mode) {
		String vars = "$enterText\n\n"
		vars += variables.indexed()
				.collect { index, variable -> "<b>[${index + 1}]</b>\n$variable" }
				.join('\n\n')
		def buttons = variables.indexed().collect { index, item ->
			new InlineKeyboardButton(index + 1 as String).callbackData("$mode=$item")
		}
		return new SendMessage(telegramUser.telegramId, vars)
				.parseMode(ParseMode.HTML)
				.replyMarkup(keyboardBuilder(buttons, 2))
	}


	void stateHandler(TelegramUser telegramUser, String messageText) {
		String text = switch (telegramUser.currentState) {
			case State.LINK_ADDING -> {
				sendSearchMessage(telegramUser)
				telegramUser.parseInfo.url = messageText
				def result = checkerService.getProduct(telegramUser.parseInfo.url)
				if (result.isParserExist) {
					deleteMessage(telegramUser.telegramId, telegramUser.lastBotMessageId)
					telegramUser.currentState = State.NONE
					SendMessage request = new SendMessage(telegramUser.telegramId,
							"""
							|<b>Готово!</b>
							|<b><u>${result.parseInfoResult.name}</u></b>
							|<b><u>${result.parseInfoResult.price}</u></b>
							""".stripMargin())
							.parseMode(ParseMode.HTML)
					sendMessage(request, telegramUser.telegramId)
					sendGreetingMessage(telegramUser.telegramId)
					""
				} else {
					telegramUser.currentState = State.NAME_ADDING
					logger.debug(messageText) /*URL*/
					editMessage(telegramUser.telegramId,
							telegramUser.lastBotMessageId,
							"Пришли <u>ссылку</u> на товар\n<b><i>$messageText</i></b> - принято✅")
					"<b>Парсер нужо настроить</b>\nПришли <i><u>полное название</u></i> товара со страницы"
				}
			}

			case State.NAME_ADDING -> {
				telegramUser.parseInfo.name = messageText
				sendSearchMessage(telegramUser)
				if (nameClassAdding(telegramUser)) {
					""
				}
			}

			case State.PRICE_ADDING -> {
				telegramUser.parseInfo.price = messageText
				sendSearchMessage(telegramUser, true)
				if (priceClassAdding(telegramUser)) {
					""
				}
			}
		}
		repository.save(telegramUser)
		if (telegramUser.currentState != State.NONE && text != null && text != "") {
			SendMessage request = new SendMessage(telegramUser.telegramId, text)
					.parseMode(ParseMode.HTML)
			sendMessage(request, telegramUser.telegramId)
		}
	}

	boolean nameClassAdding(TelegramUser telegramUser) {
		telegramUser.parseInfo = checkerService.postProductName(telegramUser.parseInfo).parseInfoResult
		if (telegramUser.parseInfo.nameClassAttributes.size() > 1) {
			sendMessage(
					messageAskToUser("Что больше похоже на класс названия?",
							telegramUser,
							telegramUser.parseInfo.nameClassAttributes,
							State.CLASS_NAME_ART.name()),
					telegramUser.telegramId)
			return false
		} else if (telegramUser.parseInfo.nameClassAttributes.size() == 0) {
			telegramUser.currentState = State.NAME_ADDING
			editMessage(
					telegramUser.telegramId,
					telegramUser.searchMessageId,
					"Не найден❌")
			SendMessage request = new SendMessage(
					telegramUser.telegramId,
					"""
					|Такого элемента <b>не нашлось</b>😥
					|Проверьте <b><u>корректность отправленого названия</u></b> и попробуйте снова
					""".stripMargin()).parseMode(ParseMode.HTML)
			sendMessage(request, telegramUser.telegramId)
			return false
		} else {
			return true
		}
	}

	boolean priceClassAdding(TelegramUser telegramUser) {
		telegramUser.parseInfo = checkerService.postProductPrice(telegramUser.parseInfo).parseInfoResult
		repository.save(telegramUser)
		if (telegramUser.parseInfo.priceClassAttributes.size() > 1) {
			sendMessage(
					messageAskToUser(
							"Что больше похоже на class цены?",
							telegramUser,
							telegramUser.parseInfo.priceClassAttributes,
							State.CLASS_PRICE_ART.name()
					),
					telegramUser.telegramId
			)
			return false
		} else if (telegramUser.parseInfo.priceClassAttributes.size() == 0) {
			telegramUser.currentState = State.PRICE_ADDING
			editMessage(
					telegramUser.telegramId,
					telegramUser.searchMessageId,
					"Не найден❌")
			SendMessage request = new SendMessage(
					telegramUser.telegramId,
					"""
					|Такого элемента <b>не нашлось</b>😥
					|Проверьте <b><u>корректность отправленой цены</u></b>  и попробуйте снова
					""".stripMargin()).parseMode(ParseMode.HTML)
			sendMessage(request, telegramUser.telegramId)
			return false
		} else {
			return true
		}
	}

	private void callbackHandler(Update update, TelegramUser telegramUser) {
		String callbackDataStr = update.callbackQuery().data()
		telegramUser.lastBotMessageId = update.callbackQuery().message().messageId()
		repository.save(telegramUser)
		if (callbackDataStr.startsWith("CLASS")) {
			SendMessage request
			String[] callbackData = callbackDataStr.split("=")
			switch (State.valueOf(callbackData[0])) {
				case State.CLASS_NAME_ART -> {
					deleteMessage(telegramUser.telegramId, telegramUser.lastBotMessageId)
					telegramUser.parseInfo = checkerService.postNameClassAtr(telegramUser.parseInfo, callbackData[-1])
							.parseInfoResult
					telegramUser.currentState = State.PRICE_ADDING

					editMessage(telegramUser.telegramId,
							telegramUser.searchMessageId,
							"""
							|Пришли <i><u>полное название</u></i> товара со страницы
							|<b><i>$telegramUser.parseInfo.name</i></b> - принято✅
							""".stripMargin()
					)
					request = new SendMessage(telegramUser.telegramId, "Пришли <b>цену</b> этого товара")
							.parseMode(ParseMode.HTML)
					sendMessage(request, telegramUser.telegramId)
				}
				case State.CLASS_PRICE_ART -> {
					deleteMessage(telegramUser.telegramId, telegramUser.lastBotMessageId)
					telegramUser.parseInfo = checkerService.postPriceClassAtr(telegramUser.parseInfo, callbackData[-1])
							.parseInfoResult
					telegramUser.currentState = State.NONE

					editMessage(telegramUser.telegramId, telegramUser.searchMessageId,
							"<b><i>$telegramUser.parseInfo.price</i></b> - принято✅"
					)

					request = new SendMessage(
							telegramUser.telegramId,
							"""
							|<b>Настройка персера завершена!</b>
							|${telegramUser.parseInfo.name}
							|${telegramUser.parseInfo.price}
							""".stripMargin()
					).parseMode(ParseMode.HTML)
					sendMessage(request, telegramUser.telegramId)
					sendGreetingMessage(telegramUser.telegramId)
				}
			}
			repository.save(telegramUser)
		} else if (!callbackDataStr.startsWith("del") && !callbackDataStr.startsWith("back")) {
			switch (State.valueOf(callbackDataStr)) {
				case State.LINK_ADDING -> {
					String text = "Пришли <u>ссылку</u> на товар"
					telegramUser.currentState = State.LINK_ADDING
					repository.save(telegramUser)
					editMessage(telegramUser.telegramId, telegramUser.lastBotMessageId, text)
				}

				case State.SHOW_ITEMS -> {
					ArrayList<String> items = checkerService.getAllFollowedProducts()
					String text = "<i><b>Твои записи:</b></i>\n\n"
					text += items.indexed().collect { index, item -> "${index + 1}. $item" }.join("\n")
					def buttons = items.indexed().collect { index, item ->
						new InlineKeyboardButton("❌ ${index + 1}").callbackData("del=${index + 1}")
					}
					def inlineKeyboard = keyboardBuilder(buttons, 3)
					inlineKeyboard.addRow(new InlineKeyboardButton("Назад").callbackData("back"))
					editMessage(telegramUser.telegramId, telegramUser.lastBotMessageId, text, inlineKeyboard)
				}

				default -> {
				}
			}
		} else if (callbackDataStr.startsWith("del")) {
			String[] callbackData = callbackDataStr.split("=")
			logger.debug(callbackData[-1]) /*тут должно быть удаление записи*/

		} else if (callbackDataStr.startsWith("back")) {
			deleteMessage(telegramUser.telegramId, telegramUser.lastBotMessageId)
			sendGreetingMessage(telegramUser.telegramId)

		}
	}

	private ExceptionHandler exceptionHandler = new ExceptionHandler() {
		@Override
		void onException(TelegramException e) {
			if (e.response() != null) {
				logger.error(e.response().errorCode().toString())
				logger.error(e.response().description())
			} else {
				logger.error(e.printStackTrace())
			}
		}
	}

//	private Callback<SendMessage, SendResponse> callback = new Callback<SendMessage, SendResponse>() {
//		@Override
//		void onResponse(SendMessage request, SendResponse response) {
//			logger.debug("callback: onResponse")
//		}
//
//		@Override
//		void onFailure(SendMessage request, IOException e) {
//			logger.debug("callback: onFailure")
//		}
//	}

	static InlineKeyboardMarkup keyboardBuilder(List<InlineKeyboardButton> buttons, Integer offset) {
		def inlineKeyboard = new InlineKeyboardMarkup()
		def row = []
		buttons.each { button ->
			row.add(button)
			if (row.size() == offset) {
				inlineKeyboard.addRow(row as InlineKeyboardButton[])
				row = []
			}
		}
		if (!row.isEmpty()) {
			inlineKeyboard.addRow(row as InlineKeyboardButton[])
		}
		return inlineKeyboard
	}

	void sendSearchMessage(TelegramUser telegramUser, Boolean isPrise = false) {
		if (isPrise) {
			telegramUser.searchMessageId = telegramUser.lastBotMessageId + 1
		} else {
			telegramUser.searchMessageId = telegramUser.lastBotMessageId
		}
		repository.save(telegramUser)
		editMessage(telegramUser.telegramId,
				telegramUser.searchMessageId, "Ищем...")
	}

	void sendGreetingMessage(Long telegramId) {
		String text = """
				|Привет это <i><u>PriceCheckerBot</u></i>
				|Этот бот помогает отследить <b>изменение цен</b> на интересующие вас товары
				""".stripMargin()
		SendMessage request = new SendMessage(telegramId, text)
				.parseMode(ParseMode.HTML)
				.replyMarkup(keyboardBuilder([
						new InlineKeyboardButton("Добавить item💎").callbackData(State.LINK_ADDING.name()),
						new InlineKeyboardButton("Посмотреть мои записи 🗒️").callbackData(State.SHOW_ITEMS.name()),
						new InlineKeyboardButton("Редактировать мои записи📝").callbackData("edit=???")
				], 1))
		sendMessage(request, telegramId)
	}

	void deleteMessage(Long telegramId, Integer messageId) {
		DeleteMessage deleteMessage = new DeleteMessage(telegramId, messageId)
		bot.execute(deleteMessage)
	}

	void editMessage(Long telegramId, Integer messageId, String text, InlineKeyboardMarkup keyboard = null) {
		EditMessageText editedMessage = new EditMessageText(telegramId, messageId, text)
				.parseMode(ParseMode.HTML)
		if (keyboard) {
			editedMessage.replyMarkup(keyboard)
		}
		bot.execute(editedMessage)
	}

	void sendMessage(SendMessage request, Long telegramId) {
		SendResponse response = bot.execute(request)
		try {
			TelegramUser telegramUser = repository.getByTelegramId(telegramId)
			telegramUser.lastBotMessageId = response.message().messageId()
			repository.save(telegramUser)
		} catch (Exception e) {
			logger.error(response?.toString() ?: e.message, e)
		}
	}

}
