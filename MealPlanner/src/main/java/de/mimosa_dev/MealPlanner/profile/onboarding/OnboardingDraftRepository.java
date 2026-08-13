package de.mimosa_dev.MealPlanner.profile.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingDraftRepository extends JpaRepository<OnboardingDraft, Long> {

    Optional<OnboardingDraft> findByUserId(Long userId);
}
