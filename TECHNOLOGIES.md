# Technologies Used — Fleet Management System

## Project Overview

A desktop application for managing a transport fleet (trucks, trailers, drivers, trips, documents).
Built as a JavaFX GUI application powered by a Spring Boot backend, with a MySQL database for production
and H2 in-memory database for automated tests.

---

## Technology Stack

### 1. Java 21
- **What it is**: The programming language and runtime for the entire application.
- **Where used**: All source files under `src/main/java/` and `src/test/java/`.
- **How used**: Target language level is set to 21 in `pom.xml` (`<java.version>21</java.version>`).
  Modern features such as records, pattern matching, and sealed classes are available, though the project
  primarily uses standard OOP patterns.

---

### 2. JavaFX 21.0.1
- **What it is**: A desktop UI toolkit for Java, providing controls (buttons, tables, forms), layout
  containers, and an FXML markup language for declarative view definitions.
- **Where used**:
  - All `controller/` classes import `javafx.*` packages and build or bind UI components programmatically.
  - `src/main/resources/fxml/login-view.fxml` — the login screen layout.
  - `src/main/resources/fxml/main-view.fxml` — the main shell layout with a sidebar navigation menu
    and a `StackPane` content area.
  - `FleetManagementApplication.java` — extends `javafx.application.Application`, calls `launch()`,
    and loads FXML views via `FXMLLoader`.
- **How used**:
  - JavaFX is the presentation layer. Controllers build their views programmatically (via `VBox`, `TableView`,
    dialogs, `FileChooser`, etc.) inside their constructors and `initialize` methods.
  - `FXMLLoader.setControllerFactory(springContext::getBean)` wires JavaFX controllers as Spring beans,
    allowing `@Autowired` injection into them.
  - Declared in `pom.xml` as `javafx-controls` and `javafx-fxml` artifacts.
  - On Apple Silicon (mac-aarch64) a separate Maven profile adds native-classifier JARs
    (`javafx-*-21.0.1-mac-aarch64.jar`) so the GPU pipeline works on ARM Macs.

---

### 3. Spring Boot 3.2.1
- **What it is**: An opinionated framework that auto-configures Spring components (data source,
  JPA, transaction management, etc.) with minimal XML configuration.
- **Where used**:
  - `FleetManagementApplication` is annotated `@SpringBootApplication`.
  - The Spring context is started manually inside `Application.start()` via `SpringApplicationBuilder`
    with `.headless(false)` (required because JavaFX is the display layer, not a servlet container).
  - `application.properties` configures the datasource, Hibernate dialect, and HikariCP pool.
- **How used**: Spring Boot handles component scanning, bean creation, transaction management, and
  datasource auto-configuration. All `@Service`, `@Repository`, and `@Component` classes are
  discovered automatically.

---

### 4. Spring Data JPA (with Hibernate 6)
- **What it is**: A Spring module that eliminates boilerplate DAO code by generating repository
  implementations at runtime from interface declarations.
- **Where used**: Every `repository/` interface extends `JpaRepository<Entity, Long>`.
- **How used**:
  - Standard CRUD is inherited from `JpaRepository`.
  - Custom finders use Spring's derived-query naming convention (e.g. `findByStatus`, `findByTruckId`).
  - Complex queries with eager-loading of collections use `@Query("SELECT DISTINCT ... LEFT JOIN FETCH ...")`
    to prevent `LazyInitializationException` when accessing entity collections from JavaFX controllers
    that run outside a JPA session.
  - `JpaConfig.java` explicitly declares `@EnableJpaRepositories` and `@EntityScan` packages.

---

### 5. Hibernate (ORM + Bytecode Enhancement)
- **What it is**: The JPA implementation used by Spring Data JPA. Also provides bytecode enhancement
  for field-level lazy loading.
- **Where used**:
  - All `model/` entity classes (`@Entity`, `@Table`, `@Column`, `@Lob`, `@ManyToOne`, etc.).
  - `hibernate-enhance-maven-plugin` runs at compile time.
- **How used**:
  - Entities are standard JPA classes. Hibernate generates the DDL automatically (`ddl-auto=update`).
  - The bytecode enhancement plugin (`enableLazyInitialization=true`) makes `@Basic(fetch=LAZY)`
    actually work on `@Lob byte[]` fields (attachment file data). Without this plugin, Hibernate
    ignores `LAZY` on `@Basic` fields and always loads the blob eagerly, wasting memory on every
    list query.

---

### 6. Spring Security Crypto
- **What it is**: A standalone module from Spring Security that provides `BCryptPasswordEncoder`
  without requiring the full Spring Security web/servlet stack.
- **Where used**:
  - `SecurityConfig.java` defines it as a `@Bean`.
  - `AuthenticationService` uses it to verify passwords on login.
  - `UserService` uses it to hash passwords before saving a user.
  - `DataInitializer` uses it when seeding the initial test users.
- **How used**: All passwords are stored as BCrypt hashes (`$2a$...`). The `spring-security-crypto`
  dependency is declared separately (not `spring-boot-starter-security`) to avoid activating the
  HTTP security filter chain, which is irrelevant for a desktop app.

---

### 7. MySQL
- **What it is**: The relational database used in production.
- **Where used**: `application.properties` configures `jdbc:mysql://localhost:3306/fleet_management`.
- **How used**: The schema is created/updated automatically by Hibernate (`ddl-auto=update`).
  The `mysql-connector-j` JDBC driver is on the runtime classpath. `createDatabaseIfNotExist=true`
  in the JDBC URL lets the app create the database automatically on first run.

---

### 8. H2 Database
- **What it is**: An in-memory SQL database used exclusively in tests.
- **Where used**: `src/test/` — Spring Boot's test auto-configuration picks H2 automatically
  when MySQL is not available and H2 is on the classpath.
- **How used**: Declared with `<scope>runtime</scope>` in `pom.xml`. The test Spring context
  (`TestConfig.java`) does not configure a datasource, so Spring Boot auto-configures an embedded H2
  instance for the JPA layer.

---

### 9. HikariCP
- **What it is**: A high-performance JDBC connection pool, bundled with Spring Boot.
- **Where used**: Configured in `application.properties`.
- **How used**:
  - `minimum-idle=5`, `maximum-pool-size=10`, `connection-timeout=10000` ensure stable connection
    reuse for the desktop session without exhausting the MySQL server.

---

### 10. Maven (Build System)
- **What it is**: The project build and dependency management tool.
- **Where used**: `pom.xml` at the project root.
- **How used**:
  - `spring-boot-maven-plugin` packages the application as an executable fat JAR.
  - `javafx-maven-plugin` (v0.0.8) is configured with `--module-path` for running the app with
    `mvn javafx:run`.
  - `hibernate-enhance-maven-plugin` runs bytecode enhancement during the `compile` phase.
  - Maven profiles (`mac-aarch64`) add platform-specific JavaFX native classifiers automatically
    based on OS detection.

---

### 11. JUnit 5 + Mockito
- **What it is**: The testing framework (JUnit 5) and mocking library (Mockito).
- **Where used**: All files under `src/test/java/`.
- **How used**:
  - `@ExtendWith(MockitoExtension.class)` drives test lifecycle with Mockito injection.
  - `@Mock` creates mock repositories; `@InjectMocks` wires the service under test.
  - `ArgumentCaptor` verifies that passwords are hashed before being persisted.
  - `@SpringBootTest(classes = TestConfig.class)` runs the full Spring context (excluding JavaFX
    controllers) to verify that all beans wire correctly.

---

### 12. jpackage (macOS Packaging)
- **What it is**: A JDK tool that creates native platform installers (`.dmg`, `.exe`, etc.).
- **Where used**: `build-mac-app.sh`.
- **How used**: The script builds the fat JAR, collects the arm64 JavaFX module JARs into a
  `target/javafx-mods/` directory, and invokes `jpackage --type dmg` to produce
  `dist/FleetManagement-1.0.dmg`. The resulting DMG contains a self-contained macOS `.app` bundle
  with all required modules.

---

## Troubles Encountered and Solutions

### 1. JavaFX + Spring Boot Integration
**Problem**: Spring Boot's standard `SpringApplication.run(...)` starts a headless process with no
GUI. JavaFX requires control of the main thread via `Application.launch()`, which conflicts with
the normal Spring Boot startup flow.

**Solution**: `FleetManagementApplication` extends `javafx.application.Application`. The Spring
context is started inside the `start(Stage)` override using `SpringApplicationBuilder` with
`.headless(false)`. This delays Spring context creation until JavaFX has already initialized its
toolkit. Beans are wired into FXML controllers via `loader.setControllerFactory(springContext::getBean)`.

---

### 2. JavaFX on Apple Silicon (mac-aarch64)
**Problem**: The standard Maven `javafx-controls` dependency without a classifier downloads the
platform-independent JAR that lacks the native graphics pipeline libraries. On Apple Silicon Macs
this causes a `UnsatisfiedLinkError` or blank window.

**Solution**: A Maven profile activated automatically when `os.arch=aarch64` and `os.family=mac`
adds the `mac-aarch64` classifier to all four JavaFX artifacts (`javafx-base`, `javafx-graphics`,
`javafx-controls`, `javafx-fxml`). The `run.sh` script also manually constructs the `--module-path`
pointing to the arm64 JARs in the local Maven repository.

---

### 3. `data.sql` Incompatible with MySQL (`spring.sql.init.mode=never`)
**Problem**: The `data.sql` seed file uses H2-specific `MERGE INTO ... KEY(id)` syntax, which is
not valid SQL in MySQL.

**Solution**: `application.properties` sets `spring.sql.init.mode=never` to disable automatic
execution of `data.sql` entirely. Instead, `DataInitializer.java` (a `CommandLineRunner`) seeds
default users programmatically at startup, only when the user table is empty. This approach is
database-agnostic and works for MySQL, H2, and PostgreSQL alike.

---

### 4. `LazyInitializationException` in JavaFX Controllers
**Problem**: JPA entity collections (e.g. `driver.getPhones()`, `trailer.getNotes()`,
`truck.getAttachments()`) are lazy by default. When a JavaFX controller accessed these collections
outside of an active JPA session (which closes immediately after the service method returns), Hibernate
threw `LazyInitializationException: failed to lazily initialize a collection`.

**Solution**: Custom `@Query` methods using `LEFT JOIN FETCH` were added to repositories that need
to display nested collections. For example, `DriverRepository.findAllWithDetails()` uses
`SELECT DISTINCT d FROM Driver d LEFT JOIN FETCH d.phones LEFT JOIN FETCH d.attachments`. This
eagerly loads all required associations within the single transactional service call.

---

### 5. Blob Fields Loading Entirely on Every List Query
**Problem**: `@Lob byte[]` attachment fields (PDF file data stored in the database) were loaded
eagerly on every list/table-refresh query even when only metadata (filename, size) was needed.
This caused very high memory usage and slow queries when many files were stored.

**Solution**: `@Basic(fetch = FetchType.LAZY)` was added to `fileData` fields in all attachment
entities (`TruckAttachment`, `TrailerAttachment`, `TripAttachment`, `DriverAttachment`). However,
Hibernate ignores `@Basic(fetch=LAZY)` on fields without bytecode instrumentation. The
`hibernate-enhance-maven-plugin` was added to `pom.xml` with `enableLazyInitialization=true`
to instrument entity classes at compile time, making field-level lazy loading actually work.
Additionally, attachment repositories expose a dedicated `findFileDataById(@Param("id") Long id)`
query that fetches only the raw bytes when explicitly needed (e.g. when the user clicks "Download PDF").

---

### 6. Double Password Hashing
**Problem**: When editing an existing user in the admin panel, the `User` object passed to
`UserService.updateUser()` already contained the BCrypt-hashed password loaded from the database.
Hashing it again produced a second hash, making the password permanently unusable.

**Solution**: Both `UserService.updateUser()` and `AuthenticationService` check whether the
incoming password string already starts with `$2a$`, `$2b$`, or `$2y$` (the BCrypt identifier
prefixes). If it does, the value is left unchanged; if it does not (i.e. the admin typed a new
raw password), it is hashed before saving.

---

### 7. Legacy Plain-text Passwords in the Database
**Problem**: Early versions of the application stored passwords as plain text (visible in `data.sql`).
When security was added, existing users with plain-text passwords could no longer log in because
`BCryptPasswordEncoder.matches()` always returned `false` for non-hashed strings.

**Solution**: `AuthenticationService.login()` first checks if the stored value is a BCrypt hash.
If not, it falls back to a direct string comparison (accepting the plain-text password once), then
immediately re-saves the user with the password replaced by a proper BCrypt hash. After the first
login the account is silently upgraded and the legacy fallback is never triggered again.

---

### 8. Tests Failing Due to JavaFX Toolkit Not Initialized
**Problem**: `@SpringBootTest` loaded the full Spring context, which triggered the creation of JavaFX
controller beans annotated with `@Component`. JavaFX controllers call `FXCollections`, `TableView`,
and other JavaFX classes in their constructors, which fail with
`java.lang.IllegalStateException: Toolkit not initialized` when no JavaFX display is running.

**Solution**: `TestConfig.java` is a `@Configuration` class that replaces the main application class
for tests. It uses `@ComponentScan` with explicit `includeFilters` to scan only `@Service` and
`@Repository` beans — intentionally excluding the `@Component`-annotated JavaFX controllers.
Unit tests for services use `@ExtendWith(MockitoExtension.class)` with no Spring context at all.

---

### 9. Console Window on Windows
**Problem**: When packaging the application for Windows, an unwanted black console window appeared
behind the main application window.

**Solution**: The `spring-boot-maven-plugin` or packaging configuration was adjusted to prevent
launching a console window. This was tracked in a commit ("no console on win"). For the macOS DMG,
`jpackage` was used instead, which produces a proper native `.app` bundle without a console.

---

### 10. No macOS App Bundle for Distribution
**Problem**: Distributing a plain `.jar` file required end users to have the correct JDK and JavaFX
SDK installed and correctly configured, which is impractical for non-technical users.

**Solution**: `build-mac-app.sh` automates the full packaging pipeline: Maven compiles the fat JAR,
the script collects the arm64 JavaFX native module JARs, and `jpackage` bundles everything into a
self-contained `dist/FleetManagement-1.0.dmg`. The resulting installer does not require a separate
JDK on the user's machine.
