-- ============================================================
-- V18: TAG THUC PHAM VA QUY TAC MON AN KY NHAU
-- Quy tac dat theo cap tag (vd HAI_SAN - VITAMIN_C) thay vi cap san pham,
-- san pham duoc gan tag qua product_food_tags.
-- ============================================================

CREATE TABLE food_tags (
    code            VARCHAR(50) PRIMARY KEY CHECK (code ~ '^[A-Z0-9_]+$'),
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product_food_tags (
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tag_code        VARCHAR(50) NOT NULL REFERENCES food_tags(code) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_code)
);
CREATE INDEX idx_product_food_tags_tag ON product_food_tags (tag_code);

CREATE TABLE food_incompatibilities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_a           VARCHAR(50) NOT NULL REFERENCES food_tags(code) ON DELETE CASCADE,
    tag_b           VARCHAR(50) NOT NULL REFERENCES food_tags(code) ON DELETE CASCADE,
    severity        VARCHAR(20) NOT NULL CHECK (
                        severity IN ('LOW', 'MEDIUM', 'HIGH', 'DANGEROUS')
                    ),
    title           VARCHAR(200) NOT NULL,
    effect          TEXT NOT NULL,
    recommendation  TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Quy tac doi xung: luon luu tag_a < tag_b (so sanh nhi phan) de (A,B) va (B,A) khong trung.
    CONSTRAINT chk_food_incompatibilities_order CHECK (tag_a COLLATE "C" < tag_b COLLATE "C"),
    CONSTRAINT uk_food_incompatibilities_pair UNIQUE (tag_a, tag_b)
);
CREATE INDEX idx_food_incompatibilities_tag_b ON food_incompatibilities (tag_b);
