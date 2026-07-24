-- restrict listing conditions to the application ItemCondition enum values
ALTER TABLE items
    ADD CONSTRAINT chk_items_condition
    CHECK (condition IN (
        'NEW',
        'NEW_OPEN_BOX',
        'NEW_WITH_DEFECTS',
        'PRE_OWNED_EXCELLENT',
        'PRE_OWNED_GOOD',
        'PRE_OWNED_FAIR',
        'REFURBISHED',
        'FOR_PARTS_OR_NOT_WORKING'
    ));