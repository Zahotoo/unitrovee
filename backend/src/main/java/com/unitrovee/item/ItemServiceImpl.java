package com.unitrovee.item;

import com.unitrovee.category.CategoryRepository;
import com.unitrovee.category.domain.Category;
import com.unitrovee.common.exception.ResourceNotFoundException;
import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import com.unitrovee.item.dto.ItemCreateRequest;
import com.unitrovee.item.dto.ItemCreateResponse;
import com.unitrovee.item.mapper.ItemMapper;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemCreateResponse createItem(String authenticatedEmail, ItemCreateRequest request) {
        User owner = userRepository.findByEmailWithSchool(authenticatedEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (owner.getRole() != Role.STUDENT || !owner.isEmailVerified()) {
            throw new AccessDeniedException("Verified students only");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active category not found"));

        Item item = new Item();
        item.setOwner(owner);
        item.setSchool(owner.getSchool());
        item.setCategory(category);
        item.setTitle(request.title().trim());
        item.setDescription(request.description().trim());
        item.setCondition(request.condition());
        item.setExchangeType(request.exchangeType());
        item.setPriceAmount(request.priceAmount());
        item.setLocationHint(
                request.locationHint() == null ? null : request.locationHint().trim()
        );

        item.setStatus(ItemStatus.DRAFT);

        Item savedItem = itemRepository.save(item);
        return itemMapper.toCreateResponse(savedItem);
    }
}
