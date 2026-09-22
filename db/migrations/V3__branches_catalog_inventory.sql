-- ============================================================
-- V3: CHI NHANH, DANH MUC, THUONG HIEU, SAN PHAM, TON KHO
-- Phuc vu API: 5.San pham theo chi nhanh, 8.Danh sach theo danh
--              muc (phan trang), 11.Tim kiem, 12.Danh muc san pham
-- ============================================================

CREATE TABLE branches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id          UUID UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    name                VARCHAR(150) NOT NULL,
    address             VARCHAR(255) NOT NULL,
    lat                 NUMERIC(9,6),
    lng                 NUMERIC(9,6),
    delivery_radius_km  NUMERIC(5,2) DEFAULT 5,
    is_warehouse        BOOLEAN NOT NULL DEFAULT false,
    phone               VARCHAR(20),
    opening_hours       JSONB,
    status              branch_status NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- account_id: 1 tai khoan (role = 'BRANCH') dung chung cho ca chi nhanh.
-- is_warehouse: kho tong, khong ban truc tiep cho khach (dung o migration sau).

CREATE TABLE categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    parent_id       UUID REFERENCES categories(id) ON DELETE SET NULL
);
CREATE INDEX idx_categories_parent ON categories(parent_id);

CREATE TABLE brands (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    logo_url        VARCHAR(500)
);

CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku             VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category_id     UUID REFERENCES categories(id) ON DELETE SET NULL,
    brand_id        UUID REFERENCES brands(id) ON DELETE SET NULL,
    unit            VARCHAR(20) NOT NULL,
    base_price      NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),
    sale_price      NUMERIC(12,2) CHECK (sale_price >= 0 AND sale_price <= base_price),
    sale_start_at   TIMESTAMPTZ,
    sale_end_at     TIMESTAMPTZ,
    image_url       VARCHAR(500),
    embedding       VECTOR(768),
    status          product_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand_id);
-- "Dang khuyen mai" = sale_price IS NOT NULL AND now() BETWEEN sale_start_at AND sale_end_at.

CREATE TABLE branch_inventory (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    stock_quantity  INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, product_id)
);
CREATE INDEX idx_branch_inventory_branch ON branch_inventory(branch_id);
CREATE INDEX idx_branch_inventory_product ON branch_inventory(product_id);
-- Gia dung chung tu products.base_price/sale_price, bang nay chi luu ton kho.
