package ru.ravel.telegramservice

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.ExceptionHandler
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.TelegramException
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ChatAction
import com.pengrad.telegrambot.model.request.ForceReply
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import org.springframework.stereotype.Service

@Service
class TelegramService {

	TelegramBot bot = new TelegramBot(System.getenv("bot_token"))

	UpdatesListener listener = updates -> {
		return UpdatesListener.CONFIRMED_UPDATES_ALL
	}

	ExceptionHandler exceptionHandler = e -> {
		if (e.response() != null) {
			e.response().errorCode()
			e.response().description()
		} else {
			e.printStackTrace()
		}
	}

	Callback<SendMessage, SendResponse> callback = new Callback<SendMessage, SendResponse>() {
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
		SendMessage request = new SendMessage(315538424, "text")
				.parseMode(ParseMode.HTML)
				.disableWebPagePreview(true)
				.disableNotification(true)
				.replyToMessageId(1)
				.replyMarkup(new ReplyKeyboardMarkup(['a', 'b'] as String[], ['c', 'd'] as String[]))
//		bot.execute(request, callback)
	}

}
