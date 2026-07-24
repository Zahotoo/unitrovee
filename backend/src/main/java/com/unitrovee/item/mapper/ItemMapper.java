package com.unitrovee.item.mapper;

import com.unitrovee.item.domain.Item;
import com.unitrovee.item.dto.ItemCreateResponse;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public ItemCreateResponse toCreateResponse(Item item) {
        return new ItemCreateResponse(
                item.getId(),
                item.getOwner().getId(),
                item.getSchool().getId(),
                item.getCategory().getId(),
                item.getTitle(),
                item.getCondition(),
                item.getExchangeType(),
                item.getPriceAmount(),
                item.getStatus(),
                item.getLocationHint()
        );
    }
}
