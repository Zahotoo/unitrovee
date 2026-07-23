package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemImage;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class ItemImageRepositoryTest extends AbstractIntegrationTest {

    @Autowired private ItemImageRepository itemImageRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void findByItemIdOrderBySortOrderAsc_returnsImagesInDisplayOrder() {
        Item item = createItem();

        ItemImage secondImage = new ItemImage();
        secondImage.setItem(item);
        secondImage.setStorageKey("items/test/second-image.png");
        secondImage.setSortOrder(1);
        itemImageRepository.saveAndFlush(secondImage);

        ItemImage coverImage = new ItemImage();
        coverImage.setItem(item);
        coverImage.setStorageKey("items/test/cover-image.png");
        coverImage.setSortOrder(0);
        itemImageRepository.saveAndFlush(coverImage);

        List<ItemImage> images = itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId());

        assertThat(images)
                .extracting(ItemImage::getStorageKey)
                .containsExactly(
                        "items/test/cover-image.png",
                        "items/test/second-image.png"
                );

        assertThat(images)
                .extracting(ItemImage::getSortOrder)
                .containsExactly(0, 1);
    }

    @Test
    void saveAndFlush_rejectsDuplicateSortOrderForSameItem() {
        Item item = createItem();

        ItemImage coverImage = new ItemImage();
        coverImage.setItem(item);
        coverImage.setStorageKey("items/test/original-cover.png");
        coverImage.setSortOrder(0);
        itemImageRepository.saveAndFlush(coverImage);

        ItemImage duplicateCoverImage = new ItemImage();
        duplicateCoverImage.setItem(item);
        duplicateCoverImage.setStorageKey("items/test/duplicate-cover.png");
        duplicateCoverImage.setSortOrder(0);

        assertThatThrownBy(() -> itemImageRepository.saveAndFlush(duplicateCoverImage))
                .isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    void saveAndFlush_rejectsDuplicateStorageKey() {
        Item item = createItem();

        ItemImage firstImage = new ItemImage();
        firstImage.setItem(item);
        firstImage.setStorageKey("items/test/shared-file.png");
        firstImage.setSortOrder(0);
        itemImageRepository.saveAndFlush(firstImage);

        ItemImage duplicateStorageKeyImage = new ItemImage();
        duplicateStorageKeyImage.setItem(item);
        duplicateStorageKeyImage.setStorageKey("items/test/shared-file.png");
        duplicateStorageKeyImage.setSortOrder(1);

        assertThatThrownBy(() -> itemImageRepository.saveAndFlush(duplicateStorageKeyImage))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Item createItem() {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        Category category = categoryRepository.findByActiveTrueOrderByNameAsc()
                .getFirst();

        User owner = new User();
        owner.setEmail("image-owner@ucdconnect.ie");
        owner.setPasswordHash("test-password-hash");
        owner.setDisplayName("Image Owner");
        owner.setSchool(school);
        userRepository.saveAndFlush(owner);

        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Item with images");
        item.setDescription("Used to test image ordering.");
        item.setCondition("GOOD");
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("20.00"));

        return itemRepository.saveAndFlush(item);
    }
}