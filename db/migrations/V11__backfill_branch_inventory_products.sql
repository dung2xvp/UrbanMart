INSERT INTO branch_inventory (
    branch_id,
    product_id,
    stock_quantity,
    updated_at
)
SELECT
    branches.id,
    products.id,
    0,
    now()
FROM branches
CROSS JOIN products
ON CONFLICT (branch_id, product_id) DO NOTHING;
