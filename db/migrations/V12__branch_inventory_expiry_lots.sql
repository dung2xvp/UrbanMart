ALTER TYPE inventory_movement_type ADD VALUE IF NOT EXISTS 'RECEIVED';

CREATE TABLE branch_inventory_lots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id    UUID NOT NULL REFERENCES branch_inventory(id) ON DELETE CASCADE,
    quantity        INTEGER NOT NULL CHECK (quantity >= 0),
    expiry_date     DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_branch_inventory_lots_inventory_expiry
    ON branch_inventory_lots (inventory_id, expiry_date, created_at);

INSERT INTO branch_inventory_lots (inventory_id, quantity, expiry_date)
SELECT id, stock_quantity, NULL
FROM branch_inventory
WHERE stock_quantity > 0;
