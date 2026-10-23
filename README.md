# SportGrid - Turf Booking System

Java Swing desktop application, Java backend (JDBC), and MySQL database.

---

## Running the Desktop Application

### 1. Launch the Desktop GUI (Normal Mode)
To launch the graphical Java Swing application:

```bash
# Compile with javac
javac -cp "lib/*" -d out $(find src -name "*.java")

# Run the Swing application
java -cp "out:lib/*" main.Main
```
*(On Windows PowerShell/CMD, use semicolons `;` instead of colons `:` for the classpath: `java -cp "out;lib/*" main.Main`)*

- **What it does**: `main.Main` initializes font antialiasing and theme defaults, verifies or initializes the administrator account, and displays the graphical **SportGrid Login** window (`ui.LoginFrame`).
- **Interactive features**: Log in as Player, Turf Owner, or Admin; browse venues; book time slots; simulate payments; cancel bookings; rate turfs and submit reviews; manage venues and approve listings.

### 2. Run the Automated Backend Demo (Console Mode)
To run the automated backend regression verification suite without opening the GUI:

```bash
java -cp "out:lib/*" main.Main --demo
```

- **What it does**: `main.Main --demo` executes a 12-stage headless verification suite directly in your terminal and **does not launch the GUI**.
- **What it tests**: Account registration, turf creation, admin approval, hourly slot generation, searching, atomic booking, double-booking rejection, payment failure rollback, owner earnings calculation, booking cancellation with slot release and refund, and turf reviews with community rating recalculation.

### 3. Run Automated Unit & Integration Tests (Maven)
You can also run the full test suite using the included Maven Wrapper:

```bash
./mvnw test
```

---

## System Requirements & Setup

### Requirements
- **Java**: JDK 17 or higher (tested with JDK 21 LTS)
- **Database**: MySQL Server 8.0 or higher
- **IDE (Optional)**: IntelliJ IDEA Community, Eclipse, or NetBeans

### Database Setup
1. **Start MySQL Server** on your machine.
2. **Import Schema**: Run `sql/schema.sql` to create the `sportgrid` database and all required tables:
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
   *(Or open `sql/schema.sql` in MySQL Workbench and execute the script).*

3. **Configure Database Connection (`db.properties`)**:
   Copy `db.properties.example` to `db.properties` in the project root:
   ```bash
   cp db.properties.example db.properties
   ```
   Edit `db.properties` with your local MySQL credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/sportgrid?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
   db.user=root
   db.password=your_mysql_password_here
   ```
   > **Note**: `db.properties` is listed in `.gitignore` to prevent committing local passwords to source control.

---

## Administrator Account Configuration

### Default Credentials
On normal startup, `main.Main` safely verifies or auto-initializes the default administrator account:
- **Admin Email**: `admin@sportgrid.com`
- **Admin Password**: `Admin@123`

### Custom Administrator Configuration
The administrator credentials can be configured before launch without modifying code:
- **Via `db.properties`**:
  ```properties
  admin.email=admin@sportgrid.com
  admin.password=YourCustomPassword123
  ```
- **Via Environment Variables**:
  - `SPORTGRID_ADMIN_EMAIL`
  - `SPORTGRID_ADMIN_PASSWORD`

### Safe Startup Guarantees
- **Non-Overwriting**: If an administrator account already exists, the application verifies it and **never overwrites** existing passwords or account details.
- **Duplicate Prevention**: If any administrator account exists in the database, the startup initializer skips creating redundant admin records.
- **Privacy**: Passwords are never output in console logs or error messages.
- **Connection Resilience**: If MySQL is temporarily offline during startup, the application logs a non-fatal warning and continues to the login screen without crashing.

---

## Layers & Architecture

```
ui  ->  service  ->  dao  ->  db (MySQL)
             \-> model (plain objects used everywhere)
```

| Package | Responsibility |
|---|---|
| `model` | Core domain entities: `User` (abstract) with `Player`, `TurfOwner`, `Admin`; `Turf`, `TimeSlot`, `Booking`; `Payment` (abstract) with `UPIPayment`, `CardPayment`; `Review`; enums (`Role`, `BookingStatus`, `PaymentStatus`). |
| `dao` | Data Access Objects containing all SQL queries. One DAO per entity table (`UserDAO`, `TurfDAO`, `TimeSlotDAO`, `BookingDAO`, `PaymentDAO`, `ReviewDAO`). |
| `service` | Business logic and validation rules: `AuthService`, `BookingService`, `TurfService`, `AdminService`. |
| `db` | Database connectivity: `DBConnection` (loads `db.properties`) and exception hierarchy (`DataAccessException`, `DuplicateEntryException`). |
| `ui` | Swing user interface screens. Calls only `service` classes; never communicates directly with SQL. |

---

## Service API Cheat Sheet

```java
AuthService auth = new AuthService();
BookingService bookings = new BookingService();
TurfService turfs = new TurfService();
AdminService admin = new AdminService();
```

| Action | Service Call |
|---|---|
| Sign up (Player / Owner) | `auth.register(name, email, phone, password, Role.PLAYER or Role.OWNER)` |
| Log in | `User u = auth.login(email, password)` (check `u.getRole()` to route to dashboard) |
| Search turfs | `bookings.searchTurfs(sport, location)` (null/blank = all approved) |
| View available slots | `bookings.getAvailableSlots(turfId)` |
| Book slot & pay | `bookings.bookSlot(player, slotId, payment)` (`UPIPayment` or `CardPayment`) |
| Cancel booking | `bookings.cancelBooking(player, bookingId)` (releases slot, refunds payment) |
| Player bookings history | `bookings.getBookingHistory(player)` |
| Owner: add turf | `turfs.addTurf(owner, name, location, sport, price)` (starts unapproved) |
| Owner: add slots | `turfs.generateHourlySlots(owner, turfId, date, 6, 22)` or `turfs.addSlot(...)` |
| Owner: dashboard | `turfs.loadOwnerDashboard(owner)`, `turfs.getOwnerBookings(owner)` |
| Rate & review turf | `turfs.addReview(player, turfId, rating, comment)`, `turfs.getReviews(turfId)` |
| Admin moderation | `admin.getPendingTurfs(u)`, `approveTurf`, `rejectTurf`, `listUsers`, `removeUser` |

---

## OOP Concepts Demonstrated

- **Encapsulation**: Domain state fields are `private`. State mutations occur strictly through business methods (e.g. `TimeSlot.book()`, `TimeSlot.release()`, `Payment.refund()`).
- **Inheritance**: Class hierarchies rooted in abstract models:
  - `User` &rarr; `Player`, `TurfOwner`, `Admin`
  - `Payment` &rarr; `UPIPayment`, `CardPayment`
  - `DataAccessException` &rarr; `DuplicateEntryException`
- **Abstraction**:
  - `User` defines abstract `getDashboard()`.
  - `Payment` defines abstract `processPayment()`, `getMethod()`, `getReference()`.
- **Polymorphism**:
  - **Dynamic dispatch**: `BookingService.bookSlot()` invokes `payment.processPayment()`. At runtime, `UPIPayment` verifies UPI regex format while `CardPayment` verifies Luhn checksum and expiry date.
  - **Overriding**: Subclass-specific `getDashboard()` implementations.
  - **Overloading**: Multiple convenience constructors in `RateTurfDialog` and `AuthService.initializeDefaultAdmin()`.
- **Interfaces**:
  - `ui.theme.ThemeChangeListener`: Decoupled Observer pattern interface implemented by 11 Swing views for runtime Light/Dark theme switching.
- **Exception Handling**:
  - Custom domain exception hierarchy: `DataAccessException`, `DuplicateEntryException`, `PaymentFailedException`.
  - Automatic rollback on transactional failures (`conn.rollback()`).
- **Collections**:
  - Extensive use of `List<T>`, `ArrayList<T>`, `Map<Integer, Turf>`, `LinkedHashMap` (order-preserving deduplication), and `Optional<T>`.
  - Functional stream processing (`.filter()`, `.map()`, `.count()`).
- **Database Connectivity**:
  - Centralized connection management in `DBConnection`.
  - 100% parameterized queries via `PreparedStatement` preventing SQL injection.
  - Manual ACID transaction demarcations (`setAutoCommit(false)`, `commit()`, `rollback()`).

---

## Important Testing & Simulation Notes

- **Simulated Payments**: Payments are simulated in-memory; no real financial transactions take place.
- **Valid Test Card**: Card number `4111 1111 1111 1111`, any valid future expiry (e.g. `12/30`), CVV `123`.
- **Simulated UPI**: Any valid format like `player@okhdfc`, `user@upi`, or `demo@paytm`.
- **Double Booking Protection**: Atomic SQL row updates inside an ACID transaction (`TimeSlotDAO.markBooked`) guarantee that concurrent attempts on the same slot result in only one confirmed booking.
- **Password Security**: Passwords are never stored in plaintext; all credentials use PBKDF2 with HMAC-SHA256 and a 16-byte cryptographically secure salt.
