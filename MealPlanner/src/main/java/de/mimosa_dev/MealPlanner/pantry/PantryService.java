package de.mimosa_dev.MealPlanner.pantry;

import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Deterministic pantry stock operations (PRD 9.1 step 4). Concurrency for these normal
 * operations relies on {@link PantryItem}'s optimistic {@code @Version} field (NFR-43a); the
 * pessimistic locking NFR-43b requires for the multi-position cooking transaction is step 8's
 * concern, not this class's.
 */
@Service
public class PantryService {

    private final PantryItemRepository pantryItemRepository;

    public PantryService(PantryItemRepository pantryItemRepository) {
        this.pantryItemRepository = pantryItemRepository;
    }

    /**
     * FR-20/FR-21: creates a new ACTIVE stock position. Expiry is always derived from the
     * product's categorical shelf-life default — never entered by the user.
     */
    @Transactional
    public PantryItem addStock(Long userId, Product product, BigDecimal quantity, Unit unit, LocalDate purchasedAt) {
        PantryItem item = new PantryItem();
        item.setUserId(userId);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setPurchasedAt(purchasedAt);
        item.setExpiresAt(purchasedAt.plusDays(product.getDefaultShelfLifeDays()));
        item.setStatus(PantryItemStatus.ACTIVE);
        return pantryItemRepository.save(item);
    }

    /**
     * FR-26/FR-27/FR-28: deducts {@code quantity} from the user's ACTIVE stock of
     * {@code productId} in {@code unit}, spread across positions oldest-expiry-first (FIFO by
     * spoilage). Draining a position partially updates its quantity in place (FR-26); draining
     * it fully flips it to {@code CONSUMED}. If total available stock is short, the whole
     * operation is rejected up front — no partial deduction is ever applied (FR-28).
     *
     * <p>Staple products (FR-29) are not filtered out here — that exclusion belongs to the
     * recipe-feasibility check (PRD step 5), which simply never calls this method for staple
     * ingredients. This method has no opinion on which products are trackable.
     *
     * @throws InsufficientStockException if available ACTIVE stock in {@code unit} is less
     *                                    than {@code quantity}
     */
    @Transactional
    public void consume(Long userId, Long productId, BigDecimal quantity, Unit unit) {
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Consumed quantity must be positive, got " + quantity);
        }

        List<PantryItem> candidates = pantryItemRepository
                .findByUserIdAndProductIdAndUnitAndStatusOrderByExpiresAtAsc(
                        userId, productId, unit, PantryItemStatus.ACTIVE);

        BigDecimal available = candidates.stream()
                .map(PantryItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (available.compareTo(quantity) < 0) {
            throw new InsufficientStockException(productId, unit, quantity, available);
        }

        BigDecimal remaining = quantity;
        for (PantryItem item : candidates) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal deducted = item.getQuantity().min(remaining);
            item.setQuantity(item.getQuantity().subtract(deducted));
            if (item.getQuantity().signum() == 0) {
                item.setStatus(PantryItemStatus.CONSUMED);
            }
            remaining = remaining.subtract(deducted);
        }
        pantryItemRepository.saveAll(candidates);
    }

    /**
     * FR-22/FR-23: marks an ACTIVE position as discarded with a reason, without deleting the
     * row — discard history feeds future anti-waste stats. INV-02: an already
     * consumed/discarded item cannot be discarded again.
     */
    @Transactional
    public void discard(Long pantryItemId, DiscardReason reason) {
        PantryItem item = pantryItemRepository.findById(pantryItemId)
                .orElseThrow(() -> new NoSuchElementException("Pantry item " + pantryItemId + " not found"));
        if (item.getStatus() != PantryItemStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Pantry item " + pantryItemId + " is " + item.getStatus() + ", not ACTIVE");
        }
        item.setStatus(PantryItemStatus.DISCARDED);
        item.setDiscardReason(reason);
        pantryItemRepository.save(item);
    }
}
