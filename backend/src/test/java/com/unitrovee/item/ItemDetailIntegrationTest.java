package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemCondition;
import com.unitrovee.item.domain.ItemImage;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class ItemDetailIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemImageRepository itemImageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void getItem_returnsAvailableItemWithOrderedPublicImageUrls() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createAvailableItem(owner, school, category);

        saveImage(item, "items/detail/second-image.png", 1);
        saveImage(item, "items/detail/cover-image.png", 0);

        mockMvc.perform(get("/api/items/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")                    .value(item.getId()))
                .andExpect(jsonPath("$.data.title")                 .value("Calculus textbook"))
                .andExpect(jsonPath("$.data.description")           .value("Clean copy with no missing pages."))
                .andExpect(jsonPath("$.data.condition")             .value("PRE_OWNED_GOOD"))
                .andExpect(jsonPath("$.data.exchangeType")          .value("SELL"))
                .andExpect(jsonPath("$.data.priceAmount")           .value(25.00))
                .andExpect(jsonPath("$.data.locationHint")          .value("UCD Library"))
                .andExpect(jsonPath("$.data.school.id")             .value(school.getId()))
                .andExpect(jsonPath("$.data.school.name")           .value(school.getName()))
                .andExpect(jsonPath("$.data.category.id")           .value(category.getId()))
                .andExpect(jsonPath("$.data.category.name")         .value(category.getName()))
                .andExpect(jsonPath("$.data.owner.id")              .value(owner.getId()))
                .andExpect(jsonPath("$.data.owner.displayName")     .value("Detail Owner"))
                .andExpect(jsonPath("$.data.owner.schoolName")      .value(school.getName()))
                .andExpect(jsonPath("$.data.images.length()")       .value(2))
                .andExpect(jsonPath("$.data.images[0].url")         .value("http://localhost:8080/files/items/detail/cover-image.png"))
                .andExpect(jsonPath("$.data.images[0].sortOrder")   .value(0))
                .andExpect(jsonPath("$.data.images[1].url")         .value("http://localhost:8080/files/items/detail/second-image.png"))
                .andExpect(jsonPath("$.data.images[1].sortOrder")   .value(1))
                .andExpect(jsonPath("$.data.images[0].storageKey")  .doesNotExist())
                .andExpect(jsonPath("$.data.owner.email")           .doesNotExist())
                .andExpect(jsonPath("$.data.owner.passwordHash")    .doesNotExist())
                .andExpect(jsonPath("$.data.status")                .doesNotExist());
    }

    @Test
    void getItem_returnsNotFoundWhenItemDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @EnumSource(
            value = ItemStatus.class,
            names = {"DRAFT", "ARCHIVED", "RESERVED", "COMPLETED", "UNDER_REVIEW"}
    )
    void getItem_returnsNotFoundWhenItemIsNotPublic(ItemStatus itemStatus) throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createAvailableItem(owner, school, category);
        item.setStatus(itemStatus);
        itemRepository.saveAndFlush(item);

        mockMvc.perform(get("/api/items/{itemId}", item.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getItem_returnsOwnersCurrentSchoolSeparatelyFromListingSchoolSnapshot() throws Exception {
        School listingSchool = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        School ownerSchool = createSchool(
                "Owner Detail University",
                "ODU",
                "owner-detail.ie"
        );
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(ownerSchool);
        Item item = createAvailableItem(owner, listingSchool, category);

        mockMvc.perform(get("/api/items/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.school.name").value(listingSchool.getName()))
                .andExpect(jsonPath("$.data.owner.schoolName").value(ownerSchool.getName()));
    }

    private User createVerifiedStudent(School school) {
        User user = new User();
        user.setEmail("detail-owner@" + school.getEmailDomain());
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Detail Owner");
        user.setSchool(school);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Item createAvailableItem(User owner, School school, Category category) {
        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Calculus textbook");
        item.setDescription("Clean copy with no missing pages.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("25.00"));
        item.setLocationHint("UCD Library");
        item.setStatus(ItemStatus.AVAILABLE);
        return itemRepository.saveAndFlush(item);
    }

    private void saveImage(Item item, String storageKey, int sortOrder) {
        ItemImage image = new ItemImage();
        image.setItem(item);
        image.setStorageKey(storageKey);
        image.setSortOrder(sortOrder);
        itemImageRepository.saveAndFlush(image);
    }

    private School createSchool(String name, String shortName, String emailDomain) {
        School school = new School();
        school.setName(name);
        school.setShortName(shortName);
        school.setEmailDomain(emailDomain);
        school.setCity("Dublin");
        school.setActive(true);
        return schoolRepository.saveAndFlush(school);
    }
}
