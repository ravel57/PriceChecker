package ru.ravel.webparser.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.ravel.webparser.entity.Parser

interface WebRepository : JpaRepository<Parser, Long> {
}