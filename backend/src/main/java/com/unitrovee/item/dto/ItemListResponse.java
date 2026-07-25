package com.unitrovee.item.dto;

import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.ItemCondition;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemListResponse(
        Long id,
        String title,
        ItemCondition condition,
        ExchangeType exchangeType,
        BigDecimal priceAmount,
        String locationHint,
        Long schoolId,
        String schoolName,
        Long categoryId,
        String categoryName,
        Instant createdAt
) {
}
