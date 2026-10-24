ALTER TABLE promotions
    ADD COLUMN title VARCHAR(150),
    ADD COLUMN description TEXT,
    ADD COLUMN is_first_order BOOLEAN NOT NULL DEFAULT false;

UPDATE promotions
SET title = code
WHERE title IS NULL;

ALTER TABLE promotions
    ALTER COLUMN title SET NOT NULL;

CREATE TABLE promotion_applicable_categories (
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    PRIMARY KEY (promotion_id, category_id)
);

CREATE TABLE promotion_special_days (
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    day_of_month INTEGER NOT NULL,
    PRIMARY KEY (promotion_id, day_of_month),
    CONSTRAINT chk_promotion_special_day CHECK (day_of_month BETWEEN 1 AND 31)
);
