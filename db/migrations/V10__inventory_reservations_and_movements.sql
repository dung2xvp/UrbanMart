ALTER TABLE branch_inventory
    ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN min_stock_level INTEGER NOT NULL DEFAULT 10,
    ADD CONSTRAINT chk_branch_inventory_reservation
        CHECK (
            reserved_quantity >= 0
            AND reserved_quantity <= stock_quantity
        ),
    ADD CONSTRAINT chk_branch_inventory_min_stock
        CHECK (min_stock_level >= 0);

CREATE TYPE inventory_movement_type AS ENUM (
    'RESERVED',
    'RELEASED',
    'DISPATCHED',
    'RETURNED',
    'ADJUSTMENT'
);

CREATE TABLE inventory_movements (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id               UUID NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    product_id              UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    order_id                UUID REFERENCES orders(id) ON DELETE SET NULL,
    performed_by            UUID REFERENCES users(id) ON DELETE SET NULL,
    movement_type           inventory_movement_type NOT NULL,
    on_hand_delta           INTEGER NOT NULL DEFAULT 0,
    reserved_delta          INTEGER NOT NULL DEFAULT 0,
    reason                  VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_movement_delta
        CHECK (on_hand_delta <> 0 OR reserved_delta <> 0)
);

CREATE INDEX idx_inventory_movements_branch_product_created
    ON inventory_movements (branch_id, product_id, created_at DESC);
CREATE INDEX idx_inventory_movements_order
    ON inventory_movements (order_id)
    WHERE order_id IS NOT NULL;
