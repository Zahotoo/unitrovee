package com.unitrovee.item;

import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemSpecifications {

    private ItemSpecifications() {}

    public static Specification<Item> publicAvailableItems(
            Long schoolId,
            Long categoryId,
            ExchangeType exchangeType,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // public listings must never expose drafts, archived, reserved, or completed items
            predicates.add(criteriaBuilder.equal(root.get("status"), ItemStatus.AVAILABLE));

            if (schoolId != null) {
                predicates.add(criteriaBuilder.equal(root.get("school").get("id"), schoolId));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (exchangeType != null) {
                predicates.add(criteriaBuilder.equal(root.get("exchangeType"), exchangeType));
            }

            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";

                predicates.add(criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern, '\\')));
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
