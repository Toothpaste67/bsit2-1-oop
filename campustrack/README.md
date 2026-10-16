# CampusTrack

Campus system for Liceo de Cagayan University (Main Campus):

- **QR Attendance** with a rotating class-session QR code
- **Lost & Found** with verified claims
- **Facility Reservation** with double-booking protection

**The system is database-free.** There is no MySQL and no JPA. Every record lives in
hand-written data structures and is saved to plain `.txt` files you can open in Notepad.

## Running it

CampusTrack is a desktop app: a window (JavaFX WebView) that shows the pages, with the
server running inside it.

- **In IntelliJ:** run `AppLauncher`.
- **By double-click:** build once (Maven panel → Lifecycle → `package`), then
  double-click `CampusTrack.bat`.
- **From a terminal with Maven:** `mvn spring-boot:run`

Closing the window stops the server. To run the server alone and use a normal browser,
run `CampusTrackApplication` instead.

Inside the app window:

- CSV exports open a *Save As* window.
- Dates and times are typed in (`YYYY-MM-DD`, `HH:MM` 24-hour); there is no picker.
- The camera scanner and *Full Screen* button need a real browser. Students scan from
  their phones; a teacher who wants the QR full screen opens http://localhost:8080 in
  Chrome or Edge while the app is running.

| Who | Address |
|---|---|
| This computer | http://localhost:8080 |
| Phones on the same Wi-Fi | `https://<this-computer's-IP>:8443` |

Phones only allow camera access on HTTPS pages. The certificate in `keystore.p12` is
self-signed, so the phone shows a warning the first time: choose *Advanced → Proceed*.
The plain HTTP port (8080) only answers on this computer.

### Default test accounts

Created on first run, when `data/users.txt` does not exist yet.

| Role | Email | Password |
|---|---|---|
| Admin | admin@liceo.edu.ph | `Admin123` |
| Faculty | hehe@liceo.edu.ph | `Faculty123` |
| Student | wanyu@liceo.edu.ph | `Student123` |
| Student | student@liceo.edu.ph | `Student123` |

These are for testing. Change them from **Admin → Users**, or set
`CAMPUSTRACK_ADMIN_PASSWORD`, `CAMPUSTRACK_FACULTY_PASSWORD` and
`CAMPUSTRACK_STUDENT_PASSWORD` before the first run. The student is already enrolled in
the demo class *IT 211*.

Other settings read from environment variables: `CAMPUSTRACK_DATA_DIR` (default `data`)
and `CAMPUSTRACK_KEYSTORE_PASSWORD`.

## Data structures used (`com.liceo.campustrack.datastructure`)

| Structure / algorithm | Where it is used | Cost |
|---|---|---|
| `HashTable` (separate chaining) | Primary storage of every repository (id → record); users by email and by ID number; one check-in per student per session; 10-second duplicate-scan debounce; claim-attempt limits | O(1) average |
| `ChainedHashSet` | Students enrolled in a class | O(1) average |
| `DynamicArray` | Query results passed to the pages | O(1) append and index |
| `SinglyLinkedList` | Audit log, newest entry at the head | O(1) insert |
| `LinkedQueue` | Replaying the phone's offline scan queue in order (FIFO) | O(1) enqueue / dequeue |
| `Sorting.mergeSort` | Every ordered list: classes by code, users and facilities by name, logs by time | O(n log n), stable |
| `Searching.firstIndexWhere` (binary search) | Reservation double-booking check on the sorted time slots | O(log n) |

`src/test` has unit tests for all of them: `mvn test`.

## Text file storage (`data/`)

One file per record type, one record per line, fields separated by `|`.
The first line (starting with `#`) names the columns.

```
# id|name|code|location|capacity|description|active
1|Audio Visual Room 1|AVR-1|3rd Floor, Rodelsa Hall|150|Multimedia projection|true
```

`users.txt`, `facilities.txt`, `classes.txt`, `sessions.txt`,
`attendance.txt`, `reservations.txt`, `lost-found.txt`, `audit-log.txt`, and the
`photos/` folder for lost & found pictures.

- Files are loaded into the hash tables at startup and rewritten on every change.
- **Edit them only while the app is stopped**; a running app overwrites manual edits.
- Passwords and lost & found answers are stored as BCrypt hashes, never as plain text.
- Inside a field, `|` is written as `\p` and a line break as `\n`.
- Delete the `data/` folder to start again with the default facilities and accounts.

## How attendance works

1. **Faculty** picks the class, then *Display Session QR*.
2. The QR code changes every 20 seconds. Each code is signed with the session's secret
   (HMAC-SHA256), so a shared screenshot stops working and codes cannot be forged.
3. **Students** (the only role that can open the scanner) scan it. The server checks:
   valid and current code → enrolled in the class → not already checked in → not the
   same code again within 10 seconds.
4. Checking in within 15 minutes of the start is **PRESENT**, later is **LATE**. When the
   teacher ends the session, everyone who did not check in becomes **ABSENT**.

Fallbacks: the short manual code under the QR, uploading a photo of the QR, the offline
queue (scans kept in the phone's local storage and synced later), and the teacher's
manual override.

## Roles

| Area | Admin | Faculty | Student |
|---|---|---|---|
| `/admin/**` users, facilities, master attendance logs, audit log | ✔ | | |
| `/faculty/**` classes, session QR, live feed, overrides, CSV export | | ✔ | |
| `/student/**`, `/attendance/**` dashboard, scanner, scan APIs | | | ✔ |
| Lost & Found, Reservations | ✔ | ✔ | ✔ |

Self-registration creates student accounts only. Faculty and admin accounts, and
password resets, are done by an admin.

## Project layout

```
src/main/java/com/liceo/campustrack/
├── config/          SecurityConfig, CustomSuccessHandler, DataSeeder
├── controller/      Admin, Attendance, Auth, CustomError, Facility, Faculty, LostFound, Student
├── datastructure/   HashTable, ChainedHashSet, DynamicArray, SinglyLinkedList, LinkedQueue, Sorting, Searching
├── dto/             ApiResponse, CameraScanRequest
├── model/           User, Facility, CourseClass, ClassSession, AttendanceRecord, Reservation, LostFoundItem, ...
├── repository/      TextFileRepository (hash table + .txt file) and one repository per model
├── security/        CustomUserDetails
└── service/         Attendance, Facility, LostFound, QrCode, User, Audit, NameLookup
src/main/resources/
├── application.properties, keystore.p12
├── static/          css/style.css, js/app.js, js/camera-scanner.js, js/offline-sync.js, images/logo.svg
└── templates/       login, register, attendance, reservations, lost-found, admin/, faculty/, student/, error/
```

## Known limits

- The camera scanner library (html5-qrcode) loads from the internet, so the phone needs a
  data connection the first time it opens the scanner. The manual code works without it.
- The desktop window (`AppLauncher`) cannot use the camera; use a browser for scanning.
- Text files suit a class project. Every save rewrites its whole file, which is fine for
  thousands of records but not for a real campus-wide deployment.
