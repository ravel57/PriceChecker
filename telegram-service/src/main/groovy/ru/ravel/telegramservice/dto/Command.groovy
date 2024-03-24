package ru.ravel.telegramservice.dto

enum Command {
	START("/start")

	private String command

	Command(String command) {
		this.command = command
	}

	static Command getByCommand(String command) {
		return values().find { it.command == command }
	}
}
