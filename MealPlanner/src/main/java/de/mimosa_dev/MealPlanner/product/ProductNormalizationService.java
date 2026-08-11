package de.mimosa_dev.MealPlanner.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Matches a recognized product name to the canonical catalogue entry, or creates a new
 * unverified one (PRD 9.1 step 4, AI-04a). Used by every scenario that turns free-form input
 * into a canonical product: pantry, shopping list, recipes, nutrition calc.
 *
 * <p>Translating free text into an English canonical name is the model's job (AI-04a: "ввод и
 * вывод модели — на языке пользователя, с приведением к каноническому имени"); this component
 * only does the deterministic part — deduping the model's candidate name against the existing
 * catalogue (exact match, then synonym match, then fuzzy match) before ever creating a new
 * entry, per FR-43c's synonym-bloat protection.
 */
@Service
public class ProductNormalizationService {

    // Similarity ratio (1 - normalized Levenshtein distance) above which two names are
    // considered the same product. Conservative on purpose — a false merge (treating two
    // different products as one) is worse than an occasional avoidable duplicate.
    private static final double FUZZY_MATCH_THRESHOLD = 0.85;

    // Conservative placeholder shelf life for a product the catalogue has never seen before
    // (FR-21 requires a categorical default, not user input; category is unknown here).
    private static final int DEFAULT_SHELF_LIFE_DAYS_FOR_UNKNOWN_PRODUCT = 7;

    private final ProductRepository productRepository;

    public ProductNormalizationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * @param recognizedName an already English-ish candidate canonical name, as produced by
     *                       the agent layer's input normalization
     * @return the matching catalogue entry, or a newly created unverified one (FR-43d) if
     *         nothing in the catalogue is close enough
     */
    @Transactional
    public Product resolve(String recognizedName) {
        String normalized = normalize(recognizedName);

        return productRepository.findByCanonicalNameIgnoreCase(normalized)
                .or(() -> productRepository.findBySynonymIgnoreCase(normalized))
                .or(() -> fuzzyMatch(normalized))
                .orElseGet(() -> createUnverified(normalized));
    }

    private static String normalize(String rawName) {
        return rawName.strip().toLowerCase(Locale.ROOT);
    }

    private Optional<Product> fuzzyMatch(String normalized) {
        Product best = null;
        double bestSimilarity = 0;
        for (Product candidate : productRepository.findAll()) {
            double similarity = bestSimilarityAgainst(normalized, candidate);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                best = candidate;
            }
        }
        return bestSimilarity >= FUZZY_MATCH_THRESHOLD ? Optional.of(best) : Optional.empty();
    }

    private static double bestSimilarityAgainst(String normalized, Product candidate) {
        double best = similarity(normalized, candidate.getCanonicalName().toLowerCase(Locale.ROOT));
        for (String synonym : candidate.getSynonyms()) {
            best = Math.max(best, similarity(normalized, synonym.toLowerCase(Locale.ROOT)));
        }
        return best;
    }

    private static double similarity(String a, String b) {
        int maxLength = Math.max(a.length(), b.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - ((double) levenshteinDistance(a, b) / maxLength);
    }

    private static int levenshteinDistance(String a, String b) {
        int[] previousRow = new int[b.length() + 1];
        int[] currentRow = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previousRow[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            currentRow[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitutionCost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                currentRow[j] = Math.min(
                        Math.min(currentRow[j - 1] + 1, previousRow[j] + 1),
                        previousRow[j - 1] + substitutionCost);
            }
            System.arraycopy(currentRow, 0, previousRow, 0, currentRow.length);
        }
        return previousRow[b.length()];
    }

    private Product createUnverified(String normalized) {
        Product product = new Product();
        product.setCanonicalName(normalized);
        product.setCategory(ProductCategory.OTHER);
        product.setDefaultShelfLifeDays(DEFAULT_SHELF_LIFE_DAYS_FOR_UNKNOWN_PRODUCT);
        product.setVerified(false);
        return productRepository.save(product);
    }
}
