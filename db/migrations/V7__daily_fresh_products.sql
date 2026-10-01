ALTER TABLE products
    ADD COLUMN is_daily_fresh BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_products_daily_fresh
    ON products (is_daily_fresh)
    WHERE is_daily_fresh = true;
