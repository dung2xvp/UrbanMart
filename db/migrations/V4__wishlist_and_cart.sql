-- ============================================================
-- V4: YEU THICH & GIO HANG
-- Phuc vu API: 7.Yeu thich/khong yeu thich, 9.Xem danh sach yeu
--              thich, 13.Them vao gio, 14.Xem gio, 15.Doi so luong
-- ============================================================

CREATE TABLE wishlists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, product_id)
);
CREATE INDEX idx_wishlists_user ON wishlists(user_id);

CREATE TABLE carts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id       UUID NOT NULL REFERENCES branches(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id         UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL REFERENCES products(id),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)
);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
-- Gio hang gan voi 1 chi nhanh cu the (carts.branch_id) de tranh lan gia/ton
-- kho giua cac chi nhanh khi khach doi chi nhanh giua chung.
