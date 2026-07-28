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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
@Transactional
public class ItemLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    @Test
    void publishItem_makesDraftOwnedByAuthenticatedUserAvailable() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-publish-owner@ucdconnect.ie",
                "Lifecycle Publish Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "action": "PUBLISH"
                        }
                        """))
                .andExpect(status().isNoContent());

        // read a fresh entity from postgresql, not the cached test object
        entityManager.flush();
        entityManager.clear();

        Item publishedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(publishedItem.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
    }

    @Test
    void archiveItem_makesAvailableItemOwnedByAuthenticatedUserArchived() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-archive-owner@ucdconnect.ie",
                "Lifecycle Archive Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);
        item.setStatus(ItemStatus.AVAILABLE);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                              "action": "ARCHIVE"
                            }
                            """))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        Item archivedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(archivedItem.getStatus()).isEqualTo(ItemStatus.ARCHIVED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESERVED", "COMPLETED", "UNDER_REVIEW"})
    void changeLifecycle_rejectsTradeOnlyActions(String action) throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-internal-action-" + action.toLowerCase() + "@ucdconnect.ie",
                "Lifecycle Internal Action Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                              "action": "%s"
                            }
                            """.formatted(action)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        entityManager.clear();

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
    }

    @Test
    void changeLifecycle_rejectsNonOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-real-owner@ucdconnect.ie",
                "Lifecycle Real Owner",
                school
        );
        User nonOwner = createVerifiedStudent(
                "lifecycle-non-owner@ucdconnect.ie",
                "Lifecycle Non Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(nonOwner.getEmail())
        );

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "action": "PUBLISH"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        entityManager.clear();

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
    }

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, PUBLISH",
            "DRAFT, ARCHIVE"
    })
    void changeLifecycle_rejectsInvalidStateTransition(ItemStatus startingStatus, String action) throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-invalid-" + startingStatus.name().toLowerCase() + "-" + action.toLowerCase() + "@ucdconnect.ie",
                "Lifecycle Invalid Transition Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        if (startingStatus == ItemStatus.AVAILABLE) {
            item.setStatus(ItemStatus.AVAILABLE);
            itemRepository.saveAndFlush(item);
        }

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                              "action": "%s"
                            }
                            """.formatted(action)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_EDITABLE"));

        entityManager.clear();

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(startingStatus);
    }

    @Test
    void changeLifecycle_requiresAuthentication() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-unauthenticated-owner@ucdconnect.ie",
                "Lifecycle Unauthenticated Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "action": "PUBLISH"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        entityManager.clear();

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
    }

    @Test
    void changeLifecycle_requiresAction() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = createVerifiedStudent(
                "lifecycle-missing-action-owner@ucdconnect.ie",
                "Lifecycle Missing Action Owner",
                school
        );
        Item item = createDraftItem(owner, school, category);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(patch("/api/items/{itemId}/lifecycle", item.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        entityManager.clear();

        Item unchangedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(unchangedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
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
        item.setTitle("Lifecycle test desk lamp");
        item.setDescription("A working lamp used to test item lifecycle changes.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("12.00"));
        item.setLocationHint("UCD Library");
        return itemRepository.saveAndFlush(item);
    }
}
