--
-- PostgreSQL database dump
--



-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: User; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (1, 'Alice Johnson', 'alice@example.com', 'Abc@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (2, 'Bob Smith', 'bob@example.com', 'Bob@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (3, 'Carol Davis', 'carol@example.com', 'Car@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (4, 'Alex Pin', 'alexpin12@gmail.com', 'Abc@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (5, 'Dan Baker', 'dan.baker@example.com', 'Dan@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (6, 'Eva Sales', 'eva.sales@example.com', 'Eva@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (7, 'Frank Cook', 'frank.cook@example.com', 'Fra@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (8, 'Grace Driver', 'grace.driver@example.com', 'Gra@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (9, 'Henry Boss', 'henry.boss@example.com', 'Hen@1234', '2026-04-23 13:19:04.850578');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (10, 'Dhati Admin', 'admin@dhati.local', 'Admin@123', '2026-04-27 13:24:01.515426');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (11, 'Bella Baker', 'bakery@dhati.local', 'Bakery@123', '2026-04-27 13:24:01.55272');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (12, 'Karthik Kumar', 'kitchen@dhati.local', 'Kitchen@123', '2026-04-27 13:24:01.557715');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (13, 'Sara Sales', 'sales@dhati.local', 'Sales@123', '2026-04-27 13:24:01.561804');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (14, 'Dinesh Driver', 'delivery@dhati.local', 'Delivery@123', '2026-04-27 13:24:01.565805');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (15, 'Maya Manager', 'management@dhati.local', 'Manage@123', '2026-04-27 13:24:01.570599');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (16, 'Anita Customer', 'customer@dhati.local', 'Customer@123', '2026-04-27 13:24:01.575601');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (17, 'Aanya Kapoor', 'delivery1@dhati.local', 'Customer@123', '2026-04-28 14:11:03.193237');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (18, 'Bhavesh Gupta', 'delivery2@dhati.local', 'Customer@123', '2026-04-28 14:11:03.200879');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (19, 'Chitra Iyer', 'delivery3@dhati.local', 'Customer@123', '2026-04-28 14:11:03.205195');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (20, 'Deepak Reddy', 'delivery4@dhati.local', 'Customer@123', '2026-04-28 14:11:03.209195');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (21, 'Esha Sharma', 'delivery5@dhati.local', 'Customer@123', '2026-04-28 14:11:03.214281');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (22, 'Farhan Khan', 'delivery6@dhati.local', 'Customer@123', '2026-04-28 14:11:03.219281');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (23, 'Geeta Menon', 'delivery7@dhati.local', 'Customer@123', '2026-04-28 14:11:03.223281');
INSERT INTO public."User" (userid, name, email, password, createdat) VALUES (24, 'Harish Rao', 'delivery8@dhati.local', 'Customer@123', '2026-04-28 14:11:03.228281');


--
-- Data for Name: Cart; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."Cart" (id, userid, createdat, created_at, user_id) VALUES (1, NULL, '2026-04-29 14:53:42.186676', '2026-04-29 14:53:42.175512', 16);


--
-- Data for Name: Categories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."Categories" (id, name, description) VALUES (1, 'Cookies', 'Freshly baked cookies');
INSERT INTO public."Categories" (id, name, description) VALUES (2, 'Cakes', 'Celebration cakes');
INSERT INTO public."Categories" (id, name, description) VALUES (3, 'Millet Specials', 'Healthy millet-based bakes');


--
-- Data for Name: Orders; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (3, 1, 14.00, 'PAID', '2026-04-23 14:40:33.481524', 'online', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (5, 1, 15.00, 'PAID', '2026-04-23 14:40:33.481524', 'instore', 'baking', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (2, 1, 22.00, 'PAID', '2026-04-23 14:40:33.481524', 'online', 'ready', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (4, 1, 28.00, 'PAID', '2026-04-23 14:40:33.481524', 'instore', 'ready', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (6, 17, 499.00, 'confirmed', '2026-04-28 14:11:03.23354', 'online', 'done', 'Ring twice', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (8, 19, 899.00, 'confirmed', '2026-04-28 14:11:03.244205', 'online', 'out_for_delivery', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (9, 20, 549.00, 'delivered', '2026-04-28 14:11:03.247322', 'online', 'delivered', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (10, 21, 1199.00, 'delivered', '2026-04-28 14:11:03.250477', 'online', 'delivered', 'Leave at door', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (11, 22, 299.00, 'confirmed', '2026-04-28 14:11:03.254689', 'online', 'delivery_failed', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (12, 23, 799.00, 'confirmed', '2026-04-28 14:11:03.257546', 'online', 'delivery_failed', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (13, 24, 649.00, 'confirmed', '2026-04-28 14:11:03.260117', 'online', 'delivery_failed', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (1, 1, 33.00, 'PAID', '2026-04-23 14:40:33.481524', 'online', 'picked_up', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public."Orders" (id, user_id, total_amount, status, created_at, channel, kitchen_status, customer_notes, kitchen_notes, approval_notes, approval_status, approved_at, requires_approval, approved_by_user_id) VALUES (7, 18, 699.00, 'confirmed', '2026-04-28 14:11:03.239471', 'online', 'out_for_delivery', NULL, NULL, NULL, NULL, NULL, NULL, NULL);


--
-- Data for Name: Product_Available; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: Products; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (1, 'Classic Chocolate Chip Cookie', 'Crunchy edges, gooey center â€” a timeless favourite.', 49.00, 50, 1, '2026-04-23 13:36:30.058202', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (2, 'Oatmeal Raisin Cookie', 'Hearty rolled oats and plump raisins in every bite.', 39.00, 40, 1, '2026-04-23 13:36:30.07', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (3, 'Double Chocolate Cookie', 'Rich cocoa dough loaded with dark chocolate chunks.', 59.00, 30, 1, '2026-04-23 13:36:30.074568', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (4, 'Peanut Butter Cookie', 'Soft, chewy, and seriously peanut-buttery.', 45.00, 25, 1, '2026-04-23 13:36:30.077803', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (5, 'Red Velvet Cake', 'Classic red velvet with silky cream-cheese frosting.', 699.00, 10, 2, '2026-04-23 13:36:30.080791', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (6, 'Black Forest Cake', 'Dark cocoa sponge, whipped cream, and black cherries.', 749.00, 8, 2, '2026-04-23 13:36:30.084271', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (7, 'Vanilla Sponge Cake', 'Light-as-air vanilla sponge layered with buttercream.', 549.00, 12, 2, '2026-04-23 13:36:30.087501', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (8, 'Chocolate Truffle Cake', 'Decadent chocolate ganache over moist chocolate sponge.', 799.00, 0, 2, '2026-04-23 13:36:30.090425', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (9, 'Ragi Millet Cookies', 'Nutty, wholesome finger-millet cookies, lightly sweetened.', 69.00, 35, 3, '2026-04-23 13:36:30.092776', 1, NULL, NULL);
INSERT INTO public."Products" (id, name, description, price, stock, category_id, created_at, created_by_userid, supported_flours, supported_sweeteners) VALUES (10, 'Jowar Millet Cake', 'Gluten-free jowar-millet cake with jaggery sweetness.', 599.00, 6, 3, '2026-04-23 13:36:30.095308', 1, NULL, NULL);


--
-- Data for Name: Product_Images; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (1, 1, 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (2, 2, 'https://images.unsplash.com/photo-1568051243851-f9b136146e97?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (3, 3, 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (4, 4, 'https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (5, 5, 'https://images.unsplash.com/photo-1586040140378-b5634cb4c8fc?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (6, 6, 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (7, 7, 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (8, 8, 'https://images.unsplash.com/photo-1578775887804-699de7086ff9?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (9, 9, 'https://images.unsplash.com/photo-1590080876100-0d2c6f8a8f85?auto=format&fit=crop&w=800&q=80');
INSERT INTO public."Product_Images" (id, product_id, image_url) VALUES (10, 10, 'https://images.unsplash.com/photo-1621303837174-89787a7d4729?auto=format&fit=crop&w=800&q=80');


--
-- Data for Name: Role; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."Role" (id, "fullName", role, department) VALUES (1, 'Alice Johnson', 'customer', NULL);
INSERT INTO public."Role" (id, "fullName", role, department) VALUES (2, 'Bob Smith', 'employee', 'bakery');
INSERT INTO public."Role" (id, "fullName", role, department) VALUES (3, 'Carol Davis', 'employee', 'sales');
INSERT INTO public."Role" (id, "fullName", role, department) VALUES (4, 'Alex Pin', 'employee', 'kitchen');
INSERT INTO public."Role" (id, "fullName", role, department) VALUES (5, 'Dan Baker', 'employee', 'delivery');
INSERT INTO public."Role" (id, "fullName", role, department) VALUES (6, 'Eva Sales', 'employee', 'management');


--
-- Data for Name: cart_items; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.cart_items (id, cart_id, product_id, quantity, customization, flour_type, sweetener_percent, sweetener_type) VALUES (1, 1, 1, 1, NULL, NULL, NULL, NULL);


--
-- Data for Name: daily_stock; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.daily_stock (id, prepared_count, stock_date, target_count, product_id) VALUES (1, 18, '2026-04-23', 60, 1);
INSERT INTO public.daily_stock (id, prepared_count, stock_date, target_count, product_id) VALUES (2, 12, '2026-04-23', 40, 2);
INSERT INTO public.daily_stock (id, prepared_count, stock_date, target_count, product_id) VALUES (3, 3, '2026-04-23', 8, 3);
INSERT INTO public.daily_stock (id, prepared_count, stock_date, target_count, product_id) VALUES (4, 2, '2026-04-23', 6, 4);


--
-- Data for Name: delivery_trips; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (2, NULL, NULL, '2026-04-28 13:26:03.2316', NULL, NULL, NULL, NULL, NULL, '4729', '2026-04-28 14:01:03.2316', NULL, '2026-04-28 13:26:03.2316', 'out_for_delivery', NULL, '2026-04-28 14:01:03.2316', 14, 8);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (3, 549.00, '2026-04-28 11:56:03.2316', '2026-04-28 11:11:03.2316', '2026-04-28 11:56:03.2316', 4.20, NULL, NULL, 'Delivered to customer in person.', '1234', '2026-04-28 11:26:03.2316', NULL, '2026-04-28 11:11:03.2316', 'delivered', 25.00, '2026-04-28 11:56:03.2316', 14, 9);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (4, NULL, NULL, '2026-04-28 09:11:03.2316', '2026-04-28 09:56:03.2316', 6.80, NULL, NULL, 'Customer not at door â€” handed to building security as per instructions.', NULL, '2026-04-28 09:31:03.2316', 'https://photos.dhati.local/proof/order-105.jpg', '2026-04-28 09:11:03.2316', 'delivered', NULL, '2026-04-28 09:56:03.2316', 14, 10);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (5, NULL, NULL, '2026-04-28 07:11:03.2316', NULL, 3.50, '2026-04-28 07:56:03.2316', 'customer_not_home', 'Knocked, called twice â€” no answer for 10 mins.', '9999', '2026-04-28 07:41:03.2316', NULL, '2026-04-28 07:11:03.2316', 'failed', NULL, '2026-04-28 07:56:03.2316', 14, 11);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (6, NULL, NULL, '2026-04-27 16:11:03.2316', NULL, 9.10, '2026-04-27 17:11:03.2316', 'refused', 'Customer says she ordered by mistake, refused delivery.', NULL, '2026-04-27 16:41:03.2316', NULL, '2026-04-27 16:11:03.2316', 'failed', NULL, '2026-04-27 17:11:03.2316', 14, 12);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (7, NULL, NULL, '2026-04-26 15:11:03.2316', NULL, 5.50, '2026-04-26 15:46:03.2316', 'damaged', 'Cake box dropped getting off the bike. Returned to bakery for refund.', NULL, '2026-04-26 15:31:03.2316', NULL, '2026-04-26 15:11:03.2316', 'failed', NULL, '2026-04-26 15:46:03.2316', 14, 13);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (8, NULL, NULL, '2026-04-28 14:18:51.361478', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-28 14:18:51.361478', 'picked_up', NULL, '2026-04-28 14:18:51.361478', 14, 1);
INSERT INTO public.delivery_trips (id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km, failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at, status, tip_amount, updated_at, driver_user_id, order_id) VALUES (1, NULL, NULL, '2026-04-28 13:56:03.2316', NULL, NULL, NULL, NULL, NULL, '9136', '2026-04-28 14:18:59.234312', NULL, '2026-04-28 13:56:03.2316', 'out_for_delivery', NULL, '2026-04-28 14:18:59.234312', 14, 7);


--
-- Data for Name: delivery_issues; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.delivery_issues (id, description, issue_type, reported_at, resolved_at, driver_user_id, trip_id) VALUES (1, 'Tire puncture near Silk Board flyover, used spare. Need to get patched soon.', 'vehicle_breakdown', '2026-04-25 14:11:03.2316', NULL, 14, NULL);
INSERT INTO public.delivery_issues (id, description, issue_type, reported_at, resolved_at, driver_user_id, trip_id) VALUES (2, 'ORR closure caused 40-min delay between Marathahalli and Sarjapur.', 'traffic_delay', '2026-04-27 18:11:03.2316', NULL, 14, NULL);
INSERT INTO public.delivery_issues (id, description, issue_type, reported_at, resolved_at, driver_user_id, trip_id) VALUES (3, 'Bike skid on wet road, dropped delivery box (see trip notes). Self ok.', 'accident', '2026-04-26 15:51:03.2316', NULL, 14, 7);


--
-- Data for Name: discount_campaigns; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (1, 1, 1, 6, 3.50, NULL, NULL, NULL, NULL);
INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (2, 1, 2, 3, 4.00, NULL, NULL, NULL, NULL);
INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (3, 2, 3, 1, 22.00, NULL, NULL, NULL, NULL);
INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (4, 3, 1, 4, 3.50, NULL, NULL, NULL, NULL);
INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (5, 4, 4, 1, 28.00, NULL, NULL, NULL, NULL);
INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (6, 5, 2, 2, 4.00, NULL, NULL, NULL, NULL);
INSERT INTO public.order_items (id, order_id, product_id, quantity, price, customization, flour_type, sweetener_percent, sweetener_type) VALUES (7, 5, 1, 2, 3.50, NULL, NULL, NULL, NULL);


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: refund_requests; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: shipping_addresses; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (1, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.23563', 'Aanya Kapoor', 'Leave at the door if no answer', 'Indiranagar', '12 MG Road', 'Apt 4B', '9876500001', '560038', 'Karnataka', 6, 17);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (2, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.23963', 'Bhavesh Gupta', NULL, 'MG Road', '47 Brigade Road', NULL, '9876500002', '560001', 'Karnataka', 7, 18);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (3, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.24463', 'Chitra Iyer', 'Call when you arrive', 'Ashok Nagar', '8 Church Street', 'Floor 3', '9876500003', '560001', 'Karnataka', 8, 19);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (4, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.247893', 'Deepak Reddy', NULL, 'Shanti Nagar', '55 Residency Road', NULL, '9876500004', '560025', 'Karnataka', 9, 20);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (5, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.250892', 'Esha Sharma', 'Leave at security desk', 'Koramangala', '21 Koramangala 4th Block', NULL, '9876500005', '560034', 'Karnataka', 10, 21);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (6, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.255088', 'Farhan Khan', NULL, 'HSR Layout', '9 HSR Sector 6', NULL, '9876500006', '560102', 'Karnataka', 11, 22);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (7, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.257087', 'Geeta Menon', NULL, 'Whitefield', '33 Whitefield Main', NULL, '9876500007', '560066', 'Karnataka', 12, 23);
INSERT INTO public.shipping_addresses (id, address_type, city, country, created_at, full_name, instructions, landmark, line1, line2, phone, pincode, state, order_id, user_id) VALUES (8, 'home', 'Bengaluru', 'India', '2026-04-28 14:11:03.260088', 'Harish Rao', NULL, 'JP Nagar', '77 JP Nagar Phase 7', NULL, '9876500008', '560078', 'Karnataka', 13, 24);


--
-- Data for Name: supplies; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (6, 'flour', 0.000, 'All-Purpose Flour', NULL, 3.000, 'kg', '2026-04-23 16:03:43.15667', 'received', NULL, 10.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (22, 'nut_seed', 0.000, 'Almonds', NULL, 0.500, 'kg', '2026-04-23 16:03:43.159673', 'received', NULL, 1.500, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (2, 'flour', 0.000, 'Bajra Millet Flour', NULL, 2.000, 'kg', '2026-04-23 16:03:43.160672', 'received', NULL, 6.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (16, 'leavening', 0.000, 'Baking Powder', NULL, 150.000, 'g', '2026-04-23 16:03:43.161668', 'received', NULL, 500.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (17, 'leavening', 0.000, 'Baking Soda', NULL, 150.000, 'g', '2026-04-23 16:03:43.163669', 'received', NULL, 500.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (8, 'sweetener', 0.000, 'Brown Sugar', NULL, 1.000, 'kg', '2026-04-23 16:03:43.164673', 'received', NULL, 5.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (12, 'dairy', 0.000, 'Butter (unsalted)', NULL, 1.000, 'kg', '2026-04-23 16:03:43.166669', 'received', NULL, 6.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (26, 'packaging', 0.000, 'Cake Boxes', NULL, 20.000, 'pcs', '2026-04-23 16:03:43.167673', 'received', NULL, 60.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (7, 'sweetener', 0.000, 'Cane Sugar', NULL, 2.000, 'kg', '2026-04-23 16:03:43.168669', 'received', NULL, 8.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (23, 'nut_seed', 0.000, 'Cashews', NULL, 0.500, 'kg', '2026-04-23 16:03:43.169669', 'received', NULL, 1.500, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (20, 'flavour', 0.000, 'Cocoa Powder', NULL, 0.500, 'kg', '2026-04-23 16:03:43.170672', 'received', NULL, 1.500, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (25, 'packaging', 0.000, 'Cookie Boxes (small)', NULL, 40.000, 'pcs', '2026-04-23 16:03:43.171669', 'received', NULL, 120.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (14, 'dairy', 0.000, 'Curd / Yogurt', NULL, 0.500, 'kg', '2026-04-23 16:03:43.172669', 'received', NULL, 2.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (15, 'egg', 0.000, 'Eggs', NULL, 24.000, 'pcs', '2026-04-23 16:03:43.173669', 'received', NULL, 60.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (1, 'flour', 0.000, 'Finger Millet (Ragi) Flour', NULL, 3.000, 'kg', '2026-04-23 16:03:43.173669', 'received', NULL, 10.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (10, 'sweetener', 0.000, 'Honey', NULL, 1.000, 'l', '2026-04-23 16:03:43.174672', 'received', NULL, 3.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (9, 'sweetener', 0.000, 'Jaggery', NULL, 1.500, 'kg', '2026-04-23 16:03:43.175669', 'received', NULL, 5.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (3, 'flour', 0.000, 'Little Millet Flour', NULL, 2.000, 'kg', '2026-04-23 16:03:43.176671', 'received', NULL, 5.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (11, 'sweetener', 0.000, 'Maple Syrup', NULL, 0.500, 'l', '2026-04-23 16:03:43.177671', 'received', NULL, 2.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (13, 'dairy', 0.000, 'Milk', NULL, 2.000, 'l', '2026-04-23 16:03:43.178671', 'waiting', '2026-04-23 15:52:07.083871', 6.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (27, 'packaging', 0.000, 'Paper Liners', NULL, 3.000, 'pack', '2026-04-23 16:03:43.179669', 'received', NULL, 10.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (28, 'packaging', 0.000, 'Parchment Paper', NULL, 2.000, 'pack', '2026-04-23 16:03:43.180669', 'received', NULL, 6.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (21, 'flavour', 0.000, 'Salt', NULL, 0.500, 'kg', '2026-04-23 16:03:43.181673', 'waiting', '2026-04-23 15:52:07.085877', 2.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (24, 'nut_seed', 0.000, 'Sesame Seeds', NULL, 0.300, 'kg', '2026-04-23 16:03:43.18267', 'received', NULL, 1.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (4, 'flour', 0.000, 'Sorghum (Jowar) Flour', NULL, 2.000, 'kg', '2026-04-23 16:03:43.18367', 'received', NULL, 5.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (19, 'flavour', 0.000, 'Vanilla Essence', NULL, 100.000, 'ml', '2026-04-23 16:03:43.18367', 'received', NULL, 500.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (5, 'flour', 0.000, 'Whole Wheat Flour', NULL, 4.000, 'kg', '2026-04-23 16:03:43.184669', 'received', NULL, 8.000, 0.000, NULL);
INSERT INTO public.supplies (id, category, current_stock, name, notes, threshold, unit, updated_at, order_status, requested_at, in_stock, requested_qty, requested_by_team) VALUES (18, 'leavening', 0.000, 'Yeast (instant)', NULL, 50.000, 'g', '2026-04-23 16:03:43.185669', 'received', NULL, 250.000, 0.000, NULL);


--
-- Data for Name: tasks; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (1, 'kitchen', NULL, '2026-04-28 12:00:57.687228', 'Corporate order for tomorrow''s all-hands. Use ragi-millet recipe. Pack in branded boxes (50/box).', '2026-04-29', 'urgent', 1001, NULL, 'open', 'URGENT: 200 millet cookies for ABC Tech', '2026-04-28 12:00:57.687228', NULL, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (2, 'kitchen', NULL, '2026-04-28 09:00:57.687228', 'Eggless. Customer dropping by 5pm.', '2026-04-28', 'high', 1002, NULL, 'in_progress', 'Bake 3 black-forest cakes for birthday', '2026-04-28 09:00:57.687228', 12, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (3, 'kitchen', '2026-04-27 16:00:57.687228', '2026-04-27 12:00:57.687228', 'Need 4 dozen mixed cookies on the counter by 9am.', '2026-04-26', 'normal', NULL, 'Refilled. Used last batch from cold storage.', 'done', 'Restock cookie display tray', '2026-04-27 16:00:57.687228', 12, 12, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (4, 'kitchen', '2026-04-27 12:00:57.687228', '2026-04-26 12:00:57.687228', 'Trial run before adding to menu. Use 50% jowar flour as discussed.', NULL, 'low', NULL, 'Cancelled â€” supplier didn''t deliver jowar in time.', 'cancelled', 'Test new banana-bread recipe', '2026-04-27 12:00:57.687228', NULL, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (5, 'bakery', NULL, '2026-04-28 06:00:57.687228', 'Front showcase: laddoos, traditional sweets, festive packaging. Decorate with diyas.', '2026-05-05', 'high', NULL, NULL, 'open', 'Set up Diwali display case', '2026-04-28 06:00:57.687228', 11, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (6, 'bakery', NULL, '2026-04-28 10:00:57.687228', 'Walk through CounterPOS, payment flows, refunds.', '2026-04-28', 'normal', NULL, NULL, 'in_progress', 'Train new counter staff on POS', '2026-04-28 10:00:57.687228', 11, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (7, 'bakery', '2026-04-27 14:00:57.687228', '2026-04-27 12:00:57.687228', 'Count opening float and reconcile.', '2026-04-26', 'normal', NULL, 'All matches.', 'done', 'Inventory check for register cash float', '2026-04-27 14:00:57.687228', NULL, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (8, 'delivery', NULL, '2026-04-28 11:00:57.687228', 'Customer reports order arrived 90 mins late. Call them, apologise, send 10% off coupon.', '2026-04-28', 'urgent', 1003, NULL, 'open', 'Customer complaint: late delivery #1003', '2026-04-28 11:00:57.687228', 14, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (9, 'delivery', '2026-04-26 12:00:57.687228', '2026-04-25 12:00:57.687228', 'Drop bike at Surya Auto for routine 5000km service.', '2026-04-26', 'normal', NULL, 'Service done. Receipt filed in petty-cash folder.', 'done', 'Vehicle service due Friday', '2026-04-26 12:00:57.687228', 14, 14, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (10, 'management', NULL, '2026-04-28 04:00:57.687228', 'Need sign-off on 35K spend for Instagram + influencer campaign.', '2026-05-05', 'high', NULL, NULL, 'open', 'Approve Q2 marketing budget', '2026-04-28 04:00:57.687228', 15, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (11, 'management', NULL, '2026-04-28 07:00:57.687228', 'Make sure we have full coverage for Oct 24-28 (Diwali rush).', '2026-04-29', 'normal', NULL, NULL, 'in_progress', 'Review staff schedules for festival week', '2026-04-28 07:00:57.687228', 15, NULL, 13);
INSERT INTO public.tasks (id, assigned_department, completed_at, created_at, description, due_date, priority, related_order_id, resolution_notes, status, title, updated_at, assigned_user_id, completed_by_user_id, created_by_user_id) VALUES (12, 'management', '2026-04-25 12:00:57.687228', '2026-04-24 12:00:57.687228', 'Call about lost shipment of finger millet flour. Ask for replacement or refund.', NULL, 'low', NULL, 'Vendor responded â€” refund issued via UPI.', 'cancelled', 'Vendor: Laxmi Mills follow-up', '2026-04-25 12:00:57.687228', NULL, NULL, 13);


--
-- Data for Name: userRole; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (1, 1, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (2, 2, 2);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (3, 3, 3);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (4, 4, 4);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (5, 5, 5);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (6, 6, 6);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (7, 10, 6);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (8, 11, 2);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (9, 12, 4);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (10, 13, 3);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (11, 14, 5);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (12, 15, 6);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (13, 16, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (14, 17, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (15, 18, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (16, 19, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (17, 20, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (18, 21, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (19, 22, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (20, 23, 1);
INSERT INTO public."userRole" (userroleid, userid, roleid) VALUES (21, 24, 1);


--
-- Name: Cart_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Cart_id_seq"', 1, true);


--
-- Name: Categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Categories_id_seq"', 3, true);


--
-- Name: Orders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Orders_id_seq"', 13, true);


--
-- Name: Product_Available_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Product_Available_id_seq"', 1, false);


--
-- Name: Product_Images_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Product_Images_id_seq"', 10, true);


--
-- Name: Products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Products_id_seq"', 10, true);


--
-- Name: Role_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Role_id_seq"', 6, true);


--
-- Name: User_UserId_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."User_UserId_seq"', 24, true);


--
-- Name: cart_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cart_items_id_seq', 1, true);


--
-- Name: daily_stock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.daily_stock_id_seq', 4, true);


--
-- Name: delivery_issues_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.delivery_issues_id_seq', 3, true);


--
-- Name: delivery_trips_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.delivery_trips_id_seq', 8, true);


--
-- Name: discount_campaigns_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.discount_campaigns_id_seq', 1, false);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.order_items_id_seq', 7, true);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payments_id_seq', 1, false);


--
-- Name: refund_requests_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.refund_requests_id_seq', 1, false);


--
-- Name: shipping_addresses_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.shipping_addresses_id_seq', 8, true);


--
-- Name: supplies_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.supplies_id_seq', 28, true);


--
-- Name: tasks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tasks_id_seq', 12, true);


--
-- Name: userRole_userroleid_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."userRole_userroleid_seq"', 21, true);


--
-- PostgreSQL database dump complete
--



