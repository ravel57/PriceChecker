package ru.ravel.telegramservice.repository


import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.ravel.telegramservice.dto.TelegramUser

@Repository
interface TelegramUserRepository extends CrudRepository<TelegramUser, Long> {

	TelegramUser getByTelegramId(Long id);

}
