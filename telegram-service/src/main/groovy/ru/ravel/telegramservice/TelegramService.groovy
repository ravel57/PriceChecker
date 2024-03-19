package ru.ravel.telegramservice

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.ExceptionHandler
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.TelegramException
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TelegramService {

	private Logger logger = LoggerFactory.getLogger(this.class)

	private TelegramBot bot = new TelegramBot(System.getenv("bot_token"))

	private UpdatesListener listener = new UpdatesListener() {
		@Override
		int process(List<Update> updates) {
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
			println()
		}

		@Override
		void onFailure(SendMessage request, IOException e) {
			println()
		}
	}

	TelegramService() {
		bot.setUpdatesListener(listener, exceptionHandler)
	}

	void sendMessage(int chatId, String text){
		SendMessage request = new SendMessage(chatId, text).parseMode(ParseMode.HTML)
		bot.execute(request, callback)
	}

}
