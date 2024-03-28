package ru.ravel.telegramservice.service

import com.pengrad.telegrambot.ExceptionHandler
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.TelegramException
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.ravel.core.dto.ParseInfo
import ru.ravel.telegramservice.builder.MessageBuilder
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
									sendGreetingMessage(telegramUser)
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


	void messageAskToUser(String enterText, TelegramUser telegramUser, List<String> variables, State mode) {
		String vars = "$enterText\n\n"
		vars += variables.indexed()
				.collect { index, variable -> "<b>[${index + 1}]</b>\n$variable" }
				.join('\n\n')
		def buttons = variables.indexed().collect { index, item ->
			new InlineKeyboardButton(index + 1 as String).callbackData("$mode=$item")
		}
		new MessageBuilder(bot)
				.id(telegramUser.telegramId)
				.text(vars)
				.buttons(buttons)
				.keyboardOffset(2)
				.parseMode(ParseMode.HTML)
	}


	void stateHandler(TelegramUser telegramUser, String messageText) {
		String text = switch (telegramUser.currentState) {
			case State.LINK_ADDING -> {
				sendSearchMessage(telegramUser)
				telegramUser.parseInfo.url = messageText
				def result = checkerService.getProduct(telegramUser.parseInfo.url)
				if (result.isParserExist) {
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.messageId(telegramUser.lastBotMessageId)
							.method(MessageBuilder.Method.DELETE)
							.send()
					telegramUser.currentState = State.NONE

					def text = """
							|<b>Готово!</b>
							|<b><u>${result.parseInfoResult.name}</u></b>
							|<b><u>${result.parseInfoResult.price}</u></b>
							""".stripMargin()
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.text(text)
							.send()
					sendGreetingMessage(telegramUser)
					""
				} else {
					telegramUser.lastBotMessageId = new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.method(MessageBuilder.Method.SEND)
							.messageId(telegramUser.lastBotMessageId)
							.text("Пришли <u>ссылку</u> на товар\n<b><i>$messageText</i></b> - принято✅")
							.parseMode(ParseMode.HTML)
							.send()
					logger.debug(messageText) /*URL*/
					telegramUser.currentState = State.NAME_ADDING
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

			default -> ""
		}
		repository.save(telegramUser)
		if (telegramUser.currentState != State.NONE && text != null && text != "") {
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.id(telegramUser.telegramId)
					.method(MessageBuilder.Method.SEND)
					.text(text)
					.parseMode(ParseMode.HTML)
					.messageId(telegramUser.searchMessageId)
					.send()
		}
	}

	boolean nameClassAdding(TelegramUser telegramUser) {
		telegramUser.parseInfo = checkerService.postProductName(telegramUser.parseInfo).parseInfoResult
		if (telegramUser.parseInfo.nameClassAttributes.size() > 1) {
			messageAskToUser("Что больше похоже на class названия?",
					telegramUser,
					telegramUser.parseInfo.nameClassAttributes,
					State.CLASS_NAME_ART)
			return false
		} else if (telegramUser.parseInfo.nameClassAttributes.size() == 0) {
			telegramUser.currentState = State.NAME_ADDING
			new MessageBuilder(bot)
					.id(telegramUser.telegramId)
					.method(MessageBuilder.Method.EDIT)
					.messageId(telegramUser.searchMessageId)
					.text("Не найден❌")
					.messageId(telegramUser.searchMessageId)
					.send()
			def text = """
					|Такого элемента <b>не нашлось</b>😥
					|Проверьте <b><u>корректность отправленого названия</u></b> и попробуйте снова
					""".stripMargin()
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.id(telegramUser.telegramId)
					.method(MessageBuilder.Method.SEND)
					.text(text)
					.parseMode(ParseMode.HTML)
					.messageId(telegramUser.searchMessageId)
					.send()
			return false
		} else {
			return true
		}
	}

	boolean priceClassAdding(TelegramUser telegramUser) {
		telegramUser.parseInfo = checkerService.postProductPrice(telegramUser.parseInfo).parseInfoResult
		repository.save(telegramUser)
		if (telegramUser.parseInfo.priceClassAttributes.size() > 1) {
			messageAskToUser(
					"Что больше похоже на class цены?",
					telegramUser,
					telegramUser.parseInfo.priceClassAttributes,
					State.CLASS_PRICE_ART.name()
			)
			return false
		} else if (telegramUser.parseInfo.priceClassAttributes.size() == 0) {
			telegramUser.currentState = State.PRICE_ADDING
			new MessageBuilder(bot)
					.id(telegramUser.telegramId)
					.method(MessageBuilder.Method.EDIT)
					.text("Не найден❌")
					.messageId(telegramUser.searchMessageId)
					.send()
			String text = """
					|Такого элемента <b>не нашлось</b>😥
					|Проверьте <b><u>корректность отправленой цены</u></b>  и попробуйте снова
					""".stripMargin()
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.id(telegramUser.telegramId)
					.method(MessageBuilder.Method.SEND)
					.text(text)
					.parseMode(ParseMode.HTML)
					.send()
			telegramUser.currentState = State.LINK_ADDING
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
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.messageId(telegramUser.lastBotMessageId)
							.method(MessageBuilder.Method.DELETE)
							.send()
					telegramUser.parseInfo = checkerService.postNameClassAtr(telegramUser.parseInfo, callbackData[-1])
							.parseInfoResult
					telegramUser.currentState = State.PRICE_ADDING

					def text = """
							|Пришли <i><u>полное название</u></i> товара со страницы
							|<b><i>$telegramUser.parseInfo.name</i></b> - принято✅
							""".stripMargin()
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.text("<b><i>$telegramUser.parseInfo.price</i></b> - принято✅")
							.messageId(telegramUser.searchMessageId)
							.method(MessageBuilder.Method.EDIT)
							.text(text)
							.send()
					request = new SendMessage(telegramUser.telegramId, "Пришли <b>цену</b> этого товара")
							.parseMode(ParseMode.HTML)
					sendMessage(request, telegramUser.telegramId)
				}
				case State.CLASS_PRICE_ART -> {
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.messageId(telegramUser.lastBotMessageId)
							.method(MessageBuilder.Method.DELETE)
							.send()
					telegramUser.parseInfo = checkerService.postPriceClassAtr(telegramUser.parseInfo, callbackData[-1])
							.parseInfoResult
					telegramUser.currentState = State.NONE

					new MessageBuilder(bot)
							.method(MessageBuilder.Method.EDIT)
							.id(telegramUser.telegramId)
							.text("<b><i>$telegramUser.parseInfo.price</i></b> - принято✅")
							.parseMode(ParseMode.HTML)
							.messageId(telegramUser.searchMessageId)
							.send()
					telegramUser.lastBotMessageId = new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.text("""
									|<b>Настройка персера завершена!</b>
									|${telegramUser.parseInfo.name}
									|${telegramUser.parseInfo.price}
									""".stripMargin())
							.method(MessageBuilder.Method.SEND)
							.parseMode(ParseMode.HTML)
							.send()
					telegramUser.currentState = State.LINK_ADDING
					sendGreetingMessage(telegramUser)
				}
			}
			repository.save(telegramUser)
		} else if (!callbackDataStr.startsWith("del") && !callbackDataStr.startsWith("back")) {
			switch (State.valueOf(callbackDataStr)) {
				case State.LINK_ADDING -> {
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.messageId(telegramUser.lastBotMessageId)
							.method(MessageBuilder.Method.EDIT)
							.text("Пришли <u>ссылку</u> на товар")
							.parseMode(ParseMode.HTML)
							.send()
					telegramUser.currentState = State.LINK_ADDING
					repository.save(telegramUser)
				}

				case State.SHOW_ITEMS -> {
					ArrayList<String> items = checkerService.getAllFollowedProducts()
					StringBuilder text = new StringBuilder("<i><b>Твои записи:</b></i>\n\n")
							.append(items.indexed().collect { index, item -> "${index + 1}. $item" }.join("\n"))
					new MessageBuilder(bot)
							.id(telegramUser.telegramId)
							.messageId(telegramUser.lastBotMessageId)
							.method(MessageBuilder.Method.EDIT)
							.text(text.toString())
							.parseMode(ParseMode.HTML)
							.buttons(items.indexed().collect { index, item ->
								new InlineKeyboardButton("❌ ${index + 1}").callbackData("del=${index + 1}")
							})
							.addBackButton("назад")
							.keyboardOffset(3)
							.send()
				}

				default -> {
				}
			}
		} else if (callbackDataStr.startsWith("del")) {
			String[] callbackData = callbackDataStr.split("=")
			logger.debug(callbackData[-1]) /*тут должно быть удаление записи*/

		} else if (callbackDataStr.startsWith("back")) {
			new MessageBuilder(bot)
					.id(telegramUser.telegramId)
					.messageId(telegramUser.lastBotMessageId)
					.method(MessageBuilder.Method.DELETE)
					.send()
			sendGreetingMessage(telegramUser)
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

	void sendSearchMessage(TelegramUser telegramUser, Boolean isPrise = false) {
		if (isPrise) {
			telegramUser.searchMessageId = telegramUser.lastBotMessageId + 1
		} else {
			telegramUser.searchMessageId = telegramUser.lastBotMessageId
		}
		telegramUser.searchMessageId = new MessageBuilder(bot)
				.id(telegramUser.telegramId)
				.messageId(telegramUser.searchMessageId)
				.method(MessageBuilder.Method.EDIT)
				.text("Ищем...")
				.send()
		repository.save(telegramUser)
	}

	void sendGreetingMessage(TelegramUser telegramUser) {
		String text = """
				|Привет это <i><u>PriceCheckerBot</u></i>
				|Этот бот помогает отследить <b>изменение цен</b> на интересующие вас товары
				""".stripMargin()
		telegramUser.lastBotMessageId = new MessageBuilder(bot)
				.id(telegramUser.telegramId)
				.text(text)
				.method(MessageBuilder.Method.SEND)
				.parseMode(ParseMode.HTML)
				.buttons(new InlineKeyboardButton("Добавить item💎").callbackData(State.LINK_ADDING.name()),
						new InlineKeyboardButton("Посмотреть мои записи 🗒️").callbackData(State.SHOW_ITEMS.name()),
						new InlineKeyboardButton("Редактировать мои записи📝").callbackData("edit=???"))
				.keyboardOffset(1)
				.send()
	}

}
