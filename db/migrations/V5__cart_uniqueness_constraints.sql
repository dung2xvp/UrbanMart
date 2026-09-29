-- Ensure each user has at most one cart per branch and each product appears
-- at most once in a cart.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM carts
        GROUP BY user_id, branch_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot add cart uniqueness constraint: duplicate carts exist for a user and branch';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM cart_items
        GROUP BY cart_id, product_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot add cart item uniqueness constraint: duplicate products exist in a cart';
    END IF;
END $$;

ALTER TABLE carts
    ADD CONSTRAINT uk_carts_user_branch UNIQUE (user_id, branch_id);

ALTER TABLE cart_items
    ADD CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id);
