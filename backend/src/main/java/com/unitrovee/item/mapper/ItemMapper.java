package com.unitrovee.item.mapper;

import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemImage;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.dto.ItemDetailResponse;
import com.unitrovee.item.dto.ItemListResponse;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public ItemDetailResponse toDetailResponse(Item item, List<ItemDetailResponse.ImageResponse> images) {
        return new ItemDetailResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getCondition(),
                item.getExchangeType(),
                item.getPriceAmount(),
                item.getLocationHint(),
                item.getCreatedAt(),
                new ItemDetailResponse.SchoolSummary(
                        item.getSchool().getId(),
                        item.getSchool().getName()
                ),
                new ItemDetailResponse.CategorySummary(
                        item.getCategory().getId(),
                        item.getCategory().getName()
                ),
                new ItemDetailResponse.OwnerSummary(
                        item.getOwner().getId(),
                        item.getOwner().getDisplayName(),
                        item.getOwner().getSchool().getName()
                ),
                images
        );
    }

    public ItemDetailResponse.ImageResponse toImageResponse(ItemImage image, String url) {
        return new ItemDetailResponse.ImageResponse(
                image.getId(),
                url,
                image.getSortOrder()
        );
    }
}
