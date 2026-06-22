# File Overview — Fleet Management System

This document describes every file in the project: what it contains, what it does, and where it is referenced or used by other parts of the system.

---

## Root-level Files

### `pom.xml`
Maven project descriptor. Declares all dependencies (Spring Boot, JavaFX, Hibernate, MySQL, H2,
Spring Security Crypto, JUnit, Mockito), build plugins (Spring Boot Maven Plugin, JavaFX Maven Plugin,
Hibernate Bytecode Enhancement Plugin), and a Maven profile for Apple Silicon (`mac-aarch64`) that
adds platform-native JavaFX classifiers.
**Used by**: Maven at build time; IDEs for classpath resolution.

### `run.sh`
Shell script for running the fat JAR on macOS Apple Silicon without `mvn`. Manually constructs the
`--module-path` pointing to the arm64 JavaFX JARs in `~/.m2/` and passes `--add-modules javafx.controls,javafx.fxml`.
**Used by**: Developers on Apple Silicon when they want to run the built JAR directly.

### `build-mac-app.sh`
Shell script that automates the three-step macOS packaging pipeline: (1) `mvn clean package -DskipTests`
to compile the fat JAR, (2) copy arm64 JavaFX native module JARs into `target/javafx-mods/`,
(3) invoke `jpackage --type dmg` to produce `dist/FleetManagement-1.0.dmg`.
**Used by**: Developers building the distributable macOS installer.

---

## `src/main/resources/`

### `application.properties`
Spring Boot configuration file. Configures the MySQL datasource URL, username, password, and JDBC
driver. Sets `spring.jpa.hibernate.ddl-auto=update` (schema auto-migration) and
`spring.jpa.show-sql=false`. Disables `data.sql` auto-execution (`spring.sql.init.mode=never`).
Tunes HikariCP pool parameters (`minimum-idle`, `maximum-pool-size`, `connection-timeout`).
**Used by**: Spring Boot at startup to configure the datasource, JPA, and connection pool.

### `data.sql`
Legacy H2 seed script containing `MERGE INTO ... KEY(id)` statements for test users, trucks,
trailers, customers, and documents. Currently **not executed** in production because
`spring.sql.init.mode=never` is set. Was used during early development when H2 was the primary
database. Retained for reference.
**Used by**: Nothing at runtime. Was used by H2 during early development.

### `fxml/login-view.fxml`
FXML layout for the login screen. Defines a `VBox` with a title label, username `TextField`
(`fx:id="usernameField"`), password `PasswordField` (`fx:id="passwordField"`), and a login `Button`
that calls `#handleLogin`. References `LoginController` as its controller class.
**Used by**: `FleetManagementApplication.showLoginScreen()` via `FXMLLoader`.

### `fxml/main-view.fxml`
FXML layout for the main application shell. Defines a `BorderPane` with a top header bar (welcome
label, role label), a left sidebar navigation menu (buttons for Trucks, Trailers, Documents, Drivers,
Trips, Admin Panel, Logout), and a center `StackPane` (`fx:id="contentArea"`) where module views
are dynamically swapped. The Admin Panel button has `fx:id="adminButton"` so it can be hidden for
non-admin users. References `MainController` as its controller class.
**Used by**: `FleetManagementApplication.showMainScreen()` via `FXMLLoader`.

### `images/logo.png`
Application window icon (PNG format).
**Used by**: `FleetManagementApplication.start()` — loaded as `primaryStage.getIcons().add(...)`.

---

## `src/main/java/org/example/fleetmanagement/`

### `FleetManagementApplication.java`
The application entry point. Extends `javafx.application.Application`. The `start(Stage)` override
boots the Spring context via `SpringApplicationBuilder` (with `.headless(false)`), sets the window
icon, and calls `showLoginScreen()`. The `stop()` override closes the Spring context and exits JavaFX.
Provides two static helpers — `showLoginScreen()` and `showMainScreen()` — that load FXML views and
wire their controllers through Spring by using `loader.setControllerFactory(springContext::getBean)`.
**Used by**: JVM entry point (`main()`). `LoginController` calls `showMainScreen()` on successful
login. `MainController` calls `showLoginScreen()` on logout.

---

## `config/`

### `config/SecurityConfig.java`
Spring `@Configuration` class. Declares the `PasswordEncoder` bean as a `BCryptPasswordEncoder`
instance. No HTTP security is configured — only the BCrypt encoder is needed for a desktop app.
**Used by**: Spring IoC container; the `PasswordEncoder` bean is injected into `AuthenticationService`,
`UserService`, and `DataInitializer`.

### `config/JpaConfig.java`
Spring `@Configuration` class. Annotated with `@EnableJpaRepositories(basePackages = "...repository")`
and `@EntityScan(basePackages = "...model")` to tell Spring Data JPA where to find repositories and
entities. Without this class the auto-configuration would still work, but this makes the scan
packages explicit.
**Used by**: Spring context at startup.

### `config/DataInitializer.java`
Spring `@Component` that implements `CommandLineRunner`. Runs once at startup. If the `app_user`
table is empty, it creates two default users: `admin` (role ADMINISTRATOR) and `logistyk` (role
LOGISTICIAN), both with BCrypt-hashed passwords. Prevents duplicate seeding on subsequent starts.
**Used by**: Spring Boot's `CommandLineRunner` mechanism. Depends on `UserRepository` and
`PasswordEncoder`.

---

## `model/`

All model classes are JPA entities annotated with `@Entity` and `@Table`. Hibernate generates the
database schema from them.

### `model/User.java`
Entity for application users (`app_user` table). Fields: `id`, `username`, `password` (BCrypt hash),
`role` (enum: `ADMINISTRATOR`, `LOGISTICIAN`), `fullName`. The password field is annotated
`@Column(length=100)`.
**Used by**: `UserRepository`, `UserService`, `AuthenticationService`, `DataInitializer`,
`UserManagementController`.

### `model/Truck.java`
Entity for tractor units (`truck` table). Fields: `id`, `brand`, `registrationNumber`,
`registrationCountry`, `productionYear`, `status` (string constant: `STATUS_AVAILABLE`, `STATUS_ON_TRIP`,
`STATUS_IN_REPAIR`), `currentLocation`, `cargoDescription`, plus a `@OneToMany` collection of
`TruckAttachment`.
**Used by**: `TruckRepository`, `TruckService`, `TruckManagementController`, `TripManagementController`
(for selecting a truck when creating a trip), `DocumentManagementController` (trucks are displayed
in the document truck combo box).

### `model/Trailer.java`
Entity for semi-trailers (`trailer` table). Fields: `id`, `registrationNumber`, `brand`,
`registrationCountry`, `type`, `status`, `currentLocation`, plus `@OneToMany` collections of
`TrailerNote` and `TrailerAttachment`.
**Used by**: `TrailerRepository`, `TrailerService`, `TrailerManagementController`,
`TripManagementController` (optional trailer selection per trip).

### `model/Driver.java`
Entity for drivers (`driver` table). Fields: `id`, `fullName`, `licenseNumber`, `licenseCategory`,
`status` (string constants: `STATUS_AVAILABLE`, `STATUS_ON_TRIP`, `STATUS_ON_LEAVE`), `notes`,
plus `@OneToMany` collections of `DriverPhone` and `DriverAttachment`.
**Used by**: `DriverRepository`, `DriverService`, `DriverManagementController`,
`TripManagementController` (optional driver selection per trip).

### `model/Trip.java`
Entity for transport routes (`trip` table). Fields: `id`, `origin`, `destination`, `plannedDeparture`,
`plannedArrival`, `startTime`, `actualArrival`, `status` (enum: `PLANNED`, `IN_PROGRESS`,
`COMPLETED`, `CANCELLED`), `cargoDescription`, `distance`, plus `@ManyToOne` to `Truck`, `Trailer`,
`Driver`, `Customer`, plus `@OneToMany` collections of `TripAttachment` and `TripNote`.
**Used by**: `TripRepository`, `TripService`, `TripManagementController`.

### `model/Customer.java`
Entity for customers (cargo recipients, `customer` table). Fields: `id`, `name`. Overrides
`toString()` to return the name, so instances display correctly in JavaFX `ComboBox` controls.
**Used by**: `CustomerRepository`, `CustomerService`, `TripManagementController` (customer selection
in trip form).

### `model/Document.java`
Entity for truck documents (`document` table). Fields: `id`, `@ManyToOne truck`, `documentType`
(enum: `INSURANCE`, `TECHNICAL_INSPECTION`, `TACHOGRAPH_CALIBRATION`, `OTHER`), `expiryDate`,
`description`, `fileName`, plus a `@Lob @Basic(fetch=LAZY) byte[] fileData` for the stored PDF.
**Used by**: `DocumentRepository`, `DocumentService`, `DocumentManagementController`.

### `model/DriverDocument.java`
Entity for driver-specific documents (e.g. driving license, `driver_document` table). Fields similar
to `Document` but with a `@ManyToOne driver` instead of `truck`. Includes its own `documentType`
enum (`DRIVING_LICENSE`, `ADR_CERTIFICATE`, `MEDICAL_CERTIFICATE`, `OTHER`), `expiryDate`,
`description`, `fileName`, and `@Lob @Basic(fetch=LAZY) byte[] fileData`.
**Used by**: `DriverDocumentRepository`, `DriverDocumentService`, `DriverManagementController`.

### `model/DriverPhone.java`
Entity for driver phone numbers (`driver_phone` table). Fields: `id`, `@ManyToOne driver`, `phoneType`
(enum: `MOBILE`, `HOME`, `WORK`), `phoneNumber`.
**Used by**: `DriverPhoneRepository`, `DriverService`, `DriverManagementController`.

### `model/DriverAttachment.java`
Entity for PDF file attachments belonging to a driver (`driver_attachment` table). Fields: `id`,
`@ManyToOne driver`, `fileName`, `uploadedAt`, `@Lob @Basic(fetch=LAZY) byte[] fileData`.
**Used by**: `DriverAttachmentRepository`, `DriverManagementController`.

### `model/TruckAttachment.java`
Entity for PDF file attachments belonging to a truck (`truck_attachment` table). Fields: `id`,
`@ManyToOne truck`, `fileName`, `uploadedAt`, `@Lob @Basic(fetch=LAZY) byte[] fileData`.
**Used by**: `TruckAttachmentRepository`, `TruckManagementController`.

### `model/TrailerAttachment.java`
Entity for PDF file attachments belonging to a trailer (`trailer_attachment` table). Fields: `id`,
`@ManyToOne trailer`, `fileName`, `uploadedAt`, `@Lob @Basic(fetch=LAZY) byte[] fileData`.
**Used by**: `TrailerAttachmentRepository`, `TrailerManagementController`.

### `model/TripAttachment.java`
Entity for PDF file attachments belonging to a trip (`trip_attachment` table). Fields: `id`,
`@ManyToOne trip`, `fileName`, `uploadedAt`, `@Lob @Basic(fetch=LAZY) byte[] fileData`.
**Used by**: `TripAttachmentRepository`, `TripManagementController`.

### `model/TrailerNote.java`
Entity for free-text notes on trailers (`trailer_note` table). Fields: `id`, `@ManyToOne trailer`,
`content`, `createdAt` (auto-set via `@PrePersist`).
**Used by**: `TrailerNoteRepository`, `TrailerService`, `TrailerManagementController`.

### `model/TripNote.java`
Entity for free-text notes on trips (`trip_note` table). Fields: `id`, `@ManyToOne trip`, `content`,
`createdAt` (auto-set via `@PrePersist`).
**Used by**: `TripNoteRepository`, `TripService`, `TripManagementController`.

---

## `repository/`

All repository interfaces extend `JpaRepository<Entity, Long>`, giving them `save`, `findById`,
`findAll`, `deleteById`, `count`, `existsById` for free.

### `repository/UserRepository.java`
Adds `findByUsername(String)` and `existsByUsername(String)`.
**Used by**: `UserService`, `AuthenticationService`, `DataInitializer`.

### `repository/TruckRepository.java`
Adds `findByRegistrationNumber`, `findByStatus`, `existsByRegistrationNumber`, and two `@Query`
methods — `findAllWithDetails()` and `findByIdWithDetails()` — that use `LEFT JOIN FETCH t.attachments`
to eagerly load the attachment collection.
**Used by**: `TruckService`.

### `repository/TrailerRepository.java`
Adds `findByRegistrationNumber`, `findByStatus`, `existsByRegistrationNumber`, and two `@Query`
methods — `findAllWithDetails()` and `findByIdWithDetails()` — that join-fetch both `notes` and
`attachments` collections.
**Used by**: `TrailerService`.

### `repository/DriverRepository.java`
Adds `findByStatus`, a default `findAvailable()` convenience method, and two `@Query` methods that
join-fetch `phones` and `attachments`.
**Used by**: `DriverService`.

### `repository/CustomerRepository.java`
No custom methods. Standard CRUD only.
**Used by**: `CustomerService`.

### `repository/DocumentRepository.java`
Adds `findByTruck(Truck)`, `findByExpiryDateBefore(LocalDate)`, and a `@Query`-backed
`findExpiringDocuments(LocalDate, LocalDate)` (documents expiring within a date range).
**Used by**: `DocumentService`.

### `repository/DriverDocumentRepository.java`
Adds `findByDriver(Driver)`, `findByExpiryDateBefore(LocalDate)`, and `findExpiringDocuments`
(same pattern as `DocumentRepository`).
**Used by**: `DriverDocumentService`.

### `repository/DriverPhoneRepository.java`
Adds `findByDriverId(Long)`.
**Used by**: `DriverService`.

### `repository/DriverAttachmentRepository.java`
Adds `findByDriverId`, `deleteByDriverId`, and `findFileDataById` (a `@Query` that selects only
the `fileData` byte array to avoid loading the full row).
**Used by**: `DriverManagementController` (indirectly via `DriverService`/controller logic).

### `repository/TruckAttachmentRepository.java`
Same pattern as `DriverAttachmentRepository` for trucks.
**Used by**: `TruckManagementController`.

### `repository/TrailerAttachmentRepository.java`
Same pattern as `DriverAttachmentRepository` for trailers.
**Used by**: `TrailerManagementController`.

### `repository/TripAttachmentRepository.java`
Same pattern as `DriverAttachmentRepository` for trips.
**Used by**: `TripManagementController`.

### `repository/TrailerNoteRepository.java`
Adds `findByTrailerIdOrderByCreatedAtDesc(Long)` to return notes newest-first.
**Used by**: `TrailerService`.

### `repository/TripNoteRepository.java`
Adds `findByTripIdOrderByCreatedAtDesc(Long)` to return notes newest-first.
**Used by**: `TripService`.

---

## `service/`

All service classes are `@Service` + `@Transactional`. They wrap repository calls with business
logic and are the only layer that performs persistence operations.

### `service/AuthenticationService.java`
Manages user login, logout, and the current-session user. `login()` verifies the password against
the BCrypt hash using `PasswordEncoder.matches()`; if it finds a legacy plain-text password it
accepts it once and upgrades to a hash. Stores the authenticated `User` in a private field
(`currentUser`) for the duration of the desktop session.
**Used by**: `LoginController` (to authenticate), `MainController` (to display username/role and
handle logout), and any controller that needs to check `isAdmin()`.

### `service/UserService.java`
CRUD for `User` entities. `addUser()` rejects duplicate usernames and always hashes the raw
password before saving. `updateUser()` re-hashes only if the incoming password is not already a
BCrypt hash (to avoid double-hashing). `deleteUser()` guards against deleting a non-existent user.
**Used by**: `UserManagementController`.

### `service/TruckService.java`
CRUD for `Truck` entities. `addTruck()` rejects duplicate registration numbers. `deleteTruck()`
guards against non-existent IDs.
**Used by**: `TruckManagementController`, `TripService` (updates truck status when trips start/end/cancel).

### `service/TrailerService.java`
CRUD for `Trailer` entities plus CRUD for `TrailerNote` sub-entities. `addTrailer()` rejects
duplicate registration numbers.
**Used by**: `TrailerManagementController`, `TripService` (updates trailer status on trip lifecycle events).

### `service/DriverService.java`
CRUD for `Driver` entities plus CRUD for `DriverPhone` sub-entities. `getAllDrivers()` uses the
`findAllWithDetails()` query (join-fetch phones and attachments).
**Used by**: `DriverManagementController`, `TripService` (updates driver status on trip lifecycle events).

### `service/TripService.java`
Core business logic for trip lifecycle. `createTrip()` marks the assigned driver, truck, and trailer
as `ON_TRIP`. `completeTrip()` records `actualArrival`, resets all three resources to `AVAILABLE`,
and updates the truck's `currentLocation` to the destination. `cancelTrip()` also frees all resources.
`deleteTrip()` frees resources first if the trip is still active. Also manages `TripNote` sub-entities.
**Used by**: `TripManagementController`.

### `service/CustomerService.java`
Simple CRUD for `Customer` entities.
**Used by**: `TripManagementController` (populates the customer combo box when creating/editing trips).

### `service/DocumentService.java`
CRUD for `Document` (truck documents) plus expiry helpers: `getExpiringDocuments()` (within 30 days)
and `getExpiredDocuments()` (already past due).
**Used by**: `DocumentManagementController`.

### `service/DriverDocumentService.java`
Same structure as `DocumentService` but for `DriverDocument` entities. Includes the same 30-day
expiry window and past-due helpers.
**Used by**: `DriverManagementController`.

---

## `controller/`

All controllers are Spring `@Component` beans. `FXMLLoader.setControllerFactory(springContext::getBean)`
allows Spring to inject services into them. Controllers that are tab/panel views build their UI
entirely in Java code (no FXML), returning a `Parent` node that `MainController` places into the
`contentArea` pane.

### `controller/LoginController.java`
Handles the login screen (defined in `login-view.fxml`). On `handleLogin()`: reads the username
and password fields, calls `AuthenticationService.login()`, and on success calls
`FleetManagementApplication.showMainScreen()`. Displays an error alert on failure.
**Used by**: `login-view.fxml` (fx:controller attribute).

### `controller/MainController.java`
The shell controller for `main-view.fxml`. On `initialize()`: sets the welcome label and role label,
hides `adminButton` for non-admin users, and loads the default truck management view. Sidebar button
handlers call the appropriate `show*()` helpers, each of which swaps `contentArea`'s child node.
`handleLogout()` calls `AuthenticationService.logout()` and returns to the login screen.
**Used by**: `main-view.fxml` (fx:controller attribute). References all module controllers to obtain
their view nodes.

### `controller/TruckManagementController.java`
Builds a view for managing tractor units: a searchable `TableView` listing all trucks, an "Add Truck"
form with fields for brand, registration number, country, year, and status, plus buttons to edit,
delete, and manage PDF attachments. Uses `FileChooser` for PDF upload; stores file bytes in the
database. Provides a "Download PDF" action that writes the `fileData` bytes to a temp file and opens
it with the OS default viewer.
**Used by**: `MainController.showTruckManagement()`.

### `controller/TrailerManagementController.java`
Same structure as `TruckManagementController` for semi-trailers. Also includes a notes panel that
displays and manages `TrailerNote` records (free-text with timestamp).
**Used by**: `MainController.showTrailerManagement()`.

### `controller/DriverManagementController.java`
Manages drivers. The table shows name, license, status. A detail/edit panel allows managing phone
numbers (add/delete), uploading PDF attachments (e.g. license scans), and viewing/adding driver
documents (`DriverDocument` with expiry dates). Displays expiry alerts for documents due within
30 days.
**Used by**: `MainController.showDriverManagement()`.

### `controller/TripManagementController.java`
The most complex controller. Manages trip lifecycle: create a trip (with combo boxes for truck,
optional trailer, optional driver, customer; date-time pickers for planned departure and arrival),
start a trip (`IN_PROGRESS`), complete, and cancel. The table shows origin, destination, status,
and assigned resources. A detail panel shows trip notes and PDF attachments. All lifecycle actions
propagate status changes to the assigned truck/trailer/driver via `TripService`.
**Used by**: `MainController.showTripManagement()`.

### `controller/DocumentManagementController.java`
Manages truck documents (insurance, technical inspection, tachograph calibration). Displays a
searchable table with expiry-date color coding (red for expired, orange for expiring within 30 days).
Allows adding a document linked to a truck with an optional PDF upload. Provides "Export PDF"
(download stored file) and "Delete" actions.
**Used by**: `MainController.showDocumentManagement()`.

### `controller/UserManagementController.java`
Admin-only panel. Lists all users in a `TableView` with columns for username, full name, and role.
Allows adding, editing (changing name, role, or password), and deleting users. Rejects attempts to
delete the currently logged-in admin. Visible only when `AuthenticationService.isAdmin()` returns
true; the sidebar button is hidden for logisticians.
**Used by**: `MainController.showUserManagement()`.

---

## `src/test/java/org/example/fleetmanagement/`

### `TestConfig.java`
Spring `@Configuration` used only in tests. Uses `@ComponentScan` with `includeFilters` to load
only `@Service` and `@Repository` beans — deliberately excluding `@Component` JavaFX controllers
that require a running JavaFX Toolkit. Declares a `PasswordEncoder` bean so security-aware services
can be instantiated.
**Used by**: `FleetManagementApplicationTests` via `@SpringBootTest(classes = TestConfig.class)`.

### `FleetManagementApplicationTests.java`
A single `contextLoads()` test that starts the Spring context with `TestConfig` and asserts that
all service and repository beans wire without errors.
**Used by**: Maven Surefire during `mvn test`.

### `service/AuthenticationServiceTest.java`
Unit tests for `AuthenticationService` using Mockito. Covers: successful login with hashed password,
wrong password, unknown user, legacy plain-text password upgrade to BCrypt hash on first login,
logout, and role checks. Uses a real `BCryptPasswordEncoder` (not mocked) so hashing behaves
identically to production.
**Used by**: Maven Surefire.

### `service/DriverServiceTest.java`
Unit tests for `DriverService`. Covers: `getAllDrivers`, `getDriverById`, `getAvailableDrivers`,
`getDriversByStatus`, `addDriver`, `deleteDriver` (existing and non-existing), `getPhonesByDriver`,
`addPhone`, `deletePhone`.
**Used by**: Maven Surefire.

### `service/TruckServiceTest.java`
Unit tests for `TruckService`. Covers: `getAllTrucks`, `getTruckById`, `addTruck` (unique and
duplicate registration number), `deleteTruck` (existing and non-existing), `getTrucksByStatus`.
**Used by**: Maven Surefire.

### `service/UserServiceTest.java`
Unit tests for `UserService` with focus on password hashing. Covers: `addUser` (hashes password,
stores as BCrypt), `addUser` with duplicate username (throws, no save), `updateUser` with raw
password (hashes it), `updateUser` with existing hash (does not re-hash), `deleteUser` (existing
and non-existing), `getAllUsers`, `getUserById`, `getUserByUsername`, `existsByUsername`.
**Used by**: Maven Surefire.

---

## `data/`

### `data/fleetdb.mv.db`
H2 persistent database file. Created when the application is run with H2 in file-based mode.
Currently the project runs MySQL in production, so this file may be a leftover from an earlier
development phase.
**Used by**: H2 engine when configured to use a file-based URL.

### `data/fleetdb.trace.db`
H2 trace/log file accompanying `fleetdb.mv.db`. Contains query and connection trace information
for debugging H2 sessions.
**Used by**: H2 engine automatically alongside `fleetdb.mv.db`.

---

## `dist/`

### `dist/FleetManagement-1.0.dmg`
The compiled macOS disk image containing the self-contained `FleetManagement.app` bundle with an
embedded JDK and all required JavaFX native libraries. Generated by `build-mac-app.sh`.
**Used by**: End users installing the application on macOS.

---

## `packaging/`

### `packaging/logo.icns`
Application icon in macOS ICNS format. Used by `jpackage` when creating the `.app` bundle and `.dmg`.
**Used by**: `build-mac-app.sh` (implicitly by `jpackage`).

### `packaging/logo.ico`
Application icon in Windows ICO format. Used when building a Windows installer.
**Used by**: `jpackage` for Windows packaging (if applicable).
