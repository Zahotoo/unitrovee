package com.unitrovee.item;

import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.dto.ItemUpdateRequest;
import com.unitrovee.common.PageResponse;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.dto.ItemListResponse;
import com.unitrovee.item.dto.ItemDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ItemService {

    ItemCreateResponse createItem(String authenticatedEmail, ItemCreateRequest request);

    void updateItem(Long itemId, String authenticatedEmail, ItemUpdateRequest request);

    void deleteItem(Long itemId, String authenticatedEmail);

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
