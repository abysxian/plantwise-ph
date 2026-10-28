# PlantWise PH (Java Swing)

A backyard-farming guide for the Philippines. It checks the live weather for your town, ranks **36 vegetables** by how well they suit your weather and soil, and keeps a garden tracker (beds, care checklists with weather hints, harvest history).

Plain Java, no Spring. Java Swing screens, JDBC database, Maven project. Runs the same in **NetBeans**, **VS Code**, or the command line.

## Run it

You need **JDK 17 or newer** (NetBeans' bundled JDK is fine).

### NetBeans
1. **File > Open Project** and choose this folder (NetBeans shows it with a Maven icon).
2. Wait for it to download the libraries (first time only, needs internet).
3. Press the green **Run** button. The window opens.

### VS Code
1. Install the **Extension Pack for Java** (VS Code suggests it when you open the folder).
2. **File > Open Folder** and choose this folder. Let the Java extension finish loading.
3. Open `src/main/java/com/agristep/Main.java` and click **Run** above `main`, or press **F5** (the "PlantWise" configuration is already set up).

### Command line
```
mvn compile exec:java        # run it
mvn package                  # builds target/agristep.jar (one file, includes everything)
java -jar target/agristep.jar
mvn test                     # runs the tests
```
`run.bat` (Windows) and `run.sh` (Mac/Linux) build the jar if needed and start it.

## Look and feel

- **Logo:** none yet. When you have one, save it as `src/main/resources/brand/logo.png` (a square PNG with a transparent background works best) and it appears in the side menu, the phone top bar, the sign-in screen and the window icon. Until then the app shows just its name.
- **Photos:** each screen has a photo banner, and the sign-in screen has a full-window photo. They live in `src/main/resources/images/` and are chosen like this: Today follows the real weather (clear = green hills, cloudy = misty mountains, rain = wet leaves), Garden = monstera, Crops = philodendron, Location = mountains, sign-in = hills (houseplants when creating an account). Replace a file with another photo of the same name to change it. Check the licence of any photo before publishing the app.
- **Movement:** buttons, cards, beds and menu items react to the pointer; screens fade in; progress bars fill; the photos drift slowly. To turn motion off, start with `-Dplantwise.reduceMotion=true` or set the environment variable `PLANTWISE_REDUCE_MOTION=1`.

## Accounts, login and saved history

When the app opens you sign in or create an account.

- **Login to try right away:** username `demo`, password `demo1234` (the "Fill in the demo account" button types it for you).
- **Create account:** pick a username (3 to 20 letters, numbers, dots, dashes or underscores), an optional display name, and a password of 6+ characters. You land on the Location page to set where you live.
- Every account has its **own beds, plants, care ticks, harvest history, soil and location**. Everything is saved to the database the moment you change it, so when you open the app again and sign in, it is all there as you left it.
- Tick **Keep me signed in on this computer** and the next start opens straight into your garden. This remembers only your username, in `~/.agristep/remembered-user.txt`; anyone using this computer can open your garden until you press **Sign out**.
- Passwords are never stored as text: each is salted and hashed (PBKDF2-SHA256). Five wrong passwords lock sign-in for 30 seconds.
- If you used the earlier version, its existing garden is kept and given to the first account that signs in (that is the `demo` account on first start).
- The H2 database path (`./data/agristep`) is relative to the folder the app is started from. NetBeans, VS Code and `run.bat`/`run.sh` all start it from the project folder, so you keep seeing the same data. The sign-in screen says where yours is saved.

## Database

By default it uses **H2**, a file database inside this folder (`data/`). Nothing to install, nothing to turn on.

To use **MySQL / XAMPP** instead (so you can see the tables in phpMyAdmin):
1. Open the XAMPP Control Panel and **Start** MySQL.
2. Open `agristep.properties` (in the project folder) and change `db.type=h2` to `db.type=mysql`.
3. Run the app. It creates a database named `agristep_swing` and all tables by itself.

The default MySQL login is XAMPP's (user `root`, empty password). Change `mysql.user` / `mysql.password` / `mysql.url` in the same file if yours differ. If MySQL isn't running, the app shows a message saying what to do and a **Try again** button.

It uses its own database name on purpose, so it will not clash with tables left over from the earlier Spring Boot version.

## What's in it

| Screen | What it does |
|---|---|
| **Today** | One-sentence advice from the weather and your tasks, current weather and 7-day forecast, soil picker (3 soil types), crops ranked for your place with a fit score |
| **Garden** | A grid of beds that fill green as the crop grows. **Edit grid** sets rows and columns (1-10 rows, 1-8 columns, e.g. 2×2 or 4×6) with + / - buttons or quick presets; the size is saved per account. Plant a bed, tick its care checklist (with rain/heat hints), harvest, clear, harvest history. Making the grid smaller removes beds from the end and asks first if any have plants |
| **Crops** | All 36 vegetables with search; each has a detail page: needs, care plan, care tips, pest tips, source |
| **Location** | Type your own place name and coordinates and save, search any town, or pick from 26 common places with a filter (works offline). Each account has its own |
| **Sign in** | Sign in or create an account; Sign out is in the side menu (top bar on phones) |

The window adapts to its size: a side menu on wide windows, a bottom menu and single-column layout on narrow ones (resize it small to see the phone layout).

## Where the data comes from (please read before presenting)

- **Weather and climate:** [Open-Meteo](https://open-meteo.com) (free, no key). Forecasts are cached 15 minutes. Yearly climate is saved in the database for 24 hours and reused if the internet drops.
- **Crop ranges:** only **Talong (eggplant)** is checked against the FAO EcoCrop datasheet (#1965) and marked *FAO verified*. The other 35 crops use *typical growing ranges* and are labelled **"Typical range"** in the app, with a note that they are not yet checked against EcoCrop. Ampalaya has an EcoCrop entry (#7795) that is linked, but its ranges here were not checked against it. If your teacher asks, you can verify more crops at <https://gaez.fao.org/pages/ecocrop-search> and edit `src/main/resources/data/crops.json`.
- **Emoji pictures:** [Twemoji](https://github.com/jdecked/twemoji), CC-BY 4.0 (credited in the app footer).

New crops added to `crops.json` appear automatically on the next start, even in an existing database (matched by `code`).

## OOP 

| Concept | Where |
|---|---|
| Abstraction and inheritance | `CareTask` (abstract) with `SowingTask`, `WateringTask`, `FeedingTask`, `PruningTask`, `PestControlTask`, `HarvestTask` |
| Polymorphism | each task builds its own `getInstructions()` and `weatherHint(...)`; the screens never check which kind it is |
| Factory | `TaskKind` enum creates the right subclass; `TaskPlanner` turns a crop's plan into dated tasks |
| Encapsulation | private fields; changes go through methods (`Plot.plant`, `CareTask.toggle`, `Plot.harvest`) |
| Value object | `ClimateRange` (min/ideal/max with a `fit()` score) |
| Strategy | `SuitabilityScorer` interface, `ClimateSuitabilityScorer` implementation |
| Interfaces / dependency inversion | `WeatherProvider` (real: `OpenMeteoClient`, tests: fake), `Repository<T,ID>` and its JDBC implementations |
| Layers | `domain` (rules) / `service` (use cases) / `repository` + `persistence` (JDBC) / `weather` / `ui` (Swing) |
| Custom exceptions | `PlotOccupiedException`, `NotFoundException`, `WeatherUnavailableException`, `DataAccessException` |
| Composition root | `AppContext` wires everything once |
| Swing | `ui/`: custom layouts (`StackLayout`, `FlowGridLayout`, `RowLayout`, `SplitLayout`) make it responsive; slow work runs in `SwingWorker` (`Tasks`) so the window never freezes |

## How it was checked

- **139 automated tests** (`mvn test`): the crop data rules for all 36 crops, scoring, planning, weather parsing and caching (with fakes, no internet), every database operation on H2 including closing and reopening a database file, accounts (validation, hashing, lockout, one account never seeing another's garden), upgrading an old single-garden database, and the Swing screens driven without a window (every screen at 4 window widths checked for overflow or cut-off text; plant, tick, harvest, search, location, soil pick, navigation).
- Screens were also rendered to images and looked at, at desktop and phone widths, and the real `Main` was started on a virtual display.
- The same SQL was run against a real **MariaDB** server (XAMPP's database): schema creation, seeding, a full garden flow, two accounts kept separate across a restart, and upgrading an old single-garden database in place.

Not verified in the build environment: `mvn package` and the Maven plugins themselves (the sandbox could not reach Maven Central), live calls to Open-Meteo (the sandbox could not reach it, so weather was tested with fakes and the offline fallback), and the MySQL Connector/J driver specifically (MariaDB's driver was used there). Please run it once on your own PC before your presentation.
