package de.mimosa_dev.MealPlanner.product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCanonicalNameContainingIgnoreCase(String canonicalName);

    Optional<Product> findByCanonicalNameIgnoreCase(String canonicalName);

    @Query("SELECT p FROM Product p JOIN p.synonyms s WHERE LOWER(s) = LOWER(:synonym)")
    Optional<Product> findBySynonymIgnoreCase(@Param("synonym") String synonym);
}
