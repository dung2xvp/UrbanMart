-- ============================================================
-- V17: DANH GIA DON HANG VA DANH GIA SAN PHAM
-- Chi khach so huu don COMPLETED moi duoc danh gia.
-- branch_id duoc sao chep tu don hang de chi nhanh/admin loc nhanh.
-- ============================================================

CREATE TABLE order_reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    customer_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id           UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    delivery_rating     INTEGER CHECK (delivery_rating BETWEEN 1 AND 5),
    service_rating      INTEGER NOT NULL CHECK (service_rating BETWEEN 1 AND 5),
    comment             TEXT,
    images              JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_reviews_branch_created ON order_reviews (branch_id, created_at DESC);
-- delivery_rating NULL voi don PICKUP (khach tu den lay, khong co giao hang).

CREATE TABLE product_reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    order_id            UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id           UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    rating              INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment             TEXT,
    images              JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_product_reviews_order_product UNIQUE (order_id, product_id)
);
CREATE INDEX idx_product_reviews_product_created ON product_reviews (product_id, created_at DESC);
CREATE INDEX idx_product_reviews_branch_created ON product_reviews (branch_id, created_at DESC);
