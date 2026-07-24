package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemCondition;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.security.CustomUserDetailsService;
import com.unitrovee.security.JwtService;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class ItemUpdateIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;

    @Test
    void updateItem_rejectsNonOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "item-owner-update@ucdconnect.ie",
                "Item Owner",
                school
        );
        User nonOwner = createVerifiedStudent(
                "non-owner-update@ucdconnect.ie",
                "Non Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(nonOwner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "A title the non-owner must not set"
                        }
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void updateItem_updatesEditableFieldsForOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-updates-item@ucdconnect.ie",
                "Owner Updates Item",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Updated desk lamp",
                          "description": "LED lamp with an adjustable brightness setting.",
                          "condition": "PRE_OWNED_EXCELLENT",
                          "locationHint": "UCD Student Centre"
                        }
                        """))
                .andExpect(status().isNoContent());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();

        assertThat(updatedItem.getTitle()).isEqualTo("Updated desk lamp");
        assertThat(updatedItem.getDescription()).isEqualTo("LED lamp with an adjustable brightness setting.");
        assertThat(updatedItem.getCondition()).isEqualTo(ItemCondition.PRE_OWNED_EXCELLENT);
        assertThat(updatedItem.getLocationHint()).isEqualTo("UCD Student Centre");

        // Fields not sent by the client must remain unchanged.
        assertThat(updatedItem.getExchangeType()).isEqualTo(ExchangeType.SELL);
        assertThat(updatedItem.getPriceAmount()).isEqualByComparingTo("12.00");
        assertThat(updatedItem.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(updatedItem.getSchool().getId()).isEqualTo(school.getId());
        assertThat(updatedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
    }

    @Test
    void updateItem_rejectsOwnerWhenItemIsReserved() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-of-reserved-item@ucdconnect.ie",
                "Reserved Item Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setStatus(ItemStatus.RESERVED);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "This update must be rejected"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_EDITABLE"));

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getTitle()).isEqualTo("Original desk lamp");
    }

    @Test
    void updateItem_changesCategoryForOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category originalCategory = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        Category newCategory = categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(category -> !category.getId().equals(originalCategory.getId()))
                .findFirst()
                .orElseThrow();

        User owner = createVerifiedStudent(
                "owner-changes-category@ucdconnect.ie",
                "Category Change Owner",
                school
        );
        Item item = createDraftItem(owner, school, originalCategory);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "categoryId": %d
                            }
                            """.formatted(newCategory.getId())))
                .andExpect(status().isNoContent());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getCategory().getId()).isEqualTo(newCategory.getId());
        assertThat(updatedItem.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(updatedItem.getSchool().getId()).isEqualTo(school.getId());
        assertThat(updatedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
    }

    @Test
    void updateItem_changesSellItemToSwapAndClearsPrice() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-changes-to-swap@ucdconnect.ie",
                "Swap Change Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "exchangeType": "SWAP"
                            }
                            """))
                .andExpect(status().isNoContent());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getExchangeType()).isEqualTo(ExchangeType.SWAP);
        assertThat(updatedItem.getPriceAmount()).isNull();
    }

    @Test
    void updateItem_changesFreeItemToSellWithPrice() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-changes-to-sell@ucdconnect.ie",
                "Sell Change Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setExchangeType(ExchangeType.FREE);
        item.setPriceAmount(null);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "exchangeType": "SELL",
                              "priceAmount": 18.50
                            }
                            """))
                .andExpect(status().isNoContent());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getExchangeType()).isEqualTo(ExchangeType.SELL);
        assertThat(updatedItem.getPriceAmount()).isEqualByComparingTo("18.50");
    }

    @Test
    void updateItem_rejectsSellWithoutPrice() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-missing-sell-price@ucdconnect.ie",
                "Missing Sell Price Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setExchangeType(ExchangeType.FREE);
        item.setPriceAmount(null);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "exchangeType": "SELL"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getExchangeType()).isEqualTo(ExchangeType.FREE);
        assertThat(unchangedItem.getPriceAmount()).isNull();
    }

    @Test
    void updateItem_rejectsSwapWithPrice() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-swap-with-price@ucdconnect.ie",
                "Swap With Price Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "exchangeType": "SWAP",
                              "priceAmount": 5.00
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getExchangeType()).isEqualTo(ExchangeType.SELL);
        assertThat(unchangedItem.getPriceAmount()).isEqualByComparingTo("12.00");
    }

    @Test
    void updateItem_rejectsBlankTitle() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-blank-title@ucdconnect.ie",
                "Blank Title Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "   "
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getTitle()).isEqualTo("Original desk lamp");
    }

    @Test
    void updateItem_allowsOwnerToUpdateAvailableItem() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "owner-available-item@ucdconnect.ie",
                "Available Item Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setStatus(ItemStatus.AVAILABLE);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "title": "Updated available desk lamp"
                            }
                            """))
                .andExpect(status().isNoContent());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getTitle()).isEqualTo("Updated available desk lamp");
        assertThat(updatedItem.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
    }

    @Test
    void updateItem_rejectsInactiveCategory() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category activeCategory = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        Category inactiveCategory = new Category();
        inactiveCategory.setName("Inactive Update Category");
        inactiveCategory.setSlug("inactive-update-category");
        inactiveCategory.setActive(false);
        categoryRepository.saveAndFlush(inactiveCategory);

        User owner = createVerifiedStudent(
                "owner-inactive-category@ucdconnect.ie",
                "Inactive Category Owner",
                school
        );
        Item item = createDraftItem(owner, school, activeCategory);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(patch("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "categoryId": %d
                            }
                            """.formatted(inactiveCategory.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getCategory().getId()).isEqualTo(activeCategory.getId());
    }

    private User createVerifiedStudent(String email, String displayName, School school) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$test");
        user.setDisplayName(displayName);
        user.setSchool(school);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Item createDraftItem(User owner, School school, Category category) {
        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Original desk lamp");
        item.setDescription("A working lamp in good condition.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("12.00"));
        item.setLocationHint("UCD Library");
        item.setStatus(ItemStatus.DRAFT);
        return itemRepository.saveAndFlush(item);
    }
}
