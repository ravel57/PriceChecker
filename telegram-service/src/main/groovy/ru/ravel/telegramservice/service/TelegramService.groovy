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
import ru.ravel.telegramservice.dto.State
import ru.ravel.telegramservice.dto.TelegramUser
import ru.ravel.telegramservice.repository.TelegramUserRepository

@Service
class TelegramService {

	private Logger logger = LoggerFactory.getLogger(this.class)

	private TelegramBot bot = new TelegramBot(System.getenv("bot_token"))

	@Autowired
	private TelegramUserRepository repository

	TelegramService() {
		bot.setUpdatesListener(listener, exceptionHandler)
	}


	private UpdatesListener listener = new UpdatesListener() {
		@Override
		int process(List<Update> updates) {
			updates.each {
				if (it.message()) {
					Long telegramId = it.message().from().id()
					String username = it.message().from().username()
					TelegramUser user = repository.getByTelegramId(telegramId)
					if (user == null) {
						repository.save(new TelegramUser(telegramId, username))
					}
					SendMessage request = new SendMessage(telegramId, """
							|Привет это <i>PriceCheckerBot</i>
							|Этот бот помогает отследить <b>изменение цен</b> на
							|интересующие вас товары
							""".stripMargin())
							.parseMode(ParseMode.HTML)
							.disableWebPagePreview(true)
							.disableNotification(true)
							.replyToMessageId(1)
							.replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton[]{
									new InlineKeyboardButton("Добавить item💎")
											.callbackData(State.LINK_ADDING.name())
							}))
					sendMessage(request)
					user.currentState = State.LINK_ADDING
					repository.save(user)
				}
				if (updates[0]?.callbackQuery()?.data()) {
					handleCallback(it, repository.getByTelegramId(it.callbackQuery().from().id()))
				}
			}
			return UpdatesListener.CONFIRMED_UPDATES_ALL
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

	private void handleCallback(Update update, TelegramUser user) {
		String callData = update.callbackQuery().data()
		Long chatId = update.callbackQuery().message().chat().id()
		switch (State.valueOf(callData)) {
			case State.LINK_ADDING -> {
				SendMessage request = new SendMessage(chatId, "Пришли ссылку на товар")
						.parseMode(ParseMode.HTML)
						.disableWebPagePreview(true)
						.disableNotification(true)
						.replyToMessageId(1)
				sendMessage(request)
				user.currentState = State.NONE
				repository.save(user)
			}
			default -> {
			}
		}
	}

	void sendMessage(SendMessage request) {
		bot.execute(request)
	}

}
