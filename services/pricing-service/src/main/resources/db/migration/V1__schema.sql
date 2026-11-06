CREATE TABLE pricing_rule (
    code        VARCHAR(40)  PRIMARY KEY,
    rule_value  BIGINT       NOT NULL,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE quote (
    id                VARCHAR(40) PRIMARY KEY,
    currency          VARCHAR(3)  NOT NULL,
    subtotal_minor    BIGINT      NOT NULL,
    discount_bp       INT         NOT NULL,
    discount_minor    BIGINT      NOT NULL,
    service_fee_minor BIGINT      NOT NULL,
    total_minor       BIGINT      NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE quote_line (
    quote_id         VARCHAR(40) NOT NULL REFERENCES quote (id),
    line_index       INT         NOT NULL,
    offer_id         VARCHAR(80) NOT NULL,
    mode             VARCHAR(20) NOT NULL,
    quantity         INT         NOT NULL,
    unit_price_minor BIGINT      NOT NULL,
    line_total_minor BIGINT      NOT NULL,
    PRIMARY KEY (quote_id, line_index)
);
