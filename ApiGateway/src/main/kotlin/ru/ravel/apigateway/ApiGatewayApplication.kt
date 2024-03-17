package ru.ravel.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate


@SpringBootApplication
@EnableDiscoveryClient
class ApiGatewayApplication {
}

fun main(args: Array<String>) {
	runApplication<ApiGatewayApplication>(*args)
}
