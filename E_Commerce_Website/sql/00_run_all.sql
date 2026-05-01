-- =============================================================================
--  E-Commerce Bakery — Full Database Bootstrap
-- =============================================================================
--  Recreates the ecommercedb schema and seeds it with the current dataset.
--  Run order:
--      1. 01_schema.sql  — CREATE TABLE for all 20 tables + sequences + FKs
--      2. 02_data.sql    — 159 INSERT statements (column-named)
--
--  How to run on a clean database:
--      createdb -U postgres ecommercedb
--      psql -U postgres -d ecommercedb -f 00_run_all.sql
--
--  How to run on an existing empty database:
--      psql -U postgres -d ecommercedb -f 00_run_all.sql
--
--  Optional add-ons (run AFTER 02_data.sql if desired):
--      psql -U postgres -d ecommercedb -f kitchen_seed.sql
--      psql -U postgres -d ecommercedb -f discount_cakes_cookies.sql
-- =============================================================================

\echo '>> 1/2  creating schema (20 tables, sequences, foreign keys) ...'
\i 01_schema.sql

\echo '>> 2/2  inserting seed data (159 rows across 16 tables) ...'
\i 02_data.sql

\echo ''
\echo '=== bootstrap complete ==='
SET search_path TO public;
SELECT
    (SELECT COUNT(*) FROM public."User")          AS users,
    (SELECT COUNT(*) FROM public."Role")          AS roles,
    (SELECT COUNT(*) FROM public."Products")      AS products,
    (SELECT COUNT(*) FROM public."Categories")    AS categories,
    (SELECT COUNT(*) FROM public."Orders")        AS orders,
    (SELECT COUNT(*) FROM public.order_items)     AS order_items,
    (SELECT COUNT(*) FROM public.tasks)           AS tasks,
    (SELECT COUNT(*) FROM public.delivery_trips)  AS trips,
    (SELECT COUNT(*) FROM public.supplies)        AS supplies;
