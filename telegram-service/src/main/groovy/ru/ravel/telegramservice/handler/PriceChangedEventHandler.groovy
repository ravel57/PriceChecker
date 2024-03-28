//package ru.ravel.telegramservice.handler
//
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.kafka.annotation.KafkaHandler
//import org.springframework.kafka.annotation.KafkaListener
//import org.springframework.stereotype.Component
//import ru.ravel.core.event.PriceChangedEvent
//
//@Component
//@KafkaListener(topics = ["product-price-changed-event"])
//class PriceChangedEventHandler {
//
//	Logger logger = LoggerFactory.getLogger(this.class)
//
//	@KafkaHandler
//	void handle(PriceChangedEvent event) {
//		logger.debug(event.toString())
//	}
//}