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
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.sql.Timestamp;
import java.time.Instant;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class ItemListIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void listItems_returnsOnlyAvailableItemsWithoutAuthentication() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent("item-list-owner@ucdconnect.ie", school);

        Item availableItem = createItem(
                owner, school, category, "Available desk lamp", ItemStatus.AVAILABLE
        );
        createItem(owner, school, category, "Draft desk lamp", ItemStatus.DRAFT);
        createItem(owner, school, category, "Archived desk lamp", ItemStatus.ARCHIVED);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(availableItem.getId()))
                .andExpect(jsonPath("$.data.content[0].title").value("Available desk lamp"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listItems_combinesSchoolCategoryExchangeTypeAndKeywordFilters() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        School otherSchool = new School();
        otherSchool.setName("Filter Test University");
        otherSchool.setShortName("FTU");
        otherSchool.setEmailDomain("filter-test.ie");
        otherSchool.setCity("Dublin");
        otherSchool.setActive(true);
        schoolRepository.saveAndFlush(otherSchool);

        Category otherCategory = categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(candidate -> !candidate.getId().equals(category.getId()))
                .findFirst()
                .orElseThrow();

        User owner = createVerifiedStudent("filter-owner@ucdconnect.ie", school);
        User otherOwner = createVerifiedStudent("filter-other-owner@filter-test.ie", otherSchool);

        Item matchingItem = createItem(
                owner,
                school,
                category,
                "Mountain bike",
                "A lightweight bike for campus commuting.",
                ItemStatus.AVAILABLE,
                ExchangeType.SWAP
        );

        createItem(
                otherOwner,
                otherSchool,
                category,
                "Mountain bike",
                "Same keyword, but from another school.",
                ItemStatus.AVAILABLE,
                ExchangeType.SWAP
        );

        createItem(
                owner,
                school,
                otherCategory,
                "Mountain bike",
                "Same keyword, but another category.",
                ItemStatus.AVAILABLE,
                ExchangeType.SWAP
        );

        createItem(
                owner,
                school,
                category,
                "Mountain bike",
                "Same keyword, but this is for sale.",
                ItemStatus.AVAILABLE,
                ExchangeType.SELL
        );

        createItem(
                owner,
                school,
                category,
                "Desk lamp",
                "A useful item, but it does not match the keyword.",
                ItemStatus.AVAILABLE,
                ExchangeType.SWAP
        );

        mockMvc.perform(get("/api/items")
                        .param("schoolId", school.getId().toString())
                        .param("categoryId", category.getId().toString())
                        .param("exchangeType", "SWAP")
                        .param("keyword", "mountain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(matchingItem.getId()))
                .andExpect(jsonPath("$.data.content[0].schoolId").value(school.getId()))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(category.getId()))
                .andExpect(jsonPath("$.data.content[0].exchangeType").value("SWAP"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listItems_returnsRequestedPageAndPaginationMetadata() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent("pagination-owner@ucdconnect.ie", school);

        createItem(owner, school, category, "Alpha chair", ItemStatus.AVAILABLE);
        createItem(owner, school, category, "Bravo chair", ItemStatus.AVAILABLE);
        Item thirdItem = createItem(owner, school, category, "Charlie chair", ItemStatus.AVAILABLE);

        mockMvc.perform(get("/api/items")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(thirdItem.getId()))
                .andExpect(jsonPath("$.data.content[0].title").value("Charlie chair"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void listItems_doesNotRevealArchivedItemsWhenStatusQueryParameterIsProvided() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent("public-status-owner@ucdconnect.ie", school);

        Item availableItem = createItem(
                owner, school, category, "Public available item", ItemStatus.AVAILABLE
        );
        createItem(
                owner, school, category, "Private archived item", ItemStatus.ARCHIVED
        );

        mockMvc.perform(get("/api/items")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(availableItem.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listItems_matchesKeywordInDescriptionIgnoringCase() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent("description-keyword-owner@ucdconnect.ie", school);

        Item matchingItem = createItem(
                owner,
                school,
                category,
                "Mechanical keyboard",
                "Includes CALCULUS notes and a USB cable.",
                ItemStatus.AVAILABLE,
                ExchangeType.SELL
        );

        createItem(
                owner,
                school,
                category,
                "Desk lamp",
                "Useful for late-night study.",
                ItemStatus.AVAILABLE,
                ExchangeType.SELL
        );

        mockMvc.perform(get("/api/items")
                        .param("keyword", "calculus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(matchingItem.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listItems_usesCreatedAtDescendingOrderByDefault() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent("default-sort-owner@ucdconnect.ie", school);

        Item oldestItem = createItem(
                owner, school, category, "Oldest item", ItemStatus.AVAILABLE
        );
        Item middleItem = createItem(
                owner, school, category, "Middle item", ItemStatus.AVAILABLE
        );
        Item newestItem = createItem(
                owner, school, category, "Newest item", ItemStatus.AVAILABLE
        );

        updateCreatedAt(oldestItem.getId(), Instant.parse("2026-01-01T00:00:00Z"));
        updateCreatedAt(middleItem.getId(), Instant.parse("2026-02-01T00:00:00Z"));
        updateCreatedAt(newestItem.getId(), Instant.parse("2026-03-01T00:00:00Z"));
        entityManager.clear();

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].id").value(newestItem.getId()))
                .andExpect(jsonPath("$.data.content[1].id").value(middleItem.getId()))
                .andExpect(jsonPath("$.data.content[2].id").value(oldestItem.getId()));
    }

    private User createVerifiedStudent(String email, School school) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Item List Owner");
        user.setSchool(school);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Item createItem(
            User owner,
            School school,
            Category category,
            String title,
            ItemStatus status
    ) {
        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle(title);
        item.setDescription("A listing created for public item-list testing.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("12.00"));
        item.setLocationHint("UCD Library");
        item.setStatus(status);
        return itemRepository.saveAndFlush(item);
    }

    private Item createItem(
            User owner,
            School school,
            Category category,
            String title,
            String description,
            ItemStatus status,
            ExchangeType exchangeType
    ) {
        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle(title);
        item.setDescription(description);
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(exchangeType);
        item.setPriceAmount(
                exchangeType == ExchangeType.SELL ? new BigDecimal("12.00") : null
        );
        item.setLocationHint("UCD Library");
        item.setStatus(status);
        return itemRepository.saveAndFlush(item);
    }

    private void updateCreatedAt(Long itemId, Instant createdAt) {
        entityManager.createNativeQuery(
                        "UPDATE items SET created_at = :createdAt WHERE id = :itemId"
                )
                .setParameter("createdAt", Timestamp.from(createdAt))
                .setParameter("itemId", itemId)
                .executeUpdate();
    }
}
