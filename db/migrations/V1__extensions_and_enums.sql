-- ============================================================
-- V1: EXTENSIONS + ENUM can cho dot bang dau tien
-- (chi khai bao enum dung ngay o V2-V4, cac enum con lai se
--  them dan trong migration tuong ung khi lam toi API do)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- cho gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "vector";        -- pgvector, dung sau cho tim kiem ngu nghia

CREATE TYPE user_role      AS ENUM ('CUSTOMER','BRANCH','ADMIN');
CREATE TYPE user_status    AS ENUM ('ACTIVE','LOCKED');
CREATE TYPE branch_status  AS ENUM ('ACTIVE','CLOSED_TEMP');
CREATE TYPE product_status AS ENUM ('ACTIVE','DISCONTINUED');
