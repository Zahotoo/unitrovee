package com.unitrovee.item;

import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;

public interface ItemService {

    ItemCreateResponse createItem(String authenticatedEmail, ItemCreateRequest request);
}
