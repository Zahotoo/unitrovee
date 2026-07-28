package com.unitrovee.item;

import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.common.exception.ResourceNotFoundException;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.item.dto.*;
import com.unitrovee.item.mapper.ItemMapper;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import com.unitrovee.item.exception.ItemNotEditableException;
import com.unitrovee.item.exception.InvalidItemUpdateException;
import com.unitrovee.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unitrovee.item.domain.ItemImage;
import org.springframework.web.multipart.MultipartFile;
import com.unitrovee.storage.StorageService;
import com.unitrovee.item.exception.InvalidItemImageException;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGES_PER_ITEM = 8;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};

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
        Item item = itemRepository.findByIdForUpdate(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getOwner().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Only the item owner may delete this item");
        }

        if (item.getStatus() == ItemStatus.DRAFT) {
            List<ItemImage> images = itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId());

            itemImageRepository.deleteAll(images);
            itemRepository.delete(item);

            deleteStoredFilesAfterCommit(images);
            return;
        }

        throw new ItemNotEditableException("Only DRAFT items can be deleted");
    }

    @Override
    @Transactional
    public void changeLifecycle(Long itemId, String authenticatedEmail, ItemLifecycleRequest request) {
        // locks this item row so lifecycle changes cannot interleave with upload/delete
        Item item = itemRepository.findByIdForUpdate(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getOwner().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Only the item owner may change its lifecycle");
        }

        if (request.action() == ItemLifecycleAction.PUBLISH && item.getStatus() == ItemStatus.DRAFT) {
            item.setStatus(ItemStatus.AVAILABLE);
            return;
        }

        if (request.action() == ItemLifecycleAction.ARCHIVE && item.getStatus() == ItemStatus.AVAILABLE) {
            item.setStatus(ItemStatus.ARCHIVED);
            return;
        }

        throw new ItemNotEditableException("This lifecycle action is not allowed for the item's current status");
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

    @Override
    @Transactional
    public ItemDetailResponse.ImageResponse uploadImage(Long itemId, String authenticatedEmail, MultipartFile image) {
        Item item = itemRepository.findByIdForUpdate(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if (!item.getOwner().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("Only the item owner may upload images");
        }

        if (item.getStatus() != ItemStatus.DRAFT && item.getStatus() != ItemStatus.AVAILABLE) {
            throw new ItemNotEditableException("Images can only be uploaded to DRAFT or AVAILABLE items");
        }

        validateImageContentType(image);

        validateImageSignature(image);

        validateImageFilename(image);

        validateImageSize(image);

        List<ItemImage> existingImages = itemImageRepository.findByItemIdOrderBySortOrderAsc(itemId);
        if (existingImages.size() >= MAX_IMAGES_PER_ITEM) {
            throw new ItemNotEditableException("An item can have at most 8 images");
        }

        String storageKey = storageService.store(image);
        registerStorageCleanupOnRollback(storageKey);

        ItemImage itemImage = new ItemImage();
        itemImage.setItem(item);
        itemImage.setStorageKey(storageKey);
        itemImage.setSortOrder(existingImages.size());

        ItemImage savedImage = itemImageRepository.saveAndFlush(itemImage);

        return itemMapper.toImageResponse(savedImage, storageService.getUrl(storageKey));
    }

    private void validateImageContentType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidItemImageException("Only JPEG, PNG and WEBP image files are allowed");
        }
    }

    private void validateImageSize(MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new InvalidItemImageException("Image size must not exceed 5 MB");
        }
    }

    private void validateImageFilename(MultipartFile image) {
        String originalFilename = image.getOriginalFilename();
        String contentType = image.getContentType();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidItemImageException("Image filename is required");
        }

        String normalizedFilename = originalFilename.toLowerCase(Locale.ROOT);

        boolean filenameMatchesContentType = switch (contentType) {
            case "image/jpeg"   -> normalizedFilename.endsWith(".jpg") || normalizedFilename.endsWith(".jpeg");
            case "image/png"    -> normalizedFilename.endsWith(".png");
            case "image/webp"   -> normalizedFilename.endsWith(".webp");
            default -> false;
        };

        if (!filenameMatchesContentType) {
            throw new InvalidItemImageException("Image filename extension must match its content type");
        }
    }

    private void validateImageSignature(MultipartFile image) {
        byte[] header;

        try (InputStream inputStream = image.getInputStream()) {
            header = inputStream.readNBytes(12);
        } catch (IOException ex) {
            throw new InvalidItemImageException("Unable to read image file");
        }

        boolean signatureMatches = switch (image.getContentType()) {
            case "image/png"    -> hasPrefix(header, PNG_SIGNATURE);
            case "image/jpeg"   -> hasPrefix(header, JPEG_SIGNATURE);
            case "image/webp"   -> hasPrefix(header, RIFF_SIGNATURE)
                    && header.length >= 12
                    && header[8] == WEBP_SIGNATURE[0]
                    && header[9] == WEBP_SIGNATURE[1]
                    && header[10] == WEBP_SIGNATURE[2]
                    && header[11] == WEBP_SIGNATURE[3];
            default -> false;
        };

        if (!signatureMatches) {
            throw new InvalidItemImageException("Image content does not match its declared type");
        }
    }

    private boolean hasPrefix(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void registerStorageCleanupOnRollback(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                            deleteStoredFileSafely(storageKey);
                        }
                    }
                }
        );
    }

    private void deleteStoredFilesAfterCommit(List<ItemImage> images) {
        List<String> storageKeys = images.stream()
                .map(ItemImage::getStorageKey)
                .toList();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        storageKeys.forEach(ItemServiceImpl.this::deleteStoredFileSafely);
                    }
                }
        );
    }

    private void deleteStoredFileSafely(String storageKey) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException ex) {
            log.error("Failed to delete stored file after transaction completion: {}", storageKey, ex);
        }
    }
}
