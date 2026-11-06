-- ============================================================
-- V16: MON AN (RECIPE) VA NGUYEN LIEU LIEN KET SAN PHAM
-- Phuc vu API tuan 9: CRUD mon an (admin), danh sach mon an,
--                      popup "Chon mua nguyen lieu" theo chi nhanh
-- ============================================================

CREATE TABLE recipes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) NOT NULL UNIQUE,
    description     TEXT,
    image_url       VARCHAR(500),
    category        VARCHAR(30) NOT NULL CHECK (
                        category IN (
                            'VEGETABLE', 'MEAT', 'SEAFOOD', 'SOUP',
                            'HOTPOT', 'VEGETARIAN', 'DESSERT', 'OTHER'
                        )
                    ),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_recipes_active_category ON recipes (is_active, category);

CREATE TABLE recipe_ingredients (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id           UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    product_id          UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    is_required         BOOLEAN NOT NULL DEFAULT true,
    ingredient_group    VARCHAR(20) NOT NULL CHECK (
                            ingredient_group IN ('MAIN', 'SPICE', 'SIDE')
                        ),
    note                VARCHAR(255),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_recipe_ingredients_recipe_product UNIQUE (recipe_id, product_id)
);
CREATE INDEX idx_recipe_ingredients_product ON recipe_ingredients (product_id);
-- ingredient_group dung de nhom tren popup (Nguyen lieu chinh / Gia vi / Nguyen lieu phu);
-- note chi la ghi chu tu do, khong dung de nhom.
