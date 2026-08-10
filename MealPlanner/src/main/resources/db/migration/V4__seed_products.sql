-- Starter product reference set (~35 items). Nutrition values are reasonable approximate
-- references for development/seed purposes, not a verified USDA import. The full 200-300
-- item catalogue (section 6, 7.1a) is a later content task, not part of infra setup.
-- Columns: canonical_name, category, kcal, protein, fat, carbs (per 100g), default_shelf_life_days, grams_per_piece, is_staple

INSERT INTO product (canonical_name, category, kcal_per_100g, protein_per_100g, fat_per_100g, carbs_per_100g, default_shelf_life_days, grams_per_piece, is_staple) VALUES
    ('salt',            'STAPLE',    0,   0,    0,    0,    3650, NULL, TRUE),
    ('black pepper',    'STAPLE',    251, 10.4, 3.3,  64,   1825, NULL, TRUE),
    ('olive oil',       'STAPLE',    884, 0,    100,  0,    730,  NULL, TRUE),
    ('sugar',           'STAPLE',    387, 0,    0,    100,  3650, NULL, FALSE),
    ('water',           'STAPLE',    0,   0,    0,    0,    3650, NULL, TRUE),

    ('milk',            'DAIRY',     42,  3.4,  1,    5,    7,    NULL, FALSE),
    ('butter',          'DAIRY',     717, 0.9,  81,   0.1,  60,   NULL, FALSE),
    ('egg',              'DAIRY',     143, 12.6, 9.5,  0.7,  21,   50,   FALSE),
    ('plain yogurt',    'DAIRY',     61,  3.5,  3.3,  4.7,  14,   NULL, FALSE),
    ('quark',           'DAIRY',     68,  12,   0.2,  4,    10,   NULL, FALSE),
    ('cheddar cheese',  'DAIRY',     403, 25,   33,   1.3,  30,   NULL, FALSE),

    ('onion',           'PRODUCE',   40,  1.1,  0.1,  9.3,  30,   110,  FALSE),
    ('garlic',          'PRODUCE',   149, 6.4,  0.5,  33,   90,   5,    FALSE),
    ('tomato',          'PRODUCE',   18,  0.9,  0.2,  3.9,  7,    120,  FALSE),
    ('potato',          'PRODUCE',   77,  2,    0.1,  17,   30,   170,  FALSE),
    ('carrot',          'PRODUCE',   41,  0.9,  0.2,  10,   21,   60,   FALSE),
    ('bell pepper',     'PRODUCE',   31,  1,    0.3,  6,    14,   120,  FALSE),
    ('zucchini',        'PRODUCE',   17,  1.2,  0.3,  3.1,  7,    200,  FALSE),
    ('spinach',         'PRODUCE',   23,  2.9,  0.4,  3.6,  5,    NULL, FALSE),
    ('mushroom',        'PRODUCE',   22,  3.1,  0.3,  3.3,  7,    NULL, FALSE),

    ('rice',            'GRAIN',     130, 2.7,  0.3,  28,   730,  NULL, FALSE),
    ('pasta',           'GRAIN',     131, 5,    1.1,  25,   730,  NULL, FALSE),
    ('rolled oats',     'GRAIN',     389, 16.9, 6.9,  66,   365,  NULL, FALSE),
    ('wheat flour',     'GRAIN',     364, 10.3, 1,    76,   365,  NULL, FALSE),
    ('bread',           'BAKERY',    265, 9,    3.2,  49,   5,    NULL, FALSE),

    ('lentils',         'LEGUME',    116, 9,    0.4,  20,   730,  NULL, FALSE),
    ('chickpeas',       'LEGUME',    164, 8.9,  2.6,  27,   730,  NULL, FALSE),
    ('black beans',     'LEGUME',    132, 8.9,  0.5,  24,   730,  NULL, FALSE),

    ('chicken breast',  'MEAT',      165, 31,   3.6,  0,    3,    NULL, FALSE),
    ('ground beef',     'MEAT',      254, 17,   20,   0,    3,    NULL, FALSE),
    ('salmon',          'FISH',      208, 20,   13,   0,    2,    NULL, FALSE),
    ('canned tuna',     'FISH',      132, 29,   1,    0,    1095, NULL, FALSE),

    ('soy sauce',       'CONDIMENT', 53,  8,    0.1,  4.9,  730,  NULL, FALSE),
    ('vinegar',         'CONDIMENT', 18,  0,    0,    0.4,  1825, NULL, FALSE),
    ('tomato paste',    'CONDIMENT', 82,  4.3,  0.5,  19,   730,  NULL, FALSE);

INSERT INTO product_synonym (product_id, synonym)
SELECT id, synonym
FROM product
JOIN (VALUES
    ('quark', 'curd cheese'),
    ('quark', 'topfen'),
    ('zucchini', 'courgette'),
    ('bell pepper', 'capsicum')
) AS s(canonical_name, synonym) USING (canonical_name);
