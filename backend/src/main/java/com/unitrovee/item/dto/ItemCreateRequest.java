package com.unitrovee.item.dto;

import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.ItemCondition;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemCreateRequest(

        @NotNull
        @Positive
        Long categoryId,

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 5000)
        String description,

        @NotNull
        ItemCondition condition,

        @NotNull
        ExchangeType exchangeType,

        @Digits(integer = 8, fraction = 2)
        @DecimalMin(value = "0.01")
        BigDecimal priceAmount,

        @Size(max = 255)
        String locationHint
) {

    // cross-field validation: pricing depends on the chosen exchange type
    @AssertTrue(message = "SELL requires a positive price; FREE and SWAP must not include a price")
    public boolean isPriceConsistentWithExchangeType() {
        if (exchangeType == null) return true;

        return exchangeType == ExchangeType.SELL ? priceAmount != null && priceAmount.signum() > 0 : priceAmount == null;
    }
}
