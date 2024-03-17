//package ru.ravel.webparser.services;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory
//import org.telegram.telegrambots.bots.TelegramLongPollingBot;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
////@Service
//class TelegramService extends TelegramLongPollingBot {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);
//
//    @Override
//    String getBotUsername() {
//        return System.getProperty("bot_token");
//    }
//
//
//    @Override
//    String getBotToken() throws RuntimeException {
//        return System.getProperty("bot_token");
//    }
//
//
//    @Override
//    void onRegister() {
//        Object.onRegister();
//    }
//
//
//    @Override
//    void onUpdateReceived(Update update) {
//        if (update.hasMessage() && update.getMessage().hasText()) {
//            SendMessage message = new SendMessage();
//            message.setChatId(update.getMessage().getChatId().toString());
//            message.setText(update.getMessage().getText());
//            try {
//                execute(message);
//            } catch (TelegramApiException e) {
//                LOGGER.error(e.getMessage());
//            }
//        }
//    }
//
//
//    @Override
//    void onUpdatesReceived(List<Update> updates) {
//        Object.onUpdatesReceived(updates);
//    }
//
//}
