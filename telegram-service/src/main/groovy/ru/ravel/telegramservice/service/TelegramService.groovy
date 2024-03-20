package ru.ravel.telegramservice.service

import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
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
					if (message.startsWith('/')) {
						switch (Command.getByCommand(message)) {
							case Command.START -> {
								sendGreetingMessage(telegramId)
							}
						}
					} else {
						switch (telegramUser.currentState) {
							case State.LINK_ADDING -> {
								PriceCheckerService.Result info = checkerService.getInfo(message)
								if (!info.isHavingParser) {
									String text = "парсер нужо настроить пришли полное название товара со страницы"
									SendMessage request = new SendMessage(telegramId, text)
											.parseMode(ParseMode.HTML)
									sendMessage(request)
								} else {
									String text = "Ок готово"
									SendMessage request = new SendMessage(telegramId, text)
											.parseMode(ParseMode.HTML)
									sendMessage(request)
								}
							}
							case State.NONE -> {
							}

							case State.NAME_ADDING -> {
							}

							case State.PRICE_ADDING -> {
							}
						}
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

	void sendGreetingMessage(Long telegramId) {
		String text = """
				|Привет это <i>PriceCheckerBot</i>
				|Этот бот помогает отследить <b>изменение цен</b> на интересующие вас товары
				""".stripMargin()
		SendMessage request = new SendMessage(telegramId, text)
				.parseMode(ParseMode.HTML)
				.replyMarkup(new InlineKeyboardMarkup([
						new InlineKeyboardButton("Добавить item💎").callbackData(State.LINK_ADDING.name())
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

			case State.NONE -> {
			}

			case State.NAME_ADDING -> {
			}

			case State.PRICE_ADDING -> {
			}

			default -> {
			}
		}
	}

	void sendMessage(SendMessage request) {
		bot.execute(request)
	}

}
