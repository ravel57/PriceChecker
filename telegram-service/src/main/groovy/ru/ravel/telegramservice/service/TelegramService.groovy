package ru.ravel.telegramservice.service

import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ChatAction
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.ravel.telegramservice.dto.Command
import ru.ravel.telegramservice.dto.State
import ru.ravel.telegramservice.dto.TelegramUser
import ru.ravel.telegramservice.repository.TelegramUserRepository

@Service
class TelegramService {

	private Logger logger = LoggerFactory.getLogger(this.class)

	private TelegramBot bot = new TelegramBot(System.getenv("bot_token"))

	@Autowired
	private TelegramUserRepository repository

	@Autowired
	private PriceCheckerService checkerService

	TelegramService() {
		bot.setUpdatesListener(listener, exceptionHandler)
	}


	private UpdatesListener listener = new UpdatesListener() {

		@Override
		int process(List<Update> updates) {
			updates.each { update ->
				Long telegramId
				TelegramUser telegramUser

				if (update?.message()) {
					telegramId = update.message().from().id()
					telegramUser = repository.getByTelegramId(telegramId)

					if (telegramUser == null) {
						telegramUser = repository.save(new TelegramUser(telegramId))
					}
					String message = update.message().text()

					if (update?.message()?.text() && message.startsWith('/')) {
						switch (Command.getByCommand(message)) {
							case Command.START -> {
								sendGreetingMessage(telegramId)
							}
						}
					} else {
						stateWorker(telegramId, message)
					}
				} else if (update?.callbackQuery()) {
					telegramId = update.callbackQuery().from().id()
					telegramUser = repository.getByTelegramId(telegramId)
					handleCallback(update, telegramUser)
				}
			}
			return UpdatesListener.CONFIRMED_UPDATES_ALL
		}

	}


	void stateWorker(Long telegramId, String message) {
		TelegramUser telegramUser
		telegramUser = repository.getByTelegramId(telegramId)
		String text = switch (telegramUser.currentState) {
			case State.LINK_ADDING -> {
				PriceCheckerService.Result info = checkerService.getInfo(message)
				if (!info.isHavingParser) {
					telegramUser.currentState = State.NAME_ADDING
					logger.debug(message) /*URL*/
					"Парсер нужо настроить, пришли <b>полное название товара со страницы</b>"
				} else {
					deleteMessage(telegramId, telegramUser.lastBotMessageId)
					telegramUser.currentState = State.NONE
					SendMessage request = new SendMessage(telegramId, "Все готово")
					sendMessage(request, telegramId)
					sendGreetingMessage(telegramId)
					""
				}
			}

			case State.NAME_ADDING -> {
				telegramUser.currentState = State.PRICE_ADDING
				logger.debug(message) /*Name*/
				"<b><i>$message</i></b> - принято.\nНапиши цену этого товара сейчас"
			}

			case State.PRICE_ADDING -> {
				deleteMessage(telegramId, telegramUser.lastBotMessageId)
				telegramUser.currentState = State.NONE
				logger.debug(message) /*Price*/
				sendGreetingMessage(telegramId)
				""
			}

			case State.SHOW_ITEMS -> {
				""
			}

			case State.NONE -> {
				"Ниче не понял" /*Заглушка?*/
			}
		}
		repository.save(telegramUser)
		if (telegramUser.currentState != State.NONE) {
			editMessage(telegramId, telegramUser.lastBotMessageId, text)
		}
	}


	void sendGreetingMessage(Long telegramId) {
		String text = """
				|Привет это <i>PriceCheckerBot</i>
				|Этот бот помогает отследить <b>изменение цен</b> на интересующие вас товары
				""".stripMargin()
		SendMessage request = new SendMessage(telegramId, text)
				.parseMode(ParseMode.HTML)
				.replyMarkup(new InlineKeyboardMarkup([
						new InlineKeyboardButton("Добавить item💎").callbackData(State.LINK_ADDING.name()),
						new InlineKeyboardButton("Посмотреть мои записи").callbackData(State.SHOW_ITEMS.name())
				] as InlineKeyboardButton[]))
		sendMessage(request, telegramId)
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

	private Callback<SendMessage, SendResponse> callback = new Callback<SendMessage, SendResponse>() {
		@Override
		void onResponse(SendMessage request, SendResponse response) {
			logger.debug("callback: onResponse")
		}

		@Override
		void onFailure(SendMessage request, IOException e) {
			logger.debug("callback: onFailure")
		}
	}

	private void handleCallback(Update update, TelegramUser telegramUser) {
		String callData = update.callbackQuery().data()
		telegramUser.callbackQueryId = update.callbackQuery().id()
		if (!callData.startsWith("del")) {
			switch (State.valueOf(callData)) {
				case State.LINK_ADDING -> {
					String text = "Пришли ссылку на товар"
					telegramUser.currentState = State.LINK_ADDING
					repository.save(telegramUser)
					editMessage(telegramUser.telegramId, telegramUser.lastBotMessageId, text)
				}

				case State.SHOW_ITEMS -> {
					ArrayList<String> items = ["iPhone 11 Pro", "Samsung S24 Ultra", "Казантип 2009",
											   "Завод по производству алюминиевых ведер", "Казахи", "Нагетсы", "Шины 12``"]
					String text = "<i><b>Твои записи:</b></i>\n\n"
					for (item in items) {
						Integer itemCount = items.indexOf(item) + 1
						text += "$itemCount. $item\n"
					}
					editMessage(telegramUser.telegramId, telegramUser.lastBotMessageId, text, keyboardGenerator(items))
				}

				default -> {
				}
			}
		} else if (callData.startsWith("del")) {
			logger.debug(callData.split('=')[-1]) /*тут должно быть удаление записи*/
		}
	}

	static InlineKeyboardMarkup keyboardGenerator(ArrayList<String> items) {
		def buttons = items.collect { item ->
			new InlineKeyboardButton("❌ ${items.indexOf(item) + 1}")
					.callbackData("del=${items.indexOf(item) + 1}")
		}
		def inlineKeyboard = new InlineKeyboardMarkup()
		def row = []
		buttons.each { button ->
			row.add(button)
			if (row.size() == 3) {
				inlineKeyboard.addRow(row as InlineKeyboardButton[])
				row = []
			}
		}
		if (!row.isEmpty()) {
			inlineKeyboard.addRow(row as InlineKeyboardButton[])
		}
		return inlineKeyboard
	}

	void deleteMessage(Long telegramId, Integer messageId) {
		DeleteMessage deleteMessage = new DeleteMessage(telegramId, messageId)
		bot.execute(deleteMessage)
	}

	void editMessage(
			Long telegramId,
			Integer messageId,
			String text,
			InlineKeyboardMarkup keyboard = null
	) {
		EditMessageText editedMessage = new EditMessageText(telegramId, messageId, text)
				.parseMode(ParseMode.HTML)
		if (keyboard) {
			editedMessage.replyMarkup(keyboard)
		}
		bot.execute(editedMessage)
	}

//	Это тот самый popup alert (только вот заюзать я его не смог)
	void showAlert(TelegramUser telegramUser) {
		AnswerCallbackQuery request = new AnswerCallbackQuery(telegramUser.callbackQueryId)
				.text("Настройка завершена")
				.showAlert(true)
		bot.execute(request)
	}

	void sendMessage(SendMessage request, Long telegramId) {
		SendResponse response = bot.execute(request)
		TelegramUser telegramUser
		telegramUser = repository.getByTelegramId(telegramId)
		telegramUser.lastBotMessageId = response.message().messageId()
		repository.save(telegramUser)
	}

}
