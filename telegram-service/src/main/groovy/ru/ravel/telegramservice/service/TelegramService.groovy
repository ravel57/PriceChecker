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
			new InlineKeyboardButton(index + 1 as String).callbackData("$mode=$index")
		}
		telegramUser.lastBotMessageId = new MessageBuilder(bot)
				.send()
				.telegramId(telegramUser.telegramId)
				.text(vars)
				.buttons(2, buttons)
				.parseMode(ParseMode.HTML)
				.execute()
	}


	void linkAddingHandler(TelegramUser telegramUser, String url) {
		sendSearchMessage(telegramUser)
		telegramUser.parseInfo.url = url
		def result = checkerService.getProduct(telegramUser.parseInfo.url)
		if (result.isParserExist) {
			new MessageBuilder(bot)
					.delete()
					.telegramId(telegramUser.telegramId)
					.messageId(telegramUser.lastBotMessageId)
					.execute()
			telegramUser.currentState = State.NONE

			def text = """
							|<b>Готово!</b>🥳
							|<u>Название</u>: <b>${result.parseInfoResult.name}</b>
							|<u>Цена</u>: <b><u>${result.parseInfoResult.price}</u></b>
							""".stripMargin()
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.send()
					.parseMode(ParseMode.HTML)
					.telegramId(telegramUser.telegramId)
					.text(text)
					.execute()
			sendGreetingMessage(telegramUser)
		} else {
			new MessageBuilder(bot)
					.edit()
					.telegramId(telegramUser.telegramId)
					.messageId(telegramUser.lastBotMessageId)
					.text("Пришли <u>ссылку</u> на товар\n<b><i>$url</i></b> - принято✅")
					.parseMode(ParseMode.HTML)
					.execute()
			telegramUser.currentState = State.NAME_ADDING
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.send()
					.telegramId(telegramUser.telegramId)
					.text("<b>Парсер нужо настроить</b>\nПришли <i><u>полное название</u></i> товара со страницы")
					.parseMode(ParseMode.HTML)
					.execute()
		}
	}


	void nameAddingHandler(TelegramUser telegramUser, String messageText) {
		telegramUser.parseInfo.name = messageText
		sendSearchMessage(telegramUser)
		telegramUser.parseInfo = checkerService.postProductName(telegramUser.parseInfo).parseInfoResult
		repository.save(telegramUser)
		if (telegramUser.parseInfo.nameClassAttributes.size() > 0) {
			messageAskToUser("Что больше похоже на class названия?",
					telegramUser,
					telegramUser.parseInfo.nameClassAttributes,
					State.CLASS_NAME_ART_ADDING)
		} else if (telegramUser.parseInfo.nameClassAttributes.size() > 0) {
			telegramUser.currentState = State.NAME_ADDING
			new MessageBuilder(bot)
					.edit()
					.telegramId(telegramUser.telegramId)
					.messageId(telegramUser.searchMessageId)
					.text("Не найден❌")
					.messageId(telegramUser.searchMessageId)
					.execute()
			def text = """
					|Такого элемента <b>не нашлось</b>😥
					|Проверьте <b><u>корректность отправленого названия</u></b> и попробуйте снова
					""".stripMargin()
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.send()
					.telegramId(telegramUser.telegramId)
					.text(text)
					.parseMode(ParseMode.HTML)
					.execute()
			repository.save(telegramUser)
		}
	}


	void priceAddingHandler(TelegramUser telegramUser, String messageText) {
		telegramUser.parseInfo.price = messageText
		sendSearchMessage(telegramUser)
		telegramUser.parseInfo = checkerService.postProductPrice(telegramUser.parseInfo).parseInfoResult
		repository.save(telegramUser)
		if (telegramUser.parseInfo.priceClassAttributes.size() > 0) {
			messageAskToUser(
					"Что больше похоже на class цены?",
					telegramUser,
					telegramUser.parseInfo.priceClassAttributes,
					State.CLASS_PRICE_ART_ADDING
			)
		} else if (telegramUser.parseInfo.priceClassAttributes.size() == 0) {
			telegramUser.currentState = State.PRICE_ADDING
			new MessageBuilder(bot)
					.edit()
					.telegramId(telegramUser.telegramId)
					.text("Не найден❌")
					.messageId(telegramUser.searchMessageId)
					.execute()
			String text = """
					|Такого элемента <b>не нашлось</b>😥
					|Проверьте <b><u>корректность отправленой цены</u></b>  и попробуйте снова
					""".stripMargin()
			telegramUser.lastBotMessageId = new MessageBuilder(bot)
					.send()
					.telegramId(telegramUser.telegramId)
					.text(text)
					.parseMode(ParseMode.HTML)
					.execute()
			telegramUser.currentState = State.LINK_ADDING
			repository.save(telegramUser)
		}
	}


	void stateHandler(TelegramUser telegramUser, String messageText) {
		switch (telegramUser.currentState) {
			case State.LINK_ADDING -> linkAddingHandler(telegramUser, messageText)
			case State.NAME_ADDING -> nameAddingHandler(telegramUser, messageText)
			case State.PRICE_ADDING -> priceAddingHandler(telegramUser, messageText)
		}
		repository.save(telegramUser)
	}

	void classNameArtHandler(TelegramUser telegramUser, String[] callbackData) {
		new MessageBuilder(bot)
				.delete()
				.telegramId(telegramUser.telegramId)
				.messageId(telegramUser.lastBotMessageId)
				.execute()

		String nameClassAttribute = telegramUser.parseInfo.nameClassAttributes[callbackData[-1].toInteger()]
		telegramUser.parseInfo = checkerService.postNameClassAtr(telegramUser.parseInfo, nameClassAttribute)
				.parseInfoResult
		telegramUser.currentState = State.PRICE_ADDING

		def text = """
							|Пришли <i><u>полное название</u></i> товара со страницы
							|<b><i>$telegramUser.parseInfo.name</i></b> - принято✅
							""".stripMargin()
		new MessageBuilder(bot)
				.edit()
				.telegramId(telegramUser.telegramId)
				.parseMode(ParseMode.HTML)
				.text(text)
				.messageId(telegramUser.searchMessageId)
				.execute()
		telegramUser.lastBotMessageId = new MessageBuilder(bot)
				.send()
				.telegramId(telegramUser.telegramId)
				.text("Пришли <b>цену</b> этого товара")
				.parseMode(ParseMode.HTML)
				.execute()
	}

	void classPriceArtHandler(TelegramUser telegramUser, String[] callbackData){
		new MessageBuilder(bot)
				.delete()
				.telegramId(telegramUser.telegramId)
				.messageId(telegramUser.lastBotMessageId)
				.execute()
		String priceClassAttribute = telegramUser.parseInfo.priceClassAttributes[callbackData[-1].toInteger()]
		telegramUser.parseInfo = checkerService.postPriceClassAtr(telegramUser.parseInfo, priceClassAttribute)
				.parseInfoResult
		telegramUser.currentState = State.NONE

		new MessageBuilder(bot)
				.edit()
				.telegramId(telegramUser.telegramId)
				.text("<b><i>$telegramUser.parseInfo.price</i></b> - принято✅")
				.parseMode(ParseMode.HTML)
				.messageId(telegramUser.searchMessageId)
				.execute()
		telegramUser.lastBotMessageId = new MessageBuilder(bot)
				.send()
				.telegramId(telegramUser.telegramId)
				.text("<b>Настройка персера завершена!</b>")
				.parseMode(ParseMode.HTML)
				.execute()
		telegramUser.currentState = State.LINK_ADDING
		linkAddingHandler(telegramUser, telegramUser.parseInfo.url)
	}

	void callbackShowItemHandler(TelegramUser telegramUser){
		ArrayList<String> items = checkerService.getAllFollowedProducts()
		StringBuilder text = new StringBuilder("<i><b>Твои записи:</b></i>\n\n")
				.append(items.indexed().collect { index, item -> "${index + 1}. $item" }.join("\n"))
		new MessageBuilder(bot)
				.edit()
				.telegramId(telegramUser.telegramId)
				.messageId(telegramUser.lastBotMessageId)
				.text(text.toString())
				.parseMode(ParseMode.HTML)
				.buttons(3, items.indexed().collect { index, item ->
					new InlineKeyboardButton("❌ ${index + 1}").callbackData("del=${index + 1}")
				})
				.addBackButton("назад")
				.execute()
	}

	void callbackLinkAddingHandler(TelegramUser telegramUser) {
		new MessageBuilder(bot)
				.edit()
				.telegramId(telegramUser.telegramId)
				.messageId(telegramUser.lastBotMessageId)
				.text("Пришли <u>ссылку</u> на товар")
				.parseMode(ParseMode.HTML)
				.execute()
		telegramUser.currentState = State.LINK_ADDING
		repository.save(telegramUser)
	}


	private void callbackHandler(Update update, TelegramUser telegramUser) {
		String callbackDataStr = update.callbackQuery().data()
		telegramUser.lastBotMessageId = update.callbackQuery().message().messageId()
		repository.save(telegramUser)
		if (callbackDataStr.startsWith("CLASS")) {
			SendMessage request
			String[] callbackData = callbackDataStr.split("=")
			switch (State.valueOf(callbackData[0])) {
				case State.CLASS_NAME_ART_ADDING -> classNameArtHandler(telegramUser, callbackData)
				case State.CLASS_PRICE_ART_ADDING -> classPriceArtHandler(telegramUser, callbackData)
			}
			repository.save(telegramUser)
		} else if (!callbackDataStr.startsWith("del") && !callbackDataStr.startsWith("back")) {
			switch (State.valueOf(callbackDataStr)) {
				case State.LINK_ADDING -> callbackLinkAddingHandler(telegramUser)
				case State.SHOW_ITEMS -> callbackShowItemHandler(telegramUser)
			}
		} else if (callbackDataStr.startsWith("del")) {
			String[] callbackData = callbackDataStr.split("=")
			logger.debug(callbackData[-1]) /*тут должно быть удаление записи*/
		} else if (callbackDataStr.startsWith("back")) {
			new MessageBuilder(bot)
					.delete()
					.telegramId(telegramUser.telegramId)
					.messageId(telegramUser.lastBotMessageId)
					.execute()
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

	void sendSearchMessage(TelegramUser telegramUser) {
		telegramUser.searchMessageId = telegramUser.lastBotMessageId
		new MessageBuilder(bot)
				.edit()
				.telegramId(telegramUser.telegramId)
				.messageId(telegramUser.searchMessageId)
				.text("Ищем...")
				.execute()
		repository.save(telegramUser)
	}

	void sendGreetingMessage(TelegramUser telegramUser) {
		String text = """
				|Привет это <i><u>PriceCheckerBot</u></i>
				|Этот бот помогает отследить <b>изменение цен</b> на интересующие вас товары
				""".stripMargin()
		telegramUser.lastBotMessageId = new MessageBuilder(bot)
				.send()
				.telegramId(telegramUser.telegramId)
				.text(text)
				.parseMode(ParseMode.HTML)
				.buttons(1,
						new InlineKeyboardButton("Добавить item💎").callbackData(State.LINK_ADDING.name()),
						new InlineKeyboardButton("Посмотреть мои записи 🗒️").callbackData(State.SHOW_ITEMS.name()),
						new InlineKeyboardButton("Редактировать мои записи📝").callbackData("edit=???"))
				.execute()
	}

}
