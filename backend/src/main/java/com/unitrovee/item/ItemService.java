package com.unitrovee.item;

import com.unitrovee.item.dto.*;
import com.unitrovee.common.PageResponse;
import com.unitrovee.item.domain.ExchangeType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ItemService {

    ItemCreateResponse createItem(String authenticatedEmail, ItemCreateRequest request);

    void updateItem(Long itemId, String authenticatedEmail, ItemUpdateRequest request);

    void deleteItem(Long itemId, String authenticatedEmail);

    void changeLifecycle(Long itemId, String authenticatedEmail, ItemLifecycleRequest request);

    PageResponse<ItemListResponse> getPublicItems(
            Long schoolId,
            Long categoryId,
            ExchangeType exchangeType,
            String keyword,
            Pageable pageable
    );

    ItemDetailResponse getPublicItemDetail(Long itemId);

    ItemDetailResponse.ImageResponse uploadImage(
            Long itemId,
            String authenticatedEmail,
            MultipartFile image
    );
}
