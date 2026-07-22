package com.unitrovee.item;

import com.unitrovee.item.domain.Item;
import com.unitrovee.item.domain.ItemStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemDefaultsTest {

    @Test
    void newItem_defaultsToDraftStatus() {
        Item item = new Item();

        assertThat(item.getStatus()).isEqualTo(ItemStatus.DRAFT);
        assertThat(item.getVersion()).isZero();
        assertThat(item.getPriceAmount()).isNull();
    }
}
