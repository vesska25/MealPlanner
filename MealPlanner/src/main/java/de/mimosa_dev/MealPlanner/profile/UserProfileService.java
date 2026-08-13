package de.mimosa_dev.MealPlanner.profile;

import de.mimosa_dev.MealPlanner.recipe.UserConstraints;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Real replacement for {@link UserConstraints#defaults()}, the stand-in every recipe-validating
 * caller has used since step 5 because no {@link UserProfile} existed yet.
 */
@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<UserProfile> findByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    /**
     * A user with no profile row yet (onboarding not finished) has no known exclusions to
     * violate, so falling back to {@link UserConstraints#defaults()} isn't an INV-04 breach —
     * it's a real, temporary gap though: their eventual real allergies aren't enforced until
     * they finish onboarding. Closed by routing new registrations through onboarding first, not
     * by this method.
     */
    public UserConstraints constraintsFor(Long userId) {
        return findByUserId(userId)
                .map(UserProfileService::toConstraints)
                .orElseGet(UserConstraints::defaults);
    }

    private static UserConstraints toConstraints(UserProfile profile) {
        return new UserConstraints(
                profile.getExcludedProductIds(), profile.getEquipment(), profile.getMaxCookTimeWeekdayMinutes());
    }
}
