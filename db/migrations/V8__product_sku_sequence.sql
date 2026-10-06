CREATE SEQUENCE product_sku_seq;

SELECT setval(
    'product_sku_seq',
    COALESCE(MAX(substring(sku FROM 3)::BIGINT), 0) + 1,
    false
)
FROM products
WHERE sku ~ '^SP[0-9]+$';
