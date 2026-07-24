package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import com.unitrovee.item.domain.ItemCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class ItemRepositoryTest extends AbstractIntegrationTest {

    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void saveAndFlush_persistsSellItemWithForeignKeysAndVersion() {

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = new User();
        owner.setEmail("item-owner@ucdconnect.ie");
        owner.setPasswordHash("test-password-hash");
        owner.setDisplayName("Item Owner");
        owner.setSchool(school);
        userRepository.saveAndFlush(owner);

        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Calculus textbook");
        item.setDescription("Clean copy, with no missing pages.");
        item.setCondition(ItemCondition.PRE_OWNED_GOOD);
        item.setExchangeType(ExchangeType.SELL);
        item.setPriceAmount(new BigDecimal("25.00"));
        item.setStatus(ItemStatus.DRAFT);
        item.setLocationHint("UCD campus");

        Item savedItem = itemRepository.saveAndFlush(item);

        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(savedItem.getSchool().getId()).isEqualTo(school.getId());
        assertThat(savedItem.getCategory().getId()).isEqualTo(category.getId());
        assertThat(savedItem.getPriceAmount()).isEqualByComparingTo("25.00");
        assertThat(savedItem.getStatus()).isEqualTo(ItemStatus.DRAFT);
        assertThat(savedItem.getVersion()).isZero();
    }
}
