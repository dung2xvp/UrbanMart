CREATE TYPE order_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SHIPPING',
    'READY_FOR_PICKUP',
    'DELIVERY_FAILED',
    'RETURNED',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE delivery_type AS ENUM ('DELIVERY', 'PICKUP');
CREATE TYPE payment_method AS ENUM ('COD', 'VNPAY');
CREATE TYPE payment_status AS ENUM ('UNPAID', 'PAID', 'FAILED', 'REFUNDED');
CREATE TYPE payment_attempt_status AS ENUM (
    'INITIATED',
    'SUCCEEDED',
    'FAILED',
    'CANCELLED'
);
CREATE TYPE order_actor AS ENUM ('CUSTOMER', 'BRANCH', 'ADMIN', 'SYSTEM');
CREATE TYPE promotion_type AS ENUM ('PERCENT', 'FIXED');
CREATE TYPE order_promotion_status AS ENUM ('APPLIED', 'RELEASED');

CREATE TABLE orders (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id             UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    branch_id               UUID NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    address_id              UUID REFERENCES addresses(id) ON DELETE SET NULL,
    order_code              VARCHAR(32) NOT NULL UNIQUE,
    client_request_id       UUID NOT NULL,
    payload_fingerprint     VARCHAR(64) NOT NULL,
    delivery_type           delivery_type NOT NULL,
    status                  order_status NOT NULL DEFAULT 'PENDING',
    payment_method          payment_method NOT NULL,
    payment_status          payment_status NOT NULL DEFAULT 'UNPAID',
    customer_full_name      VARCHAR(150) NOT NULL,
    customer_phone          VARCHAR(20) NOT NULL,
    customer_email          VARCHAR(150),
    recipient_name          VARCHAR(150) NOT NULL,
    recipient_phone         VARCHAR(20) NOT NULL,
    shipping_address        VARCHAR(600),
    shipping_lat            NUMERIC(9,6),
    shipping_lng            NUMERIC(9,6),
    subtotal                NUMERIC(12,2) NOT NULL,
    shipping_fee            NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(12,2) NOT NULL,
    receive_from            TIMESTAMPTZ,
    receive_to              TIMESTAMPTZ,
    estimated_delivery_time TIMESTAMPTZ,
    pickup_deadline_at      TIMESTAMPTZ,
    payment_expires_at      TIMESTAMPTZ,
    note                    VARCHAR(1000),
    cancelled_by            order_actor,
    cancel_reason           VARCHAR(500),
    cancelled_at            TIMESTAMPTZ,
    processing_at           TIMESTAMPTZ,
    shipped_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    paid_at                 TIMESTAMPTZ,
    paid_confirmed_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    delivery_failed_reason VARCHAR(500),
    returned_at             TIMESTAMPTZ,
    refund_required         BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_orders_customer_request UNIQUE (customer_id, client_request_id),
    CONSTRAINT chk_orders_payload_fingerprint CHECK (length(payload_fingerprint) = 64),
    CONSTRAINT chk_orders_subtotal_nonnegative CHECK (subtotal >= 0),
    CONSTRAINT chk_orders_shipping_fee_nonnegative CHECK (shipping_fee >= 0),
    CONSTRAINT chk_orders_discount_valid
        CHECK (discount_amount >= 0 AND discount_amount <= subtotal),
    CONSTRAINT chk_orders_total
        CHECK (total_amount = subtotal + shipping_fee - discount_amount),
    CONSTRAINT chk_orders_receive_window
        CHECK (receive_to IS NULL OR (receive_from IS NOT NULL AND receive_to > receive_from)),
    CONSTRAINT chk_orders_delivery_address
        CHECK (
            delivery_type <> 'DELIVERY'
            OR (
                shipping_address IS NOT NULL
                AND shipping_lat IS NOT NULL
                AND shipping_lng IS NOT NULL
            )
        ),
    CONSTRAINT chk_orders_pickup_without_address
        CHECK (
            delivery_type <> 'PICKUP'
            OR (
                address_id IS NULL
                AND shipping_address IS NULL
                AND shipping_lat IS NULL
                AND shipping_lng IS NULL
            )
        ),
    CONSTRAINT chk_orders_cancellation_fields
        CHECK (
            (status = 'CANCELLED' AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL)
            OR (status <> 'CANCELLED' AND cancelled_by IS NULL AND cancelled_at IS NULL)
        )
);

CREATE INDEX idx_orders_customer_created
    ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_branch_status_created
    ON orders (branch_id, status, created_at DESC);
CREATE INDEX idx_orders_branch_updated
    ON orders (branch_id, updated_at DESC);
CREATE INDEX idx_orders_payment_expiration
    ON orders (payment_expires_at)
    WHERE status = 'PENDING' AND payment_status = 'UNPAID';
CREATE INDEX idx_orders_pickup_deadline
    ON orders (pickup_deadline_at)
    WHERE status = 'READY_FOR_PICKUP';

CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    product_id      UUID REFERENCES products(id) ON DELETE SET NULL,
    product_name    VARCHAR(200) NOT NULL,
    sku             VARCHAR(50) NOT NULL,
    image_url       VARCHAR(500),
    unit            VARCHAR(20) NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    total_price     NUMERIC(14,2) NOT NULL,
    CONSTRAINT uk_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_total_price
        CHECK (total_price = quantity * unit_price)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE payments (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                 UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    provider                 VARCHAR(30) NOT NULL,
    amount                   NUMERIC(12,2) NOT NULL,
    currency                 VARCHAR(3) NOT NULL DEFAULT 'VND',
    provider_reference       VARCHAR(100) NOT NULL,
    transaction_id           VARCHAR(150) UNIQUE,
    status                   payment_attempt_status NOT NULL DEFAULT 'INITIATED',
    refund_required          BOOLEAN NOT NULL DEFAULT false,
    raw_response             JSONB,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
);
CREATE INDEX idx_payments_order_created ON payments (order_id, created_at DESC);
CREATE INDEX idx_payments_provider_reference ON payments (provider, provider_reference);

CREATE TABLE promotions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50) NOT NULL,
    type                promotion_type NOT NULL,
    discount_value      NUMERIC(12,2) NOT NULL,
    max_discount_amount NUMERIC(12,2),
    min_order_value     NUMERIC(12,2) NOT NULL DEFAULT 0,
    starts_at           TIMESTAMPTZ NOT NULL,
    ends_at             TIMESTAMPTZ NOT NULL,
    branch_id           UUID REFERENCES branches(id) ON DELETE CASCADE,
    usage_limit         INTEGER,
    used_count          INTEGER NOT NULL DEFAULT 0,
    per_user_limit      INTEGER NOT NULL DEFAULT 1,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_promotions_value CHECK (
        (type = 'PERCENT' AND discount_value > 0 AND discount_value <= 100)
        OR (type = 'FIXED' AND discount_value > 0)
    ),
    CONSTRAINT chk_promotions_max_discount
        CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT chk_promotions_min_order CHECK (min_order_value >= 0),
    CONSTRAINT chk_promotions_period CHECK (starts_at < ends_at),
    CONSTRAINT chk_promotions_usage CHECK (
        used_count >= 0
        AND (usage_limit IS NULL OR (usage_limit >= 0 AND used_count <= usage_limit))
    ),
    CONSTRAINT chk_promotions_per_user CHECK (per_user_limit > 0)
);
CREATE UNIQUE INDEX uk_promotions_code_ci ON promotions (lower(code));
CREATE INDEX idx_promotions_active_period ON promotions (active, starts_at, ends_at);
CREATE INDEX idx_promotions_branch ON promotions (branch_id);

CREATE TABLE order_promotions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    promotion_id        UUID NOT NULL REFERENCES promotions(id) ON DELETE RESTRICT,
    customer_id         UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    discount_amount     NUMERIC(12,2) NOT NULL,
    status              order_promotion_status NOT NULL DEFAULT 'APPLIED',
    released_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_order_promotions_order_promotion UNIQUE (order_id, promotion_id),
    CONSTRAINT chk_order_promotions_discount CHECK (discount_amount >= 0),
    CONSTRAINT chk_order_promotions_release CHECK (
        (status = 'APPLIED' AND released_at IS NULL)
        OR (status = 'RELEASED' AND released_at IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uk_order_promotions_one_applied_per_order
    ON order_promotions (order_id)
    WHERE status = 'APPLIED';
CREATE INDEX idx_order_promotions_customer_promotion
    ON order_promotions (customer_id, promotion_id, status);

CREATE TABLE promotion_user_usage (
    promotion_id    UUID NOT NULL REFERENCES promotions(id) ON DELETE RESTRICT,
    customer_id     UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    used_count      INTEGER NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (promotion_id, customer_id),
    CONSTRAINT chk_promotion_user_usage_count CHECK (used_count >= 0)
);
