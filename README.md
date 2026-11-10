# Inventory and Order Management API (Core Java)

A portfolio REST API written without Spring Boot. It uses Java's built-in `HttpServer`, JDBC, PostgreSQL, Jackson for JSON, and Maven for dependency management.

## Features

- REST endpoints implemented using the JDK HTTP server
- PostgreSQL schema and JDBC prepared statements
- Product search, filtering, pagination, stock adjustments, and soft deactivation
- Transactional order creation with row locks to prevent overselling
- Purchase-time item snapshots, order totals, ownership checks, and cancellation with one-time stock restoration
- HTTP Basic demo authentication with separate admin and customer roles
- Input validation, useful HTTP errors, and JUnit tests

## Easiest setup: Docker Desktop

You do not need to install Java or Maven when using Docker. Install Docker Desktop for Windows and start it. If prompted, enable its WSL 2 backend and restart Windows if requested.

1. Copy `.env.example` to a new file named `.env` in this project folder.
2. Open `.env` in Notepad and replace all `CHANGE_ME` values with passwords you choose. Keep `.env` private; it is ignored by Git.
3. Open PowerShell in this project folder and run:

   ```powershell
   docker compose up --build
   ```

4. Wait for the log line `Inventory API listening at http://localhost:8080`.
5. Check `http://localhost:8080/actuator/health` in your browser; it should show `{"status":"UP"}`.

The first Docker start downloads build and database images, so it can take several minutes. Leave that PowerShell window open while using the API. To stop, run `docker compose down` in a second PowerShell window opened in this folder. This preserves the database volume. `docker compose down -v` also erases its saved data.

## Try the API

In a second PowerShell window in this folder, use the passwords you put in `.env`.

Create a product as admin:

```powershell
curl.exe -u admin:YOUR_ADMIN_PASSWORD -H "Content-Type: application/json" -d '{"sku":"COF-1","name":"Coffee","category":"Grocery","price":4.50,"stockQuantity":10}' http://localhost:8080/api/v1/products
```

List products without logging in:

```powershell
curl.exe "http://localhost:8080/api/v1/products?q=cof&category=Grocery&page=0&size=20"
```

Place an order as customer. Use the product ID from the create response:

```powershell
curl.exe -u customer:YOUR_CUSTOMER_PASSWORD -H "Content-Type: application/json" -d '{"items":[{"productId":1,"quantity":2}]}' http://localhost:8080/api/v1/orders
```

## API endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/v1/products` | Public | Search and page active products (`q`, `category`, `page`, `size`) |
| GET | `/api/v1/products/{id}` | Public | Product details |
| POST | `/api/v1/products` | ADMIN | Create product |
| PUT | `/api/v1/products/{id}` | ADMIN | Update product and quantity |
| PATCH | `/api/v1/products/{id}/stock` | ADMIN | Add or subtract stock using `{"adjustment":5}` |
| DELETE | `/api/v1/products/{id}` | ADMIN | Deactivate without deleting order history |
| POST | `/api/v1/orders` | CUSTOMER or ADMIN | Place an order |
| GET | `/api/v1/orders/me` | Authenticated | List your orders |
| GET | `/api/v1/orders/{id}` | Owner or ADMIN | Read an order |
| POST | `/api/v1/orders/{id}/cancel` | Owner or ADMIN | Cancel once and restore stock |
| GET | `/api/v1/orders` | ADMIN | List all orders |

## Run directly on Windows

Install JDK 17 or newer, Maven, and PostgreSQL. Create database `inventory_db`. In PowerShell, set database and demo account settings (choose your own passwords):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/inventory_db"
$env:DB_USERNAME = "inventory"
$env:DB_PASSWORD = "your-database-password"
$env:ADMIN_PASSWORD = "your-admin-password"
$env:CUSTOMER_PASSWORD = "your-customer-password"
```

Then package and run:

```powershell
mvn package
java --add-modules jdk.httpserver -jar target/inventory-order-api-java-1.0.0.jar
```

The application runs idempotent statements in `src/main/resources/schema.sql` on startup; it does not delete existing data. The demo HTTP Basic authentication is for local learning, not a production identity system. Use HTTPS and a proper identity provider before deploying publicly.

## Tests

Run `mvn test`. The included JUnit tests check request validation. They do not require a PostgreSQL server. The project should be built and the API flows checked before listing it on a resume.
