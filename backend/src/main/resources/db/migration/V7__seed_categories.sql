-- Categories are reference data: seed once through Flyway, never hard-code them in the frontend
INSERT INTO categories (name, slug, active, created_at, updated_at)
VALUES
    ('Textbooks',   'textbooks',   TRUE, NOW(), NOW()),
    ('Electronics', 'electronics', TRUE, NOW(), NOW()),
    ('Furniture',   'furniture',   TRUE, NOW(), NOW()),
    ('Kitchen',     'kitchen',     TRUE, NOW(), NOW()),
    ('Clothing',    'clothing',    TRUE, NOW(), NOW()),
    ('Bikes',       'bikes',       TRUE, NOW(), NOW()),
    ('Sports',      'sports',      TRUE, NOW(), NOW()),
    ('Free Items',  'free-items',  TRUE, NOW(), NOW()),
    ('Other',       'other',       TRUE, NOW(), NOW());