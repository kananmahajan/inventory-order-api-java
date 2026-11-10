CREATE TABLE IF NOT EXISTS products (
 id BIGSERIAL PRIMARY KEY,
 sku VARCHAR(64) NOT NULL,
 name VARCHAR(160) NOT NULL,
 category VARCHAR(100) NOT NULL,
 price NUMERIC(12,2) NOT NULL CHECK(price>0),
 stock_quantity INTEGER NOT NULL CHECK(stock_quantity>=0),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_products_sku_lower ON products(LOWER(sku));
CREATE INDEX IF NOT EXISTS idx_products_name_lower ON products(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE TABLE IF NOT EXISTS purchase_orders (
 id BIGSERIAL PRIMARY KEY,
 order_number UUID NOT NULL UNIQUE,
 customer_username VARCHAR(100) NOT NULL,
 status VARCHAR(24) NOT NULL CHECK(status IN ('PLACED','CANCELLED')),
 total_amount NUMERIC(12,2) NOT NULL CHECK(total_amount>=0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 cancelled_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON purchase_orders(customer_username,created_at DESC);
CREATE TABLE IF NOT EXISTS order_items (
 id BIGSERIAL PRIMARY KEY,
 order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
 product_id BIGINT NOT NULL REFERENCES products(id),
 sku_at_purchase VARCHAR(64) NOT NULL,
 product_name_at_purchase VARCHAR(160) NOT NULL,
 unit_price NUMERIC(12,2) NOT NULL CHECK(unit_price>0),
 quantity INTEGER NOT NULL CHECK(quantity>0),
 line_total NUMERIC(12,2) NOT NULL CHECK(line_total>0)
);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
