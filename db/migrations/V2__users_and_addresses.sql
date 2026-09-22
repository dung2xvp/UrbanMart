-- ============================================================
-- V2: NGUOI DUNG & DIA CHI
-- Phuc vu API: 1.Dang ky, 2.Dang nhap, 3.Quen mat khau,
--              4.Doi mat khau, 6.Sua thong tin, 10.CRUD dia chi
-- ============================================================

CREATE TABLE users (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name               VARCHAR(150) NOT NULL,
    phone                   VARCHAR(20) NOT NULL UNIQUE,
    email                   VARCHAR(150) UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    birth_date              DATE,
    gender                  VARCHAR(10),
    role                    user_role NOT NULL DEFAULT 'CUSTOMER',
    status                  user_status NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts   SMALLINT NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Dang nhap bang so dien thoai (unique, bat buoc), email chi la tuy chon.
-- OTP dang ky/quen mat khau luu tam o Redis (TTL vai phut), khong can bang DB.
-- failed_login_attempts >= 5 thi BE set locked_until de khoa tam thoi.

CREATE TABLE addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_name  VARCHAR(150) NOT NULL,
    phone           VARCHAR(20) NOT NULL,
    line            VARCHAR(255) NOT NULL,
    ward            VARCHAR(100),
    district        VARCHAR(100),
    city            VARCHAR(100),
    lat             NUMERIC(9,6),
    lng             NUMERIC(9,6),
    is_default      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_addresses_user ON addresses(user_id);
