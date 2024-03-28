//package ru.ravel.telegramservice.config
//
//import org.apache.kafka.clients.consumer.ConsumerConfig
//import org.apache.kafka.common.serialization.LongDeserializer
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.core.env.Environment
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
//import org.springframework.kafka.core.ConsumerFactory
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory
//import org.springframework.kafka.support.serializer.JsonDeserializer
//
//@Configuration
//class KafkaConfig {
//
//	@Autowired
//	Environment environment
//
//	@Bean
//	ConsumerFactory<String, Object> consumerFactory() {
//		Map<String, Object> config = new HashMap<>()
//		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
//		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
//		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class)
//		config.put(JsonDeserializer.TRUSTED_PACKAGES, "ru.ravel.core.event")
//		config.put(ConsumerConfig.GROUP_ID_CONFIG, "product-price-changed-event")
//		return new DefaultKafkaConsumerFactory<String, Object>(config)
//	}
//
//	@Bean
//	ConcurrentKafkaListenerContainerFactory<String, Object> factory(ConsumerFactory<String, Object> consumerFactory) {
//		ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>()
//		factory.setConsumerFactory(consumerFactory)
//		return factory
//	}
//
//}
