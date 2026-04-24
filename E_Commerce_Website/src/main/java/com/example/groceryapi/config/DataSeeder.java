package com.example.groceryapi.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.User;
import com.example.groceryapi.repository.Repository;

/**
 * Seeds sample bakery products (cookies + cakes) the first time the app starts
 * with an empty Products table. Idempotent — runs only when catalog is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final Repository repository;

    public DataSeeder(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (!repository.findAllProducts().isEmpty()) {
            return;
        }

        User creator = repository.findAllUsers().stream().findFirst().orElseGet(() -> {
            User u = new User();
            u.setname("Dhati Admin");
            u.setemail("admin@dhati.local");
            u.setpassword("Admin@123");
            u.setcreatedat(LocalDateTime.now());
            return repository.saveUser(u);
        });

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

    private record Seed(String name, String description, String price,
                        int stock, Category category, String imageUrl) {}
}
