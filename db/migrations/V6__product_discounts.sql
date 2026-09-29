CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE product_discounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    discount_percent    NUMERIC(5,2) NOT NULL
                            CHECK (discount_percent > 0 AND discount_percent <= 100),
    starts_at           TIMESTAMPTZ NOT NULL,
    ends_at             TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_product_discounts_valid_period
        CHECK (starts_at < ends_at),
    CONSTRAINT ex_product_discounts_no_overlap
        EXCLUDE USING gist (
            product_id WITH =,
            (tstzrange(starts_at, ends_at, '[)')) WITH &&
        )
);

ALTER TABLE products
    DROP COLUMN IF EXISTS sale_price,
    DROP COLUMN IF EXISTS sale_start_at,
    DROP COLUMN IF EXISTS sale_end_at;
