package ru.ravel.webparser.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

	@Bean
	fun createTopic(): NewTopic {
		return TopicBuilder.name("product-price-changed-event")
			.partitions(1)
			.replicas(1)
			.build()
	}
	
}