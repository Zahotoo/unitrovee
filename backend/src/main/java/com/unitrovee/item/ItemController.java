package com.unitrovee.item;

import com.unitrovee.common.ApiResponse;
import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.unitrovee.item.dto.ItemUpdateRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemCreateResponse>> createItem(
            Authentication authentication,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        ItemCreateResponse response = itemService.createItem(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Item created"));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Void> updateItem(
            @PathVariable Long itemId,
            Authentication authentication,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        itemService.updateItem(itemId, authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
