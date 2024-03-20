package ru.ravel.telegramservice.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.ravel.telegramservice.dto.TelegramUser

@Repository
interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

	TelegramUser getByTelegramId(Long id)

}
