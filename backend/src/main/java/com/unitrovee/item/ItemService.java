package com.unitrovee.item;

import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.dto.ItemUpdateRequest;

public interface ItemService {

    ItemCreateResponse createItem(String authenticatedEmail, ItemCreateRequest request);

    void updateItem(Long itemId, String authenticatedEmail, ItemUpdateRequest request);

    void deleteItem(Long itemId, String authenticatedEmail);
}
