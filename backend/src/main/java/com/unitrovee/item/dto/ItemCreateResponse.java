package com.unitrovee.item.dto;

import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.item.domain.ItemCondition;

import java.math.BigDecimal;

public record ItemCreateResponse(
        Long id,
        Long ownerId,
        Long schoolId,
        Long categoryId,
        String title,
        ItemCondition condition,
        ExchangeType exchangeType,
        BigDecimal priceAmount,
        ItemStatus status,
        String locationHint
) {
}
