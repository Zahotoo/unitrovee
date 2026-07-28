package com.unitrovee.item.dto;

import jakarta.validation.constraints.NotNull;

public record ItemLifecycleRequest(

        @NotNull(message = "action is required")
        ItemLifecycleAction action
) {
}
