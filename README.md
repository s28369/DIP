# System zarządzania flotą transportową

Uproszczona aplikacja desktopowa do zarządzania flotą transportową — projekt studencki z działającym interfejsem graficznym (GUI). Umożliwia logowanie użytkowników, zarządzanie ciężarówkami, naczepami, kierowcami, rejsami oraz dokumentami, w tym dołączanie i pobieranie plików PDF.

---

## Spis treści

- [Funkcjonalności](#funkcjonalności)
- [Technologie](#technologie)
- [Wymagania](#wymagania)
- [Konfiguracja bazy danych](#konfiguracja-bazy-danych)
- [Uruchomienie](#uruchomienie)
- [Konta testowe](#konta-testowe)
- [Testy](#testy)
- [Budowanie i dystrybucja](#budowanie-i-dystrybucja)
- [Struktura projektu](#struktura-projektu)
- [Bezpieczeństwo haseł](#bezpieczeństwo-haseł)
- [Dodatkowa dokumentacja](#dodatkowa-dokumentacja)

---

## Funkcjonalności

### Uwierzytelnianie i role
- Logowanie bez rejestracji (dane testowe w bazie)
- Role: **Administrator** i **Logistyk**
- Panel administratora do dodawania, edycji i usuwania użytkowników

### Flota
- **Ciężarówki (ciągniki)** — dodawanie, edycja, usuwanie, status, lokalizacja i ładunek
- **Naczepy** — zarządzanie flotą naczep, notatki, załączniki PDF
- **Kierowcy** — dane kierowców, numery telefonów, dokumenty i załączniki PDF

### Rejsy (trasy)
- Tworzenie rejsów z przypisaniem ciężarówki, naczepy, kierowcy i klienta
- Statusy: zaplanowany, w trakcie, zakończony, anulowany
- Notatki i załączniki PDF do rejsów
- Automatyczna aktualizacja statusu pojazdów i kierowcy przy tworzeniu/zakończeniu rejsu

### Dokumenty
- Dokumenty przypisane do ciężarówek (typ, data ważności, opis)
- Dołączanie i pobieranie plików PDF
- Podgląd dokumentów wygasających w ciągu 30 dni

---

## Technologie

| Warstwa | Technologia |
|---------|-------------|
| Język | Java 21 |
| GUI | JavaFX 21.0.1 (FXML + kontrolery programistyczne) |
| Backend | Spring Boot 3.2.1 |
| ORM | Spring Data JPA / Hibernate 6 |
| Baza danych | MySQL (produkcja), H2 (testy) |
| Bezpieczeństwo haseł | Spring Security Crypto (BCrypt) |
| Build | Maven |
| Testy | JUnit 5, Mockito |

Architektura: **MVC** — modele (encje JPA), widoki (JavaFX/FXML), kontrolery (JavaFX + serwisy Spring).

Szczegółowy opis technologii: [`TECHNOLOGIES.md`](TECHNOLOGIES.md)

---

## Wymagania

- **JDK 21** (np. Amazon Corretto, Temurin)
- **Maven 3.8+**
- **MySQL 8.x** uruchomiony lokalnie (domyślnie port `3306`)

Na macOS z procesorem Apple Silicon projekt zawiera profil Maven `mac-aarch64` z natywnymi bibliotekami JavaFX.

---

## Konfiguracja bazy danych

1. Uruchom serwer MySQL.
2. Edytuj plik `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fleet_management?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=TWOJE_HASLO
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
```

Parametr `createDatabaseIfNotExist=true` tworzy bazę `fleet_management` automatycznie przy pierwszym uruchomieniu. Schemat tabel generuje Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

3. Przy pierwszym starcie, jeśli tabela użytkowników jest pusta, `DataInitializer` utworzy konta testowe (patrz [Konta testowe](#konta-testowe)).

> **Uwaga:** Nie commituj prawdziwego hasła do repozytorium. Użyj własnych danych dostępowych w pliku lokalnym.

---

## Uruchomienie

### Sposób 1 — Maven + JavaFX (zalecany do developmentu)

```bash
mvn clean javafx:run
```

### Sposób 2 — kompilacja i uruchomienie JAR-a

```bash
mvn clean package -DskipTests
java -jar target/fleet-management-1.0-SNAPSHOT.jar
```

Na macOS (Apple Silicon) możesz użyć skryptu pomocniczego:

```bash
./run.sh
```

Skrypt `run.sh` ustawia `--module-path` z natywnymi modułami JavaFX dla architektury ARM64.

### Sposób 3 — instalator macOS (.dmg)

```bash
./build-mac-app.sh
```

Skrypt buduje fat JAR, pakuje moduły JavaFX i tworzy plik `dist/FleetManagement-1.0.dmg` za pomocą `jpackage`.

---

## Konta testowe

| Użytkownik | Hasło | Rola |
|------------|-------|------|
| `admin` | `admin123` | Administrator |
| `logistyk` | `logistyk123` | Logistyk |

Hasła są przechowywane w bazie jako **hash BCrypt**, nie w postaci jawnej.

---

## Testy

Uruchomienie wszystkich testów:

```bash
mvn test
```

Testy obejmują:
- testy jednostkowe serwisów (Mockito): `TruckService`, `UserService`, `AuthenticationService`, `DriverService`
- test kontekstu Spring (`FleetManagementApplicationTests`) — weryfikuje poprawne podłączenie beanów

Testy serwisów nie wymagają JavaFX. Kontekst testowy (`TestConfig`) ładuje tylko warstwę `@Service` i `@Repository`, bez kontrolerów GUI.

---

## Budowanie i dystrybucja

| Polecenie | Opis |
|---------|------|
| `mvn clean compile` | Kompilacja projektu |
| `mvn clean package` | Budowa fat JAR-a |
| `mvn javafx:run` | Uruchomienie aplikacji z GUI |
| `mvn test` | Uruchomienie testów |
| `./build-mac-app.sh` | Pakiet `.dmg` dla macOS |

Pliki PDF dołączane w aplikacji (do ciężarówek, rejsów, kierowców itd.) są zapisywane w bazie danych jako dane binarne (`BLOB` / `@Lob byte[]`), a nie na dysku lokalnym.

---

## Struktura projektu

```
src/main/java/org/example/fleetmanagement/
├── FleetManagementApplication.java   # Punkt wejścia (JavaFX + Spring Boot)
├── config/                           # Konfiguracja JPA, BCrypt, inicjalizacja danych
├── controller/                       # Kontrolery JavaFX (widoki i logika UI)
├── model/                            # Encje JPA (User, Truck, Trip, Document, …)
├── repository/                       # Repozytoria Spring Data JPA
└── service/                          # Logika biznesowa

src/main/resources/
├── application.properties            # Konfiguracja MySQL i JPA
├── fxml/                             # Layouty ekranu logowania i głównego
└── images/                           # Ikona aplikacji

src/test/java/                        # Testy jednostkowe i konfiguracja testowa
```

Szczegółowy opis każdego pliku: [`FILE_OVERVIEW.md`](FILE_OVERVIEW.md)

---

## Bezpieczeństwo haseł

- Hasła użytkowników są hashowane algorytmem **BCrypt** przed zapisem do bazy (`UserService`, `DataInitializer`).
- Logowanie weryfikuje hasło przez `PasswordEncoder.matches()` (`AuthenticationService`).
- Przy edycji użytkownika hasło jest hashowane tylko wtedy, gdy administrator wpisze nowe — istniejący hash BCrypt nie jest hashowany ponownie.
- Starsze konta z hasłem w postaci jawnej są automatycznie uaktualniane do BCrypt przy pierwszym poprawnym logowaniu.

---

## Dodatkowa dokumentacja

| Plik | Zawartość |
|------|-----------|
| [`FILE_OVERVIEW.md`](FILE_OVERVIEW.md) | Opis wszystkich plików projektu |
| [`TECHNOLOGIES.md`](TECHNOLOGIES.md) | Stack technologiczny i rozwiązane problemy |

---

## Ograniczenia (wersja uproszczona)

- Brak integracji GPS
- Brak zaawansowanych algorytmów optymalizacji tras
- Brak aplikacji mobilnej
- Aplikacja desktopowa — jeden użytkownik na instancję (sesja w pamięci)

---

## Licencja

Projekt studencki — do użytku edukacyjnego.
