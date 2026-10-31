CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    type            VARCHAR(20) NOT NULL CHECK (
                        type IN ('ORDER', 'PROMOTION', 'SYSTEM', 'ENGAGEMENT')
                    ),
    is_read         BOOLEAN NOT NULL DEFAULT false,
    image_url       VARCHAR(500),
    target_url      VARCHAR(500) NOT NULL,
    reference_id    VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_notifications_unread
    ON notifications (user_id)
    WHERE is_read = false;
