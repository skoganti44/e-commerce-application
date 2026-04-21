# E-Commerce Frontend

React + Redux Toolkit + Material UI + Axios frontend for the Spring Boot API in `../E_Commerce_Website`.

## Prerequisites

You need **Node.js 18+** installed. Download from https://nodejs.org/ (pick the LTS installer). After install, open a new terminal and verify:

```
node --version
npm --version
```

## First-time setup

From this folder (`ecommerce-frontend`):

```
npm install
```

This reads `package.json` and downloads all dependencies into `node_modules/`.

## Run in development

```
npm run dev
```

The dev server starts at http://localhost:5173.

It proxies any request to `/api/*` to your Spring Boot backend at `http://localhost:8080/*` (see `vite.config.js`). So when the frontend calls `/api/users`, the backend actually receives `GET /users`. Start the Spring Boot app in another terminal.

## Build for production

```
npm run build
```

Outputs static files to `dist/`. Serve them with any static web server or from Spring Boot.

## Project layout

```
ecommerce-frontend/
  index.html              # HTML entry point
  vite.config.js          # Vite + dev proxy config
  src/
    main.jsx              # React entry — wires Provider, Router, Theme
    App.jsx               # Routes
    theme.js              # Material UI theme
    api/
      client.js           # Axios instance (base URL, error handling)
      endpoints.js        # One function per backend endpoint
    store/
      index.js            # Redux store (combines all slices)
      slices/
        sessionSlice.js   # Active user id (persisted in localStorage)
        usersSlice.js     # GET /users
        productsSlice.js  # POST /product
        cartSlice.js      # GET /cart
        ordersSlice.js    # GET /orders
        paymentsSlice.js  # GET /payments
    components/
      Layout.jsx          # AppBar + nav + user selector + <Outlet/>
      UserSelector.jsx    # Dropdown in the AppBar
    pages/
      Home.jsx
      Users.jsx
      Products.jsx
      Cart.jsx
      Orders.jsx
      Payments.jsx
```

## How the pieces fit together

1. You open a page (e.g. `/cart`). React Router renders `Cart.jsx`.
2. The component dispatches a Redux thunk (e.g. `loadCart(userId)`).
3. The thunk calls `api.fetchCart(userId)` → Axios `GET /api/cart?userid=1`.
4. Vite proxy forwards that to `http://localhost:8080/cart?userid=1`.
5. Spring Boot returns JSON. Axios resolves, the thunk fulfils, the slice stores the data.
6. The component re-renders because it's subscribed via `useSelector`.

The active user id is stored in `sessionSlice` and persisted in `localStorage`, so picking a user once remembers it across page refreshes.

## Connecting to the backend

### In development
Nothing to do — the Vite proxy handles it. Just make sure Spring Boot runs on port 8080.

### In production (when deploying separately)
Your browser will block cross-origin requests unless the backend allows them. Add a CORS config class to the Spring Boot project, for example:

```java
// E_Commerce_Website/src/main/java/com/example/groceryapi/config/CorsConfig.java
package com.example.groceryapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "https://your-frontend-domain")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*");
            }
        };
    }
}
```

Then set the API base URL via an environment variable when building the frontend:

```
VITE_API_BASE_URL=https://your-backend-host npm run build
```

## Mapping to backend endpoints

| Page       | Backend call                          |
| ---------- | ------------------------------------- |
| Users      | `GET /users`                          |
| Products   | `POST /product` (create)              |
| Cart       | `GET /cart?userid={id}`               |
| Orders     | `GET /orders?userid={id}`             |
| Payments   | `GET /payments?userid={id}&includeAll`|

The user-id selector in the AppBar controls which user's data the Cart, Orders, and Payments pages show.
