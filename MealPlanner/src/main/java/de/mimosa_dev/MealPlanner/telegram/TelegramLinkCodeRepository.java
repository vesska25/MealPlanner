package de.mimosa_dev.MealPlanner.telegram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramLinkCodeRepository extends JpaRepository<TelegramLinkCode, Long> {

    Optional<TelegramLinkCode> findByCodeHash(String codeHash);

    List<TelegramLinkCode> findByUserIdAndUsedAtIsNull(Long userId);
}
