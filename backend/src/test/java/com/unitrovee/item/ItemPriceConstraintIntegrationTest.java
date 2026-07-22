package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.item.domain.ExchangeType;
import com.unitrovee.item.domain.Item;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class ItemPriceConstraintIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void saveAndFlush_rejectsFreeItemWithPrice() {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();
        Category category = categoryRepository.findByActiveTrueOrderByNameAsc().getFirst();

        User owner = new User();
        owner.setEmail("invalid-price@ucdconnect.ie");
        owner.setPasswordHash("test-password-hash");
        owner.setDisplayName("Constraint Tester");
        owner.setSchool(school);
        userRepository.saveAndFlush(owner);

        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Free notebook");
        item.setDescription("This intentionally has an invalid price.");
        item.setCondition("GOOD");
        item.setExchangeType(ExchangeType.FREE);
        item.setPriceAmount(new BigDecimal("1.00"));

        assertThatThrownBy(() -> itemRepository.saveAndFlush(item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_rejectsSellItemWithoutPrice() {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        Category category = categoryRepository.findByActiveTrueOrderByNameAsc()
                .getFirst();

        User owner = new User();
        owner.setEmail("missing-sell-price@ucdconnect.ie");
        owner.setPasswordHash("test-password-hash");
        owner.setDisplayName("Missing Price Tester");
        owner.setSchool(school);
        userRepository.saveAndFlush(owner);

        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(school);
        item.setCategory(category);
        item.setTitle("Laptop for sale");
        item.setDescription("This intentionally has no price.");
        item.setCondition("GOOD");
        item.setExchangeType(ExchangeType.SELL);

        assertThatThrownBy(() -> itemRepository.saveAndFlush(item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
