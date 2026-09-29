-- ============================================================
-- SEED DATA - CHI DUNG DE TEST O MOI TRUONG DEV/LOCAL
-- Khong phai migration, khong dat ten Vx, khong chay tu dong
-- qua Flyway. Chay tay 1 lan sau khi da co du 10 bang (V1-V4).
-- ============================================================

DO $$
DECLARE
    branch_hadong_id   UUID;
    branch_caugiay_id  UUID;

    cat_rau_qua_id     UUID;
    cat_rau_cu_id      UUID;
    cat_cu_id          UUID;
    cat_rau_nem_id     UUID;
    cat_trai_cay_id    UUID;
    cat_cam_buoi_id    UUID;
    cat_thit_ca_id     UUID;
    cat_thit_id        UUID;
    cat_ca_id          UUID;
    cat_sua_id         UUID;

    brand_vinamilk_id  UUID;
    brand_thtruemilk_id UUID;

    prod_khoaitay_id   UUID;
    prod_carot_id      UUID;
    prod_hanhla_id     UUID;
    prod_cam_id        UUID;
    prod_buoi_id       UUID;
    prod_thitbo_id     UUID;
    prod_thitheo_id    UUID;
    prod_caloc_id      UUID;
    prod_suavinamilk_id UUID;
    prod_suathtrue_id  UUID;
BEGIN
    -- ---------- CHI NHANH ----------
    -- Toa do that o Ha Dong va Cau Giay, Ha Noi - dung de test API
    -- /api/branches/nearby tinh khoang cach cho dung nghia.
    INSERT INTO branches (name, address, lat, lng, delivery_radius_km, is_warehouse, phone, opening_hours, status)
    VALUES ('UrbanMart Ha Dong', 'So 339 Quoc lo 70B, Ha Dong, Ha Noi', 20.9721, 105.7797, 8, false, '02412345678', '{"mon_sun":"7:00-22:00"}', 'ACTIVE')
    RETURNING id INTO branch_hadong_id;

    INSERT INTO branches (name, address, lat, lng, delivery_radius_km, is_warehouse, phone, opening_hours, status)
    VALUES ('UrbanMart Cau Giay', 'So 10 Nguyen Trai, Cau Giay, Ha Noi', 21.0333, 105.7942, 6, false, '02423456789', '{"mon_sun":"7:00-22:00"}', 'ACTIVE')
    RETURNING id INTO branch_caugiay_id;

    -- ---------- DANH MUC (cay 3 cap, giong vi du JSON API ban dua) ----------
    INSERT INTO categories (name) VALUES ('Rau cu, trai cay') RETURNING id INTO cat_rau_qua_id;
    INSERT INTO categories (name, parent_id) VALUES ('Rau cu', cat_rau_qua_id) RETURNING id INTO cat_rau_cu_id;
    INSERT INTO categories (name, parent_id) VALUES ('Cu', cat_rau_cu_id) RETURNING id INTO cat_cu_id;
    INSERT INTO categories (name, parent_id) VALUES ('Rau nem, rau thom', cat_rau_cu_id) RETURNING id INTO cat_rau_nem_id;
    INSERT INTO categories (name, parent_id) VALUES ('Trai cay', cat_rau_qua_id) RETURNING id INTO cat_trai_cay_id;
    INSERT INTO categories (name, parent_id) VALUES ('Cam, buoi, quyt', cat_trai_cay_id) RETURNING id INTO cat_cam_buoi_id;

    INSERT INTO categories (name) VALUES ('Thit, ca, trung, hai san') RETURNING id INTO cat_thit_ca_id;
    INSERT INTO categories (name, parent_id) VALUES ('Thit', cat_thit_ca_id) RETURNING id INTO cat_thit_id;
    INSERT INTO categories (name, parent_id) VALUES ('Ca', cat_thit_ca_id) RETURNING id INTO cat_ca_id;

    INSERT INTO categories (name) VALUES ('Sua va san pham tu sua') RETURNING id INTO cat_sua_id;

    -- ---------- THUONG HIEU ----------
    INSERT INTO brands (name) VALUES ('Vinamilk') RETURNING id INTO brand_vinamilk_id;
    INSERT INTO brands (name) VALUES ('TH true MILK') RETURNING id INTO brand_thtruemilk_id;

    -- ---------- SAN PHAM ----------
    -- Rau cu qua - khong co thuong hieu (brand_id = NULL)
    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP001', 'Khoai tay Da Lat', cat_cu_id, 'kg', 25000, 'ACTIVE')
    RETURNING id INTO prod_khoaitay_id;

    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP002', 'Ca rot Da Lat', cat_cu_id, 'kg', 20000, 'ACTIVE')
    RETURNING id INTO prod_carot_id;

    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP003', 'Hanh la', cat_rau_nem_id, 'bo', 5000, 'ACTIVE')
    RETURNING id INTO prod_hanhla_id;

    -- Cam
    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP004', 'Cam sanh', cat_cam_buoi_id, 'kg', 35000, 'ACTIVE')
    RETURNING id INTO prod_cam_id;

    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP005', 'Buoi da xanh', cat_cam_buoi_id, 'qua', 45000, 'ACTIVE')
    RETURNING id INTO prod_buoi_id;

    -- Thit, ca
    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP006', 'Thit bo than', cat_thit_id, 'kg', 250000, 'ACTIVE')
    RETURNING id INTO prod_thitbo_id;

    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP007', 'Thit heo ba chi', cat_thit_id, 'kg', 120000, 'ACTIVE')
    RETURNING id INTO prod_thitheo_id;

    INSERT INTO products (sku, name, category_id, unit, base_price, status)
    VALUES ('SP008', 'Ca loc', cat_ca_id, 'kg', 80000, 'ACTIVE')
    RETURNING id INTO prod_caloc_id;

    -- Sua - co thuong hieu
    INSERT INTO products (sku, name, category_id, brand_id, unit, base_price, status)
    VALUES ('SP009', 'Sua tuoi Vinamilk 1L', cat_sua_id, brand_vinamilk_id, 'hop', 32000, 'ACTIVE')
    RETURNING id INTO prod_suavinamilk_id;

    INSERT INTO products (sku, name, category_id, brand_id, unit, base_price, status)
    VALUES ('SP010', 'Sua tuoi TH true MILK 1L', cat_sua_id, brand_thtruemilk_id, 'hop', 34000, 'ACTIVE')
    RETURNING id INTO prod_suathtrue_id;

    -- ---------- TON KHO THEO CHI NHANH ----------
    -- Ca 2 chi nhanh deu co du 10 san pham, chi khac nhau ve so luong ton
    -- (thuc te hon la ep giong het tung con so, nhung khong con truong hop
    -- thieu han 1 san pham o chi nhanh nay ma chi nhanh kia co).
    INSERT INTO branch_inventory (branch_id, product_id, stock_quantity) VALUES
        (branch_hadong_id, prod_khoaitay_id, 50),
        (branch_hadong_id, prod_carot_id, 40),
        (branch_hadong_id, prod_hanhla_id, 100),
        (branch_hadong_id, prod_cam_id, 40),
        (branch_hadong_id, prod_buoi_id, 20),
        (branch_hadong_id, prod_thitbo_id, 15),
        (branch_hadong_id, prod_thitheo_id, 30),
        (branch_hadong_id, prod_caloc_id, 10),
        (branch_hadong_id, prod_suavinamilk_id, 60),
        (branch_hadong_id, prod_suathtrue_id, 35);

    INSERT INTO branch_inventory (branch_id, product_id, stock_quantity) VALUES
        (branch_caugiay_id, prod_khoaitay_id, 35),
        (branch_caugiay_id, prod_carot_id, 45),
        (branch_caugiay_id, prod_hanhla_id, 80),
        (branch_caugiay_id, prod_cam_id, 25),
        (branch_caugiay_id, prod_buoi_id, 18),
        (branch_caugiay_id, prod_thitbo_id, 12),
        (branch_caugiay_id, prod_thitheo_id, 22),
        (branch_caugiay_id, prod_caloc_id, 8),
        (branch_caugiay_id, prod_suavinamilk_id, 50),
        (branch_caugiay_id, prod_suathtrue_id, 40);

    RAISE NOTICE 'Seed data OK: 2 chi nhanh, 10 danh muc, 2 thuong hieu, 10 san pham, 20 dong ton kho (moi chi nhanh du 10 san pham).';
END $$;
