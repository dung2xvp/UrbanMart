ALTER TYPE order_status
    ADD VALUE IF NOT EXISTS 'DELIVERED_PENDING_CONFIRMATION'
    AFTER 'DELIVERY_FAILED';

ALTER TABLE orders
    ADD COLUMN delivered_at TIMESTAMPTZ;

CREATE INDEX idx_orders_delivery_confirmation_deadline
    ON orders (delivered_at)
    WHERE delivered_at IS NOT NULL;
