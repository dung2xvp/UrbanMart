-- Tai khoan ADMIN mau chi dung cho moi truong dev/local.
-- Chay sau migration V2; co the chay lai de dat lai mat khau test.
INSERT INTO users (full_name, phone, password_hash, role)
VALUES (
    'UrbanMart Admin',
    '0900000000',
    crypt('Admin@123', gen_salt('bf')),
    'ADMIN'
)
ON CONFLICT (phone) DO UPDATE
SET full_name = EXCLUDED.full_name,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    status = 'ACTIVE';
