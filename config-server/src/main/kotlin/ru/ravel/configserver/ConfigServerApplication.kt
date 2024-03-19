package ru.ravel.configserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.config.server.EnableConfigServer

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigServer
class ConfigServerApplication

fun main(args: Array<String>) {
	runApplication<ConfigServerApplication>(*args)
}
