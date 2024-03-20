package ru.ravel.telegramservice.service

import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove
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
						sendMessage(StateWorker(telegramId, message))
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


	SendMessage StateWorker(Long telegramId, String message) {
		TelegramUser telegramUser
		telegramUser = repository.getByTelegramId(telegramId)
		String text
		switch (telegramUser.currentState) {
			case State.LINK_ADDING -> {
				PriceCheckerService.Result info = checkerService.getInfo(message)
				if (!info.isHavingParser) {
					text = "Парсер нужо настроить, пришли <b>полное название товара со страницы</b>"
					telegramUser.currentState = State.NAME_ADDING
					println(message) /*URL*/
				} else {
					text = "Ок готово"
					telegramUser.currentState = State.NONE
				}
			}

			case State.NAME_ADDING -> {
				text = "<b><i>$message</i></b> - принято.\nНапиши цену этого товара сейчас"
				telegramUser.currentState = State.PRICE_ADDING
				println(message) /*Name*/
			}

			case State.PRICE_ADDING -> {
				text = "Принято"
				telegramUser.currentState = State.NONE
				println(message) /*Price*/
			}
			case State.NONE -> {
				text = "Ниче не понял" /*Заглушка?*/
			}
		}
		SendMessage request = new SendMessage(telegramId, text)
				.parseMode(ParseMode.HTML)
		repository.save(telegramUser)
		return request
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
		sendMessage(request)
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
		switch (State.valueOf(callData)) {
			case State.LINK_ADDING -> {
				SendMessage request = new SendMessage(telegramUser.telegramId, "Пришли ссылку на товар")
						.parseMode(ParseMode.HTML)

				telegramUser.currentState = State.LINK_ADDING
				repository.save(telegramUser)
				sendMessage(request)
			}
			case State.SHOW_ITEMS -> {
				ArrayList<String> items
				items = ["iPhone 11 Pro", "Samsung S24 Ultra", "Казантип 2009"]
				String text
				text = "<i><b>Твои записи:</b></i>\n\n"
				for (item in items){
					text += item + "\n"
				}
				SendMessage request = new SendMessage(telegramUser.telegramId, text)
						.parseMode(ParseMode.HTML)
				sendMessage(request)
			}
			default -> {
			}
		}
	}

	void editMessage(Long telegramId, Integer messageId, String text) {
		EditMessageText editedMessage = new EditMessageText(telegramId, messageId, text)
				.parseMode(ParseMode.HTML)
		bot.execute(editedMessage)
	}

	void sendMessage(SendMessage request) {
		bot.execute(request)
	}

}
