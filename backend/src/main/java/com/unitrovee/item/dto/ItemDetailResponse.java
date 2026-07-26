package com.unitrovee.item.dto;

import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.ItemCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public record ItemDetailResponse(
        Long                    id,
        String                  title,
        String                  description,
        ItemCondition           condition,
        ExchangeType            exchangeType,
        BigDecimal              priceAmount,
        String                  locationHint,
        Instant                 createdAt,
        SchoolSummary           school,
        CategorySummary         category,
        OwnerSummary            owner,
        List<ImageResponse>     images
) {
    public record SchoolSummary     (Long id, String name) {}

    public record CategorySummary   (Long id, String name) {}

    public record OwnerSummary      (Long id, String displayName, String schoolName) {}

    public record ImageResponse     (Long id, String url, int sortOrder) {}
}
