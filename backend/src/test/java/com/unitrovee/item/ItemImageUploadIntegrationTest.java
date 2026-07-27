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
import com.unitrovee.security.CustomUserDetailsService;
import com.unitrovee.security.JwtService;
import com.unitrovee.storage.StorageService;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class ItemImageUploadIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemImageRepository itemImageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;
    @Autowired private StorageService storageService;

    @AfterEach
    void deleteStoredFiles() throws Exception {
        itemImageRepository.findAll().forEach(image -> storageService.delete(image.getStorageKey()));
    }

    @Test
    void uploadImage_storesMetadataAndMakesTheFileRetrievable() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        byte[] imageBytes = PNG_SIGNATURE.clone();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "desk-lamp.png",
                MediaType.IMAGE_PNG_VALUE,
                imageBytes
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(image)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.url").isString())
                .andExpect(jsonPath("$.data.sortOrder").value(0))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist());

        List<ItemImage> images = itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId());

        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getSortOrder()).isZero();

        String filePath = URI.create(
                storageService.getUrl(images.getFirst().getStorageKey())
        ).getPath();

        mockMvc.perform(get(filePath))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void uploadImage_rejectsNonImageFile() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "notes.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is not an image.".getBytes()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(file)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).isEmpty();
    }

    @Test
    void uploadImage_rejectsWhenItemAlreadyHasEightImages() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        for (int sortOrder = 0; sortOrder < 8; sortOrder++) {
            ItemImage existingImage = new ItemImage();
            existingImage.setItem(item);
            existingImage.setStorageKey("items/test/existing-" + sortOrder + ".png");
            existingImage.setSortOrder(sortOrder);
            itemImageRepository.saveAndFlush(existingImage);
        }

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "ninth-image.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_SIGNATURE.clone()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(image)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).hasSize(8);
    }

    @Test
    void uploadImage_rejectsImageLargerThanFiveMegabytes() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        byte[] oversizedImageBytes = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(
                PNG_SIGNATURE,
                0,
                oversizedImageBytes,
                0,
                PNG_SIGNATURE.length
        );

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "oversized-image.png",
                MediaType.IMAGE_PNG_VALUE,
                oversizedImageBytes
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(image)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).isEmpty();
    }

    @Test
    void uploadImage_rejectsFilenameThatDoesNotMatchContentType() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "not-really-an-image.html",
                MediaType.IMAGE_PNG_VALUE,
                PNG_SIGNATURE.clone()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(image)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).isEmpty();
    }

    @Test
    void uploadImage_rejectsNonOwner() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        User nonOwner = createVerifiedStudent(school, "image-upload-non-owner");
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(nonOwner.getEmail()));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "attempted-upload.png",
                MediaType.IMAGE_PNG_VALUE,
                "not-owner-upload".getBytes()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(image)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).isEmpty();
    }

    @Test
    void uploadImage_rejectsReservedItem() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        item.setStatus(ItemStatus.RESERVED);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "reserved-item.png",
                MediaType.IMAGE_PNG_VALUE,
                "reserved-item-upload".getBytes()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId()))
                .isEmpty();
    }

    @Test
    void uploadImage_allowsAvailableItemOwnedByAuthenticatedUser() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        item.setStatus(ItemStatus.AVAILABLE);
        itemRepository.saveAndFlush(item);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "available-item.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_SIGNATURE.clone()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sortOrder").value(0));

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId()))
                .hasSize(1);
    }

    @Test
    void uploadImage_assignsNextSortOrder() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        ItemImage firstImage = new ItemImage();
        firstImage.setItem(item);
        firstImage.setStorageKey("items/test/existing-cover.png");
        firstImage.setSortOrder(0);
        itemImageRepository.saveAndFlush(firstImage);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "second-image.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_SIGNATURE.clone()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sortOrder").value(1));

        List<ItemImage> images = itemImageRepository
                .findByItemIdOrderBySortOrderAsc(item.getId());

        assertThat(images).hasSize(2);
        assertThat(images.get(0).getSortOrder()).isZero();
        assertThat(images.get(1).getSortOrder()).isEqualTo(1);
    }

    @Test
    void uploadImage_requiresAuthentication() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "unauthenticated-upload.png",
                MediaType.IMAGE_PNG_VALUE,
                "unauthenticated-upload".getBytes()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", 999L)
                        .file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadImage_rejectsRequestWithoutImagePart() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(owner.getEmail())
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadImage_rejectsFileWithInvalidImageSignature() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();
        User owner = createVerifiedStudent(school);
        Item item = createEditableItem(owner, school, category);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(owner.getEmail()));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "fake-image.png",
                MediaType.IMAGE_PNG_VALUE,
                "<html>This is HTML, not a PNG image.</html>".getBytes()
        );

        mockMvc.perform(multipart("/api/items/{itemId}/images", item.getId())
                .file(image)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        assertThat(itemImageRepository.findByItemIdOrderBySortOrderAsc(item.getId())).isEmpty();
    }

    private User createVerifiedStudent(School school) {
        return createVerifiedStudent(school, "image-upload-owner");
    }

    private User createVerifiedStudent(School school, String emailPrefix) {
        User user = new User();
        user.setEmail(emailPrefix + "@" + school.getEmailDomain());
        user.setPasswordHash("$2a$test");
        user.setDisplayName("Image Upload Owner");
        user.setSchool(school);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Item createEditableItem(User owner, School school, Category category) {
        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Desk lamp");
        item.setDescription("A working lamp for upload testing.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("12.00"));
        item.setLocationHint("UCD Library");
        item.setStatus(ItemStatus.DRAFT);
        return itemRepository.saveAndFlush(item);
    }
}
