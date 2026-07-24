package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemCondition;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.security.CustomUserDetailsService;
import com.unitrovee.security.JwtService;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.item.domain.ItemImage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@AutoConfigureMockMvc
@Transactional
public class ItemDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;
    @Autowired private ItemImageRepository itemImageRepository;

    @Test
    void deleteItem_permanentlyDeletesDraftItemOwnedByAuthenticatedUser() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "delete-owner@ucdconnect.ie",
                "Delete Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(itemRepository.findById(item.getId())).isEmpty();
    }

    @Test
    void deleteItem_archiveAvailableItemOwnedByAuthenticatedUser() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "archive-available-owner@ucdconnect.ie",
                "Available Item Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setStatus(ItemStatus.AVAILABLE);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        Item archivedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(archivedItem.getStatus()).isEqualTo(ItemStatus.ARCHIVED);
    }

    @Test
    void deleteItem_deletesImageMetadataWithDraftItem() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "delete-item-with-image@ucdconnect.ie",
                "Delete Image Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        ItemImage image = new ItemImage();
        image.setItem(item);
        image.setStorageKey("items/test/delete-item-image.png");
        image.setSortOrder(0);
        itemImageRepository.saveAndFlush(image);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(itemRepository.findById(item.getId())).isEmpty();
        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).isEmpty();
    }

    @Test
    void deleteItem_rejectsNonOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "delete-real-owner@ucdconnect.ie",
                "Real Owner",
                school
        );
        User nonOwner = createVerifiedStudent(
                "delete-non-owner@ucdconnect.ie",
                "Non Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(nonOwner.getEmail())
        );

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        entityManager.clear();
        assertThat(itemRepository.findById(item.getId())).isPresent();
    }

    @Test
    void deleteItem_rejectsReservedItemEvenForOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "delete-reserved-owner@ucdconnect.ie",
                "Reserved Item Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setStatus(ItemStatus.RESERVED);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_EDITABLE"));

        entityManager.clear();
        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @ParameterizedTest
    @EnumSource(
            value = ItemStatus.class,
            names = {"COMPLETED", "ARCHIVED", "UNDER_REVIEW"}
    )
    void deleteItem_rejectsOtherNonDeletableStatuses(ItemStatus itemStatus) throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "delete-status-" + itemStatus.name().toLowerCase() + "@ucdconnect.ie",
                "Non Deletable Status Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setStatus(itemStatus);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_EDITABLE"));

        entityManager.clear();
        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(itemStatus);
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
        item.setTitle("Delete test desk lamp");
        item.setDescription("A working lamp used to test hard deletion.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("12.00"));
        item.setLocationHint("UCD Library");
        return itemRepository.saveAndFlush(item);
    }
}
