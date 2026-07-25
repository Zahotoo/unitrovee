package com.unitrovee.item.mapper;

import com.unitrovee.item.domain.Item;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.dto.ItemListResponse;
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

    public ItemListResponse toListResponse(Item item) {
        return new ItemListResponse(
                item.getId(),
                item.getTitle(),
                item.getCondition(),
                item.getExchangeType(),
                item.getPriceAmount(),
                item.getLocationHint(),
                item.getSchool().getId(),
                item.getSchool().getName(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getCreatedAt()
        );
    }
}
