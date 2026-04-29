-- =====================================================================
-- Discount: 20% off all Cakes and Cookies
--
-- Run this in pgAdmin against the ecommercedb database. It is wrapped
-- in a transaction so you can review the preview output and ROLLBACK
-- if anything looks wrong.
-- =====================================================================

BEGIN;

-- 1) Before: list current prices for products in the Cakes / Cookies categories
SELECT p.id,
       c.name  AS category,
       p.name  AS product,
       p.price AS old_price,
       ROUND(p.price * 0.80) AS new_price
FROM   public."Products"   p
JOIN   public."Categories" c ON c.id = p.category_id
WHERE  c.name IN ('Cakes', 'Cookies')
ORDER  BY c.name, p.name;

-- 2) Apply 20% off, rounded to whole rupees
UPDATE public."Products" AS p
SET    price = ROUND(p.price * 0.80)
FROM   public."Categories" c
WHERE  p.category_id = c.id
  AND  c.name IN ('Cakes', 'Cookies');

-- 3) After: confirm the new prices
SELECT p.id,
       c.name  AS category,
       p.name  AS product,
       p.price AS new_price
FROM   public."Products"   p
JOIN   public."Categories" c ON c.id = p.category_id
WHERE  c.name IN ('Cakes', 'Cookies')
ORDER  BY c.name, p.name;

-- If the preview (step 3) looks right, run:   COMMIT;
-- If not, run:                                ROLLBACK;
COMMIT;
