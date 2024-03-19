package ru.ravel.telegramservice.service

import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.ravel.telegramservice.dto.User
import ru.ravel.telegramservice.repository.TelegramRepository

@Service
class TelegramService {

	private Logger logger = LoggerFactory.getLogger(this.class)

	private TelegramBot bot = new TelegramBot(System.getenv("bot_token"))

	@Autowired
	private TelegramRepository repository

	TelegramService() {
		bot.setUpdatesListener(listener, exceptionHandler)
	}


	private UpdatesListener listener = new UpdatesListener() {
		@Override
		int process(List<Update> updates) {
			updates.each {
				Long telegramId = it.message().from().id()
				String username = it.message().from().username()
				User user = repository.getByTelegramId(telegramId)
				if (user != null) {
					user
				} else {
					repository.save(new User(telegramId, username))
				}
			}
			return UpdatesListener.CONFIRMED_UPDATES_ALL;
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

	void sendMessage(int chatId, String text) {
		SendMessage request = new SendMessage(chatId, text).parseMode(ParseMode.HTML)
		bot.execute(request, callback)
	}

}
