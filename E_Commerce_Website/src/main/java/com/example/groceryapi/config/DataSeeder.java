package com.example.groceryapi.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.DeliveryIssue;
import com.example.groceryapi.model.DeliveryTrip;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.ShippingAddress;
import com.example.groceryapi.model.Task;
import com.example.groceryapi.model.User;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.repository.Repository;

/**
 * Seeds login-ready test users (one per department + a customer + admin) and
 * sample bakery products. User seeding is idempotent per-email so existing
 * databases pick up new test logins on startup. Product seeding only runs
 * when the catalog is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final Repository repository;

    public DataSeeder(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        User admin = seedUsers();
        seedProducts(admin);
        seedSalesTasks();
        seedDeliveryScenarios();
    }

    private User seedUsers() {
        User admin = upsertEmployee("Dhati Admin", "admin@dhati.local", "Admin@123", "management");
        upsertEmployee("Bella Baker",   "bakery@dhati.local",     "Bakery@123",   "bakery");
        upsertEmployee("Karthik Kumar", "kitchen@dhati.local",    "Kitchen@123",  "kitchen");
        upsertEmployee("Sara Sales",    "sales@dhati.local",      "Sales@123",    "sales");
        upsertEmployee("Dinesh Driver", "delivery@dhati.local",   "Delivery@123", "delivery");
        upsertEmployee("Maya Manager",  "management@dhati.local", "Manage@123",   "management");
        upsertCustomer("Anita Customer", "customer@dhati.local",  "Customer@123");
        return admin;
    }

    private User upsertEmployee(String name, String email, String password, String department) {
        User user = repository.findUserByEmail(email).orElseGet(() -> createUser(name, email, password));
        Role role = repository.findRoleByRoleAndDepartment("employee", department).orElseGet(() -> {
            Role r = new Role();
            r.setRole("employee");
            r.setDepartment(department);
            r.setFullName("Employee - " + capitalize(department));
            return repository.saveRole(r);
        });
        ensureUserRole(user, role);
        return user;
    }

    private void upsertCustomer(String name, String email, String password) {
        User user = repository.findUserByEmail(email).orElseGet(() -> createUser(name, email, password));
        Role role = repository.findRoleByName("customer").orElseGet(() -> {
            Role r = new Role();
            r.setRole("customer");
            r.setFullName("Customer");
            return repository.saveRole(r);
        });
        ensureUserRole(user, role);
    }

    private User createUser(String name, String email, String password) {
        User u = new User();
        u.setname(name);
        u.setemail(email);
        u.setpassword(password);
        u.setcreatedat(LocalDateTime.now());
        return repository.saveUser(u);
    }

    private void ensureUserRole(User user, Role role) {
        boolean alreadyLinked = repository.findUserRolesByUserId(user.getuserid()).stream()
                .anyMatch(ur -> ur.getRole() != null && ur.getRole().getId() == role.getId());
        if (alreadyLinked) return;
        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        repository.saveUserRole(ur);
    }

    private void seedProducts(User creator) {
        if (!repository.findAllProducts().isEmpty()) {
            return;
        }

        Category cookies = new Category();
        cookies.setName("Cookies");
        cookies.setDescription("Freshly baked cookies");
        cookies = repository.saveCategory(cookies);

        Category cakes = new Category();
        cakes.setName("Cakes");
        cakes.setDescription("Celebration cakes");
        cakes = repository.saveCategory(cakes);

        Category millets = new Category();
        millets.setName("Millet Specials");
        millets.setDescription("Healthy millet-based bakes");
        millets = repository.saveCategory(millets);

        List<Seed> seeds = List.of(
            new Seed("Classic Chocolate Chip Cookie", "Crunchy edges, gooey center — a timeless favourite.",
                    "49", 50, cookies,
                    "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&w=800&q=80"),
            new Seed("Oatmeal Raisin Cookie", "Hearty rolled oats and plump raisins in every bite.",
                    "39", 40, cookies,
                    "https://images.unsplash.com/photo-1568051243851-f9b136146e97?auto=format&fit=crop&w=800&q=80"),
            new Seed("Double Chocolate Cookie", "Rich cocoa dough loaded with dark chocolate chunks.",
                    "59", 30, cookies,
                    "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=800&q=80"),
            new Seed("Peanut Butter Cookie", "Soft, chewy, and seriously peanut-buttery.",
                    "45", 25, cookies,
                    "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&w=800&q=80"),
            new Seed("Red Velvet Cake", "Classic red velvet with silky cream-cheese frosting.",
                    "699", 10, cakes,
                    "https://images.unsplash.com/photo-1586040140378-b5634cb4c8fc?auto=format&fit=crop&w=800&q=80"),
            new Seed("Black Forest Cake", "Dark cocoa sponge, whipped cream, and black cherries.",
                    "749", 8, cakes,
                    "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=80"),
            new Seed("Vanilla Sponge Cake", "Light-as-air vanilla sponge layered with buttercream.",
                    "549", 12, cakes,
                    "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=800&q=80"),
            new Seed("Chocolate Truffle Cake", "Decadent chocolate ganache over moist chocolate sponge.",
                    "799", 0, cakes,
                    "https://images.unsplash.com/photo-1578775887804-699de7086ff9?auto=format&fit=crop&w=800&q=80"),
            new Seed("Ragi Millet Cookies", "Nutty, wholesome finger-millet cookies, lightly sweetened.",
                    "69", 35, millets,
                    "https://images.unsplash.com/photo-1590080876100-0d2c6f8a8f85?auto=format&fit=crop&w=800&q=80"),
            new Seed("Jowar Millet Cake", "Gluten-free jowar-millet cake with jaggery sweetness.",
                    "599", 6, millets,
                    "https://images.unsplash.com/photo-1621303837174-89787a7d4729?auto=format&fit=crop&w=800&q=80")
        );

        for (Seed s : seeds) {
            Product p = new Product();
            p.setName(s.name);
            p.setDescription(s.description);
            p.setPrice(new BigDecimal(s.price));
            p.setStock(s.stock);
            p.setCategory(s.category);
            p.setCreatedBy(creator);
            p = repository.saveProduct(p);

            ProductImage img = new ProductImage();
            img.setProduct(p);
            img.setImageUrl(s.imageUrl);
            repository.saveProductImage(img);
        }
    }

    /**
     * Seeds a representative set of cross-team tasks created by Sara Sales,
     * covering every status (open, in_progress, done, cancelled), every
     * receiving department, every priority, plus edge cases (past due,
     * relatedOrderId, long description, resolution notes). Idempotent:
     * only runs when the tasks table is empty.
     */
    private void seedSalesTasks() {
        if (!repository.findAllTasks().isEmpty()) {
            return;
        }
        Optional<User> sara = repository.findUserByEmail("sales@dhati.local");
        if (sara.isEmpty()) return;
        User creator = sara.get();
        User bella    = repository.findUserByEmail("bakery@dhati.local").orElse(null);
        User karthik  = repository.findUserByEmail("kitchen@dhati.local").orElse(null);
        User dinesh   = repository.findUserByEmail("delivery@dhati.local").orElse(null);
        User maya     = repository.findUserByEmail("management@dhati.local").orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDate today   = LocalDate.now();
        LocalDate past    = today.minusDays(2);
        LocalDate soon    = today.plusDays(1);
        LocalDate later   = today.plusDays(7);

        // ---- KITCHEN ----
        saveTask(creator, "kitchen", null,
                "URGENT: 200 millet cookies for ABC Tech",
                "Corporate order for tomorrow's all-hands. Use ragi-millet recipe. Pack in branded boxes (50/box).",
                "urgent", "open", soon, 1001L, now, null, null);
        saveTask(creator, "kitchen", karthik,
                "Bake 3 black-forest cakes for birthday",
                "Eggless. Customer dropping by 5pm.",
                "high", "in_progress", today, 1002L, now.minusHours(3), null, null);
        saveTask(creator, "kitchen", karthik,
                "Restock cookie display tray",
                "Need 4 dozen mixed cookies on the counter by 9am.",
                "normal", "done", past, null,
                now.minusDays(1), now.minusHours(20), "Refilled. Used last batch from cold storage.");
        saveTask(creator, "kitchen", null,
                "Test new banana-bread recipe",
                "Trial run before adding to menu. Use 50% jowar flour as discussed.",
                "low", "cancelled", null, null,
                now.minusDays(2), now.minusDays(1), "Cancelled — supplier didn't deliver jowar in time.");

        // ---- BAKERY (counter) ----
        saveTask(creator, "bakery", bella,
                "Set up Diwali display case",
                "Front showcase: laddoos, traditional sweets, festive packaging. Decorate with diyas.",
                "high", "open", later, null, now.minusHours(6), null, null);
        saveTask(creator, "bakery", bella,
                "Train new counter staff on POS",
                "Walk through CounterPOS, payment flows, refunds.",
                "normal", "in_progress", today, null, now.minusHours(2), null, null);
        saveTask(creator, "bakery", null,
                "Inventory check for register cash float",
                "Count opening float and reconcile.",
                "normal", "done", past, null,
                now.minusDays(1), now.minusDays(1).plusHours(2), "All matches.");

        // ---- DELIVERY ----
        saveTask(creator, "delivery", dinesh,
                "Customer complaint: late delivery #1003",
                "Customer reports order arrived 90 mins late. Call them, apologise, send 10% off coupon.",
                "urgent", "open", today, 1003L, now.minusHours(1), null, null);
        saveTask(creator, "delivery", dinesh,
                "Vehicle service due Friday",
                "Drop bike at Surya Auto for routine 5000km service.",
                "normal", "done", past, null,
                now.minusDays(3), now.minusDays(2),
                "Service done. Receipt filed in petty-cash folder.");

        // ---- MANAGEMENT ----
        saveTask(creator, "management", maya,
                "Approve Q2 marketing budget",
                "Need sign-off on 35K spend for Instagram + influencer campaign.",
                "high", "open", later, null, now.minusHours(8), null, null);
        saveTask(creator, "management", maya,
                "Review staff schedules for festival week",
                "Make sure we have full coverage for Oct 24-28 (Diwali rush).",
                "normal", "in_progress", soon, null, now.minusHours(5), null, null);
        saveTask(creator, "management", null,
                "Vendor: Laxmi Mills follow-up",
                "Call about lost shipment of finger millet flour. Ask for replacement or refund.",
                "low", "cancelled", null, null,
                now.minusDays(4), now.minusDays(3), "Vendor responded — refund issued via UPI.");
    }

    /**
     * Seeds realistic delivery scenarios so a driver can exercise the full
     * day-to-day flow end-to-end:
     *
     *   1. Pickup queue   — online order, kitchen DONE, no trip yet
     *   2. Picked up      — driver has it, hasn't left counter
     *   3. Out for delivery — en route, OTP issued
     *   4. Delivered (OTP + COD)        — happy path
     *   5. Delivered (photo proof only) — alternative proof
     *   6. Failed: customer not home
     *   7. Failed: refused
     *   8. Failed: damaged
     *
     * Plus three logged issues (vehicle breakdown, traffic delay, accident),
     * one tied to an in-flight trip. Idempotent — only runs when no trips
     * exist yet.
     */
    private void seedDeliveryScenarios() {
        // Idempotency: scenario customers are created with deterministic emails,
        // so if the first one already exists we've seeded before — skip.
        if (repository.findUserByEmail("delivery1@dhati.local").isPresent()) return;

        User driver = repository.findUserByEmail("delivery@dhati.local").orElse(null);
        if (driver == null) return;

        // Eight customers — one per scenario, deterministic emails so re-runs are no-ops.
        User c1 = upsertCustomerSilent("Aanya Kapoor",   "delivery1@dhati.local", "Customer@123");
        User c2 = upsertCustomerSilent("Bhavesh Gupta",  "delivery2@dhati.local", "Customer@123");
        User c3 = upsertCustomerSilent("Chitra Iyer",    "delivery3@dhati.local", "Customer@123");
        User c4 = upsertCustomerSilent("Deepak Reddy",   "delivery4@dhati.local", "Customer@123");
        User c5 = upsertCustomerSilent("Esha Sharma",    "delivery5@dhati.local", "Customer@123");
        User c6 = upsertCustomerSilent("Farhan Khan",    "delivery6@dhati.local", "Customer@123");
        User c7 = upsertCustomerSilent("Geeta Menon",    "delivery7@dhati.local", "Customer@123");
        User c8 = upsertCustomerSilent("Harish Rao",     "delivery8@dhati.local", "Customer@123");

        LocalDateTime now = LocalDateTime.now();

        // Scenario 1: ready in pickup queue (no trip yet)
        Orders o1 = newOrder(c1, "499", "online", "confirmed", "done", "Ring twice");
        addAddress(c1, o1, "Aanya Kapoor",  "9876500001", "12 MG Road", "Apt 4B",
                "Indiranagar", "Bengaluru", "560038", "Leave at the door if no answer");

        // Scenario 2: trip picked_up (driver has it, hasn't left)
        Orders o2 = newOrder(c2, "699", "online", "confirmed", "picked_up", null);
        addAddress(c2, o2, "Bhavesh Gupta", "9876500002", "47 Brigade Road", null,
                "MG Road", "Bengaluru", "560001", null);
        newTrip(o2, driver, "picked_up", null, null, null, null, null, null,
                now.minusMinutes(15), null, null, null, null, null);

        // Scenario 3: out_for_delivery — en route with OTP
        Orders o3 = newOrder(c3, "899", "online", "confirmed", "out_for_delivery", null);
        addAddress(c3, o3, "Chitra Iyer",   "9876500003", "8 Church Street", "Floor 3",
                "Ashok Nagar", "Bengaluru", "560001", "Call when you arrive");
        newTrip(o3, driver, "out_for_delivery", "4729", null,
                null, null, null, null,
                now.minusMinutes(45), now.minusMinutes(10), null, null, null, null);

        // Scenario 4: delivered with OTP + COD (happy path, includes tip)
        Orders o4 = newOrder(c4, "549", "online", "delivered", "delivered", null);
        addAddress(c4, o4, "Deepak Reddy",  "9876500004", "55 Residency Road", null,
                "Shanti Nagar", "Bengaluru", "560025", null);
        newTrip(o4, driver, "delivered", "1234", null,
                new BigDecimal("549.00"), new BigDecimal("25.00"), new BigDecimal("4.2"),
                "Delivered to customer in person.",
                now.minusHours(3), now.minusHours(2).minusMinutes(45),
                now.minusHours(2).minusMinutes(15), null, null, null);

        // Scenario 5: delivered with photo proof (no OTP)
        Orders o5 = newOrder(c5, "1199", "online", "delivered", "delivered", "Leave at door");
        addAddress(c5, o5, "Esha Sharma",   "9876500005", "21 Koramangala 4th Block", null,
                "Koramangala", "Bengaluru", "560034", "Leave at security desk");
        newTrip(o5, driver, "delivered", null,
                "https://photos.dhati.local/proof/order-105.jpg",
                null, null, new BigDecimal("6.8"),
                "Customer not at door — handed to building security as per instructions.",
                now.minusHours(5), now.minusHours(4).minusMinutes(40),
                now.minusHours(4).minusMinutes(15), null, null, null);

        // Scenario 6: failed — customer not home
        Orders o6 = newOrder(c6, "299", "online", "confirmed", "delivery_failed", null);
        addAddress(c6, o6, "Farhan Khan",   "9876500006", "9 HSR Sector 6", null,
                "HSR Layout", "Bengaluru", "560102", null);
        newTrip(o6, driver, "failed", "9999", null, null, null, new BigDecimal("3.5"),
                "Knocked, called twice — no answer for 10 mins.",
                now.minusHours(7), now.minusHours(6).minusMinutes(30),
                null, now.minusHours(6).minusMinutes(15), "customer_not_home", null);

        // Scenario 7: failed — customer refused
        Orders o7 = newOrder(c7, "799", "online", "confirmed", "delivery_failed", null);
        addAddress(c7, o7, "Geeta Menon",   "9876500007", "33 Whitefield Main", null,
                "Whitefield", "Bengaluru", "560066", null);
        newTrip(o7, driver, "failed", null, null, null, null, new BigDecimal("9.1"),
                "Customer says she ordered by mistake, refused delivery.",
                now.minusDays(1).plusHours(2), now.minusDays(1).plusHours(2).plusMinutes(30),
                null, now.minusDays(1).plusHours(3), "refused", null);

        // Scenario 8: failed — damaged in transit
        Orders o8 = newOrder(c8, "649", "online", "confirmed", "delivery_failed", null);
        addAddress(c8, o8, "Harish Rao",    "9876500008", "77 JP Nagar Phase 7", null,
                "JP Nagar", "Bengaluru", "560078", null);
        DeliveryTrip damagedTrip = newTrip(o8, driver, "failed", null, null,
                null, null, new BigDecimal("5.5"),
                "Cake box dropped getting off the bike. Returned to bakery for refund.",
                now.minusDays(2).plusHours(1), now.minusDays(2).plusHours(1).plusMinutes(20),
                null, now.minusDays(2).plusHours(1).plusMinutes(35), "damaged", null);

        // ---- Delivery issues (3): one linked to the damaged trip ----
        newIssue(driver, null, "vehicle_breakdown",
                "Tire puncture near Silk Board flyover, used spare. Need to get patched soon.",
                now.minusDays(3));
        newIssue(driver, null, "traffic_delay",
                "ORR closure caused 40-min delay between Marathahalli and Sarjapur.",
                now.minusDays(1).plusHours(4));
        newIssue(driver, damagedTrip, "accident",
                "Bike skid on wet road, dropped delivery box (see trip notes). Self ok.",
                now.minusDays(2).plusHours(1).plusMinutes(40));
    }

    private User upsertCustomerSilent(String name, String email, String password) {
        return repository.findUserByEmail(email).orElseGet(() -> {
            User u = createUser(name, email, password);
            Role r = repository.findRoleByName("customer").orElseGet(() -> {
                Role nr = new Role();
                nr.setRole("customer");
                nr.setFullName("Customer");
                return repository.saveRole(nr);
            });
            ensureUserRole(u, r);
            return u;
        });
    }

    private Orders newOrder(User customer, String total, String channel,
                             String status, String kitchenStatus, String customerNotes) {
        Orders o = new Orders();
        o.setUser(customer);
        o.setTotalAmount(new BigDecimal(total));
        o.setChannel(channel);
        o.setStatus(status);
        o.setKitchenStatus(kitchenStatus);
        o.setCustomerNotes(customerNotes);
        return repository.saveOrder(o);
    }

    private void addAddress(User customer, Orders order, String fullName, String phone,
                             String line1, String line2, String landmark, String city,
                             String pincode, String instructions) {
        ShippingAddress sa = new ShippingAddress();
        sa.setUser(customer);
        sa.setOrder(order);
        sa.setFullName(fullName);
        sa.setPhone(phone);
        sa.setLine1(line1);
        sa.setLine2(line2);
        sa.setLandmark(landmark);
        sa.setCity(city);
        sa.setState("Karnataka");
        sa.setPincode(pincode);
        sa.setCountry("India");
        sa.setInstructions(instructions);
        sa.setAddressType("home");
        repository.saveShippingAddress(sa);
    }

    private DeliveryTrip newTrip(Orders order, User driver, String status,
                                  String otp, String photoUrl,
                                  BigDecimal cod, BigDecimal tip, BigDecimal distanceKm,
                                  String notes,
                                  LocalDateTime pickedUpAt, LocalDateTime outAt,
                                  LocalDateTime deliveredAt, LocalDateTime failedAt,
                                  String failureReason, LocalDateTime codCollectedAt) {
        DeliveryTrip t = new DeliveryTrip();
        t.setOrder(order);
        t.setDriver(driver);
        t.setStatus(status);
        t.setOtpCode(otp);
        t.setPhotoProofUrl(photoUrl);
        t.setCodAmount(cod);
        t.setTipAmount(tip);
        t.setDistanceKm(distanceKm);
        t.setNotes(notes);
        t.setPickedUpAt(pickedUpAt);
        t.setOutAt(outAt);
        t.setDeliveredAt(deliveredAt);
        t.setFailedAt(failedAt);
        t.setFailureReason(failureReason);
        if (cod != null && codCollectedAt == null) {
            t.setCodCollectedAt(deliveredAt);
        } else {
            t.setCodCollectedAt(codCollectedAt);
        }
        LocalDateTime created = pickedUpAt != null ? pickedUpAt : LocalDateTime.now();
        t.setCreatedAt(created);
        LocalDateTime updated = deliveredAt != null ? deliveredAt
                : failedAt != null ? failedAt
                : outAt != null ? outAt : created;
        t.setUpdatedAt(updated);
        return repository.saveTrip(t);
    }

    private void newIssue(User driver, DeliveryTrip trip, String issueType,
                           String description, LocalDateTime reportedAt) {
        DeliveryIssue i = new DeliveryIssue();
        i.setDriver(driver);
        i.setTrip(trip);
        i.setIssueType(issueType);
        i.setDescription(description);
        i.setReportedAt(reportedAt);
        repository.saveIssue(i);
    }

    private void saveTask(User createdBy, String dept, User assignee,
                          String title, String description, String priority,
                          String status, LocalDate dueDate, Long relatedOrderId,
                          LocalDateTime createdAt, LocalDateTime completedAt,
                          String resolutionNotes) {
        Task t = new Task();
        t.setCreatedBy(createdBy);
        t.setAssignedToDepartment(dept);
        t.setAssignedToUser(assignee);
        t.setTitle(title);
        t.setDescription(description);
        t.setPriority(priority);
        t.setStatus(status);
        t.setDueDate(dueDate);
        t.setRelatedOrderId(relatedOrderId);
        t.setCreatedAt(createdAt);
        t.setUpdatedAt(completedAt != null ? completedAt : createdAt);
        if ("done".equals(status) || "cancelled".equals(status)) {
            t.setCompletedAt(completedAt);
            t.setCompletedBy(assignee);
            t.setResolutionNotes(resolutionNotes);
        }
        repository.saveTask(t);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record Seed(String name, String description, String price,
                        int stock, Category category, String imageUrl) {}
}
