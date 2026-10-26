CREATE TABLE customer_inquiries (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX customer_inquiries_created_at_id_idx
    ON customer_inquiries (created_at DESC, id DESC);
