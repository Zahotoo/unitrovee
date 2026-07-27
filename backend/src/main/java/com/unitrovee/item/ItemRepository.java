package com.unitrovee.item;

import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    @EntityGraph(attributePaths = {"school", "category"})
    Page<Item> findAll(Specification<Item> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "owner.school", "school", "category"})
    Optional<Item> findByIdAndStatus(Long itemId, ItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Item item where item.id = :itemId")
    Optional<Item> findByIdForUpdate(@Param("itemId") Long itemId);
}
