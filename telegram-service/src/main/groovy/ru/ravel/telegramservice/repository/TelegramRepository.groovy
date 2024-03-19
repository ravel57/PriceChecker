package ru.ravel.telegramservice.repository


import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.ravel.telegramservice.dto.User

@Repository
interface TelegramRepository extends JpaRepository<User, Long> {

	User getByTelegramId(Long id);

}
