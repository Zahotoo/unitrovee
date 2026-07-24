package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class ItemCreateIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;

    @Test
    void createItem_rejectsUnverifiedUser() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User user = new User();
        user.setEmail("unverified-seller@ucdconnect.ie");
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Unverified Seller");
        user.setSchool(school);
        user.setEmailVerified(false);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));

        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryId": %d,
                          "title": "Calculus textbook",
                          "description": "Clean copy with no missing pages.",
                          "condition": "PRE_OWNED_GOOD",
                          "exchangeType": "SELL",
                          "priceAmount": 25.00,
                          "locationHint": "UCD Library"
                        }
                        """.formatted(category.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void createItem_createsDraftOwnedByAuthenticatedVerifiedUser() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User user = new User();
        user.setEmail("verified-seller@ucdconnect.ie");
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Verified Seller");
        user.setSchool(school);
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));

        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryId": %d,
                          "title": "Calculus textbook",
                          "description": "Clean copy with no missing pages.",
                          "condition": "PRE_OWNED_GOOD",
                          "exchangeType": "SELL",
                          "priceAmount": 25.00,
                          "locationHint": "UCD Library"
                        }
                        """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.ownerId").value(user.getId()))
                .andExpect(jsonPath("$.data.schoolId").value(school.getId()))
                .andExpect(jsonPath("$.data.categoryId").value(category.getId()))
                .andExpect(jsonPath("$.data.title").value("Calculus textbook"))
                .andExpect(jsonPath("$.data.condition").value("PRE_OWNED_GOOD"))
                .andExpect(jsonPath("$.data.exchangeType").value("SELL"))
                .andExpect(jsonPath("$.data.priceAmount").value(25.00))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.locationHint").value("UCD Library"));
    }

    @Test
    void createItem_rejectsFreeItemWithPrice() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User user = new User();
        user.setEmail("price-validation@ucdconnect.ie");
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Price Validation Student");
        user.setSchool(school);
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));

        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryId": %d,
                          "title": "Free notebook",
                          "description": "This request intentionally has an invalid price.",
                          "condition": "PRE_OWNED_GOOD",
                          "exchangeType": "FREE",
                          "priceAmount": 1.00
                        }
                        """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createItem_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryId": 1,
                          "title": "Unauthenticated item",
                          "description": "This request must be rejected before item creation.",
                          "condition": "PRE_OWNED_GOOD",
                          "exchangeType": "FREE"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void createItem_rejectsSellItemWithoutPrice() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        Category category = categoryRepository.findByActiveTrueOrderByNameAsc()
                .getFirst();

        User user = new User();
        user.setEmail("missing-price@ucdconnect.ie");
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Missing Price Student");
        user.setSchool(school);
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(user.getEmail())
        );

        mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "categoryId": %d,
                              "title": "Laptop for sale",
                              "description": "This request intentionally has no price.",
                              "condition": "PRE_OWNED_GOOD",
                              "exchangeType": "SELL"
                            }
                            """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createItem_rejectsInactiveCategory() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        Category inactiveCategory = new Category();
        inactiveCategory.setName("Inactive Test Category");
        inactiveCategory.setSlug("inactive-test-category");
        inactiveCategory.setActive(false);
        inactiveCategory = categoryRepository.saveAndFlush(inactiveCategory);

        User user = new User();
        user.setEmail("inactive-category@ucdconnect.ie");
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Inactive Category Student");
        user.setSchool(school);
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(user.getEmail())
        );

        mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "categoryId": %d,
                              "title": "Item in an inactive category",
                              "description": "This request must not create a listing.",
                              "condition": "PRE_OWNED_GOOD",
                              "exchangeType": "FREE"
                            }
                            """.formatted(inactiveCategory.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createItem_rejectsUnknownCondition() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User user = new User();
        user.setEmail("invalid-condition@ucdconnect.ie");
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Invalid Condition Student");
        user.setSchool(school);
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(user.getEmail())
        );

        mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "categoryId": %d,
                              "title": "Unknown-condition item",
                              "description": "This condition must be rejected.",
                              "condition": "DAMAGED",
                              "exchangeType": "FREE"
                            }
                            """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
