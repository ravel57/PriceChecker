package ru.ravel.telegramservice.dto

enum Command {
	START("/start")

	private String text

	Command(String text) {
		this.text = text
	}

	static Command getByText(String command) {
		return values().find { it.text == command }
	}
}
