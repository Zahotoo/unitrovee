package com.unitrovee.item;

import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.common.exception.ResourceNotFoundException;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.mapper.ItemMapper;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import com.unitrovee.item.dto.ItemUpdateRequest;
import com.unitrovee.item.exception.ItemNotEditableException;
import com.unitrovee.item.exception.InvalidItemUpdateException;
import com.unitrovee.common.PageResponse;
import com.unitrovee.item.dto.ItemListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unitrovee.item.domain.ItemImage;
import com.unitrovee.item.dto.ItemDetailResponse;
import com.unitrovee.storage.StorageService;

import java.util.List;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemMapper itemMapper;
    private final StorageService storageService;

    @Override
    @Transactional
    public ItemCreateResponse createItem(String authenticatedEmail, ItemCreateRequest request) {
        User owner = userRepository.findByEmailWithSchool(authenticatedEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (owner.getRole() != Role.STUDENT || !owner.isEmailVerified()) {
            throw new AccessDeniedException("Verified students only");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active category not found"));

        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(owner.getSchool());
        item.setCategory(category);
        item.setTitle(request.title().trim());
        item.setDescription(request.description().trim());
        item.setCondition(request.condition());
        item.setExchangeType(request.exchangeType());
        item.setPriceAmount(request.priceAmount());
        item.setLocationHint(
                request.locationHint() == null ? null : request.locationHint().trim()
        );

        item.setStatus(ItemStatus.DRAFT);

        Item savedItem = itemRepository.save(item);
        return itemMapper.toCreateResponse(savedItem);
    }

    @Override
    @Transactional
    public void updateItem(Long itemId, String authenticatedEmail, ItemUpdateRequest request) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getOwner().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Only the item owner may update this item");
        }

        if (item.getStatus() != ItemStatus.DRAFT && item.getStatus() != ItemStatus.AVAILABLE) {
            throw new ItemNotEditableException("Only DRAFT or AVAILABLE items can be updated");
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .filter(Category::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Active category not found"));

            item.setCategory(category);
        }

        if (request.exchangeType() != null && request.exchangeType() != ExchangeType.SELL && request.priceAmount() != null) {
            throw new InvalidItemUpdateException("FREE and SWAP must not include a price");
        }

        ExchangeType updatedExchangeType = request.exchangeType() == null ? item.getExchangeType() : request.exchangeType();
        BigDecimal updatedPriceAmount = item.getPriceAmount();

        if (request.exchangeType() != null && request.exchangeType() != ExchangeType.SELL) {
            updatedPriceAmount = null;
        } else if (request.priceAmount() != null) {
            updatedPriceAmount = request.priceAmount();
        }

        if (updatedExchangeType == ExchangeType.SELL && (updatedPriceAmount == null || updatedPriceAmount.signum() <= 0)) {
            throw new InvalidItemUpdateException("SELL requires a positive price");
        }
        item.setExchangeType(updatedExchangeType);
        item.setPriceAmount(updatedPriceAmount);

        if (request.title() != null) {
            item.setTitle(request.title().trim());
        }

        if (request.description() != null) {
            item.setDescription(request.description().trim());
        }

        if (request.condition() != null) {
            item.setCondition(request.condition());
        }

        if (request.locationHint() != null) {
            item.setLocationHint(request.locationHint().trim());
        }
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId, String authenticatedEmail) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getOwner().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Only the item owner may delete this item");
        }

        if (item.getStatus() == ItemStatus.DRAFT) {
            itemImageRepository.deleteAll(
                    itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())
            );
            itemRepository.delete(item);
            return;
        }

        if (item.getStatus() == ItemStatus.AVAILABLE) {
            item.setStatus(ItemStatus.ARCHIVED);
            return;
        }

        throw new ItemNotEditableException("Only DRAFT items can be deleted and AVAILABLE items can be archived");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemListResponse> getPublicItems(
            Long schoolId,
            Long categoryId,
            ExchangeType exchangeType,
            String keyword,
            Pageable pageable
    ) {
        Page<ItemListResponse> items = itemRepository.findAll(
                ItemSpecifications.publicAvailableItems(
                        schoolId,
                        categoryId,
                        exchangeType,
                        keyword
                ),
                pageable
        ).map(itemMapper::toListResponse);

        return PageResponse.from(items);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDetailResponse getPublicItemDetail(Long itemId) {
        Item item = itemRepository.findByIdAndStatus(itemId, ItemStatus.AVAILABLE).orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        List<ItemDetailResponse.ImageResponse> images = itemImageRepository
                .findByItemIdOrderBySortOrderAsc(itemId)
                .stream()
                .map(image -> itemMapper.toImageResponse(image, storageService.getUrl(image.getStorageKey()))).toList();

        return itemMapper.toDetailResponse(item, images);
    }
}
