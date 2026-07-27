package com.unitrovee.item;

import com.unitrovee.common.ApiResponse;
import com.unitrovee.common.PageResponse;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.dto.ItemListResponse;
import com.unitrovee.item.dto.ItemUpdateRequest;
import com.unitrovee.item.dto.ItemDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ApiResponse<PageResponse<ItemListResponse>> listItems(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ExchangeType exchangeType,
            @RequestParam(required = false) String keyword,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ApiResponse.ok(itemService.getPublicItems(schoolId, categoryId, exchangeType, keyword, pageable));
    }

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

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        itemService.deleteItem(itemId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{itemId}")
    public ApiResponse<ItemDetailResponse> getItemDetail(@PathVariable Long itemId) {
        return ApiResponse.ok(itemService.getPublicItemDetail(itemId));
    }

    @PostMapping(
            value = "/{itemId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ItemDetailResponse.ImageResponse>> uploadImage(
            @PathVariable Long itemId,
            Authentication authentication,
            @RequestParam("image") MultipartFile image
    ) {
        ItemDetailResponse.ImageResponse response = itemService.uploadImage(itemId, authentication.getName(), image);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Item image uploaded"));
    }
}
