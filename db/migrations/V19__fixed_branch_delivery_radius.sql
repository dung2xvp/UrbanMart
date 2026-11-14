UPDATE branches
SET delivery_radius_km = 40;

ALTER TABLE branches
    ALTER COLUMN delivery_radius_km SET DEFAULT 40,
    ALTER COLUMN delivery_radius_km SET NOT NULL,
    ADD CONSTRAINT chk_branches_delivery_radius_40
        CHECK (delivery_radius_km = 40);
