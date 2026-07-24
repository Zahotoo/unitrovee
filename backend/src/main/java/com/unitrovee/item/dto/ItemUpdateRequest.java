package com.unitrovee.item.dto;

import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.ItemCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemUpdateRequest(
        @Positive
        Long categoryId,

        @Size(max = 150)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        String title,

        @Size(max = 5000)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        String description,

        ItemCondition condition,

        ExchangeType exchangeType,

        @Digits(integer = 8, fraction = 2)
        @DecimalMin(value = "0.01")
        BigDecimal priceAmount,

        @Size(max = 255)
        String locationHint
) {
}
