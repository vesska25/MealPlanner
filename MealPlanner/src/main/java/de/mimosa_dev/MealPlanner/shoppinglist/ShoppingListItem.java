package de.mimosa_dev.MealPlanner.shoppinglist;

import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of {@link ShoppingList#getItems()}. */
@Entity
@Table(name = "shopping_list_item")
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    // Denormalized alongside shoppingList, matching every other user-owned entity in this
    // codebase — ownership checks (FR-03) are a direct column filter, not a join through a parent.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "block", nullable = false)
    private ShoppingListBlock block;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShoppingListItemStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ShoppingListItem() {
    }

    public ShoppingListItem(Long userId, Product product, BigDecimal quantity, Unit unit, ShoppingListBlock block) {
        this.userId = userId;
        this.product = product;
        this.quantity = quantity;
        this.unit = unit;
        this.block = block;
        this.status = ShoppingListItemStatus.PENDING;
    }

    void setShoppingList(ShoppingList shoppingList) {
        this.shoppingList = shoppingList;
    }

    public void resolve(ShoppingListItemStatus status) {
        this.status = status;
        this.resolvedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public Long getUserId() {
        return userId;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public ShoppingListBlock getBlock() {
        return block;
    }

    public ShoppingListItemStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
