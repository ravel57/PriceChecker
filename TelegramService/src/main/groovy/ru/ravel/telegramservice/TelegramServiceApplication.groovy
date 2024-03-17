package ru.ravel.telegramservice

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class TelegramServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(TelegramServiceApplication, args)
	}

}
