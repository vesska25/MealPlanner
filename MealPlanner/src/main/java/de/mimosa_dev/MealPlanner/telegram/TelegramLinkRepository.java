package de.mimosa_dev.MealPlanner.telegram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramLinkRepository extends JpaRepository<TelegramLink, Long> {

    Optional<TelegramLink> findByUserId(Long userId);

    Optional<TelegramLink> findByTelegramUserId(Long telegramUserId);
}
