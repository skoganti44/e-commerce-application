-- =====================================================================
-- Kitchen dashboard seed script (robust / self-healing version)
--
-- Run this in pgAdmin against the ecommercedb database AFTER Spring Boot
-- has started with the updated code (so the channel + kitchen_status
-- columns and the daily_stock table exist).
--
-- Unlike the earlier version, this one:
--   * creates a test customer if none exists
--   * creates the 4 test products if they don't exist
--   * uses variables so inserts never silently fail on no match
--   * prints RAISE NOTICE messages so you can see what happened
-- =====================================================================

-- Belt-and-suspenders: ensure columns and table exist even if Hibernate
-- didn't run (idempotent).
ALTER TABLE "Orders"
    ADD COLUMN IF NOT EXISTS channel        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS kitchen_status VARCHAR(20);

CREATE TABLE IF NOT EXISTS daily_stock (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT,
    target_count   INTEGER NOT NULL DEFAULT 0,
    prepared_count INTEGER NOT NULL DEFAULT 0,
    stock_date     DATE NOT NULL
);

-- =====================================================================
-- Main seed block
-- =====================================================================
DO $$
DECLARE
    v_role_id      INT;
    v_user_id      INT;
    v_prod_cookie  BIGINT;
    v_prod_sponge  BIGINT;
    v_prod_millet  BIGINT;
    v_prod_velvet  BIGINT;
    v_order_id     BIGINT;
BEGIN
    -----------------------------------------------------------------
    -- 1) Ensure a 'customer' role exists (no department)
    -----------------------------------------------------------------
    SELECT id INTO v_role_id
      FROM "Role"
     WHERE LOWER(role) = 'customer'
       AND (department IS NULL OR department = '')
     LIMIT 1;

    IF v_role_id IS NULL THEN
        INSERT INTO "Role" (role, "fullName", department)
        VALUES ('customer', 'Customer', NULL)
        RETURNING id INTO v_role_id;
        RAISE NOTICE 'Created customer role id=%', v_role_id;
    ELSE
        RAISE NOTICE 'Found customer role id=%', v_role_id;
    END IF;

    -----------------------------------------------------------------
    -- 2) Find any customer user, or create a test one
    -----------------------------------------------------------------
    SELECT u.userid INTO v_user_id
      FROM "User" u
      JOIN "userRole" ur ON ur.userid = u.userid
     WHERE ur.roleid = v_role_id
     ORDER BY u.userid ASC
     LIMIT 1;

    IF v_user_id IS NULL THEN
        INSERT INTO "User" (name, email, password, createdat)
        VALUES ('Kitchen Test Customer',
                'kitchen.test@example.com',
                'not-for-login',
                NOW())
        RETURNING userid INTO v_user_id;

        INSERT INTO "userRole" (userid, roleid)
        VALUES (v_user_id, v_role_id);

        RAISE NOTICE 'Created test customer userid=%', v_user_id;
    ELSE
        RAISE NOTICE 'Using existing customer userid=%', v_user_id;
    END IF;

    -----------------------------------------------------------------
    -- 3) Ensure 4 test products exist; capture their ids
    -----------------------------------------------------------------
    SELECT id INTO v_prod_cookie FROM "Products"
     WHERE name = 'Chocolate Chip Cookie' LIMIT 1;
    IF v_prod_cookie IS NULL THEN
        INSERT INTO "Products" (name, description, price, stock)
        VALUES ('Chocolate Chip Cookie',
                'Classic chewy chocolate chip cookie', 3.50, 200)
        RETURNING id INTO v_prod_cookie;
    END IF;

    SELECT id INTO v_prod_sponge FROM "Products"
     WHERE name = 'Vanilla Sponge Cake' LIMIT 1;
    IF v_prod_sponge IS NULL THEN
        INSERT INTO "Products" (name, description, price, stock)
        VALUES ('Vanilla Sponge Cake',
                'Light vanilla sponge, 500g', 22.00, 50)
        RETURNING id INTO v_prod_sponge;
    END IF;

    SELECT id INTO v_prod_millet FROM "Products"
     WHERE name = 'Millet Cookie' LIMIT 1;
    IF v_prod_millet IS NULL THEN
        INSERT INTO "Products" (name, description, price, stock)
        VALUES ('Millet Cookie',
                'Finger-millet cookie with jaggery', 4.00, 150)
        RETURNING id INTO v_prod_millet;
    END IF;

    SELECT id INTO v_prod_velvet FROM "Products"
     WHERE name = 'Red Velvet Cake' LIMIT 1;
    IF v_prod_velvet IS NULL THEN
        INSERT INTO "Products" (name, description, price, stock)
        VALUES ('Red Velvet Cake',
                'Small 1 lb red velvet cake', 28.00, 30)
        RETURNING id INTO v_prod_velvet;
    END IF;

    RAISE NOTICE 'Products: cookie=% sponge=% millet=% velvet=%',
        v_prod_cookie, v_prod_sponge, v_prod_millet, v_prod_velvet;

    -----------------------------------------------------------------
    -- 4) Clear previous kitchen test data (rows tagged with channel)
    -----------------------------------------------------------------
    DELETE FROM order_items
     WHERE order_id IN (SELECT id FROM "Orders" WHERE channel IS NOT NULL);
    DELETE FROM payments
     WHERE order_id IN (SELECT id FROM "Orders" WHERE channel IS NOT NULL);
    DELETE FROM "Orders" WHERE channel IS NOT NULL;
    DELETE FROM daily_stock WHERE stock_date = CURRENT_DATE;

    -----------------------------------------------------------------
    -- 5) Three ONLINE orders
    -----------------------------------------------------------------
    INSERT INTO "Orders" (user_id, total_amount, status, channel, kitchen_status)
    VALUES (v_user_id, 33.00, 'PAID', 'online', 'pending')
    RETURNING id INTO v_order_id;
    INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
        (v_order_id, v_prod_cookie, 6, 3.50),
        (v_order_id, v_prod_millet, 3, 4.00);

    INSERT INTO "Orders" (user_id, total_amount, status, channel, kitchen_status)
    VALUES (v_user_id, 22.00, 'PAID', 'online', 'baking')
    RETURNING id INTO v_order_id;
    INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
        (v_order_id, v_prod_sponge, 1, 22.00);

    INSERT INTO "Orders" (user_id, total_amount, status, channel, kitchen_status)
    VALUES (v_user_id, 14.00, 'PAID', 'online', 'pending')
    RETURNING id INTO v_order_id;
    INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
        (v_order_id, v_prod_cookie, 4, 3.50);

    -----------------------------------------------------------------
    -- 6) Two IN-STORE (walk-in) orders
    -----------------------------------------------------------------
    INSERT INTO "Orders" (user_id, total_amount, status, channel, kitchen_status)
    VALUES (v_user_id, 28.00, 'PAID', 'instore', 'pending')
    RETURNING id INTO v_order_id;
    INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
        (v_order_id, v_prod_velvet, 1, 28.00);

    INSERT INTO "Orders" (user_id, total_amount, status, channel, kitchen_status)
    VALUES (v_user_id, 15.00, 'PAID', 'instore', 'baking')
    RETURNING id INTO v_order_id;
    INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
        (v_order_id, v_prod_millet, 2, 4.00),
        (v_order_id, v_prod_cookie, 2, 3.50);

    -----------------------------------------------------------------
    -- 7) Today's daily baking targets
    -----------------------------------------------------------------
    INSERT INTO daily_stock (product_id, target_count, prepared_count, stock_date) VALUES
        (v_prod_cookie, 60, 18, CURRENT_DATE),
        (v_prod_millet, 40, 12, CURRENT_DATE),
        (v_prod_sponge,  8,  3, CURRENT_DATE),
        (v_prod_velvet,  6,  2, CURRENT_DATE);

    RAISE NOTICE 'Seed complete: 3 online orders, 2 in-store orders, 4 daily-stock rows.';
END $$;

-- =====================================================================
-- Verification queries — run these and you should see rows
-- =====================================================================
SELECT id, channel, kitchen_status, total_amount
  FROM "Orders"
 WHERE channel IS NOT NULL
 ORDER BY id DESC;

SELECT oi.order_id, p.name AS product, oi.quantity, oi.price
  FROM order_items oi
  JOIN "Products" p ON p.id = oi.product_id
  JOIN "Orders"   o ON o.id = oi.order_id
 WHERE o.channel IS NOT NULL
 ORDER BY oi.order_id DESC, oi.id ASC;

SELECT ds.id, p.name, ds.target_count, ds.prepared_count, ds.stock_date
  FROM daily_stock ds
  JOIN "Products" p ON p.id = ds.product_id
 WHERE ds.stock_date = CURRENT_DATE
 ORDER BY ds.id;
