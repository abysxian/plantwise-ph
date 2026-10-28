# PlantWise PH

**A backyard-farming guide for the Philippines.** PlantWise PH reads the live weather and climate for your town, ranks 36 Philippine vegetables by how well they suit your weather and soil, and then runs your garden with you: a grid of beds, dated care checklists, harvest history and a saved account for every gardener.

It is a desktop application written in plain Java (Swing for the screens, JDBC for the database, Maven for the build). It runs the same way in NetBeans, VS Code or the command line, and its layout adapts from a wide desktop window down to a narrow, phone-shaped one.

> **A note on the name.** This system was first called **AgriStep PH** and was renamed **PlantWise PH**. The name shown in the app is PlantWise PH, but some internal names in the code still say `agristep`: the Java package `com.agristep`, the settings file `agristep.properties`, the MySQL database `agristep_swing`, the H2 files in `./data/agristep`, and the remembered-sign-in file `~/.agristep/remembered-user.txt`. They were deliberately left unchanged so that existing saved data and setups keep working. They refer to the same system.

---

## Contents

1. [What the system is for](#1-what-the-system-is-for)
2. [Why it is beneficial](#2-why-it-is-beneficial)
3. [What makes it different from similar projects](#3-what-makes-it-different-from-similar-projects)
4. [Quick start](#4-quick-start)
5. [Running the system](#5-running-the-system)
6. [Using the system](#6-using-the-system)
7. [Database options (H2, MySQL and phpMyAdmin)](#7-database-options-h2-mysql-and-phpmyadmin)
8. [Where the data comes from](#8-where-the-data-comes-from)
9. [How the scoring works](#9-how-the-scoring-works)
10. [Technology and OOP design](#10-technology-and-oop-design)
11. [Project structure](#11-project-structure)
12. [Settings and customising](#12-settings-and-customising)
13. [Testing](#13-testing)
14. [Limitations](#14-limitations)
15. [Troubleshooting](#15-troubleshooting)
16. [Credits](#16-credits)


---

## 1. What the system is for

PlantWise PH helps a home or community gardener in the Philippines answer three everyday questions:

- **What should I plant here, now?** The Today screen compares the current weather and the past year's climate for the gardener's own location with the growing needs of 36 vegetables, and shows them in ranked order with a fit score and the reasons behind it.
- **What do I need to do in my garden today?** Every planted bed gets a dated care plan (sowing, watering, feeding, pruning, pest checks, harvest). The checklist adds weather hints, such as skipping watering when rain is forecast.
- **What have I grown, and how did it go?** The Garden screen shows each bed filling with green as its crop matures, and a harvest history keeps a record of finished crops.

Each person signs in to their own account, so beds, plants, soil, location and history are saved and are there again at the next sign-in.

## 2. Why it is beneficial

- **Better planting decisions.** Crops are matched to real local conditions instead of a generic calendar, which reduces planting vegetables that are likely to struggle in the current heat, rain or soil.
- **Less guesswork day to day.** Care tasks are generated with dates and amounts, and overdue work is flagged, so a beginner always knows the next step.
- **Saves water and effort.** Rain in the forecast turns into a plain hint to skip watering.
- **Built for Filipino gardens.** The catalogue uses local names (talong, ampalaya, kangkong, sitaw, pechay and more) with the English names beside them, and the location picker starts with 26 Philippine towns and cities.
- **Works for the whole household.** Separate accounts keep each person's garden apart, on one shared computer.
- **Honest about its data.** Each crop is labelled to show whether its growing ranges were checked against the FAO EcoCrop database or are typical garden ranges (see [section 8](#8-where-the-data-comes-from)).
- **Keeps working without internet.** If the connection drops, the app uses the climate data it saved earlier and says so on screen.
- **Free to run and easy to install.** The default database is a file inside the project folder, so nothing needs to be installed or switched on. MySQL (XAMPP) is available as an option.

## 3. What makes it different from similar projects

Typical garden apps and class projects fall into three groups: a fixed planting calendar, a weather app that knows nothing about crops, or a plain list or spreadsheet for tracking plants. PlantWise PH joins these into one workflow:

| Typical approach | PlantWise PH |
|---|---|
| A fixed "plant in month X" calendar that ignores the actual weather | A **live suitability score** for each crop, from the current temperature, the past year's rainfall and the gardener's soil, recalculated for the gardener's own coordinates |
| Weather and gardening in separate apps | **Weather drives the garden:** the Today banner follows the real conditions, and care tasks carry rain and heat hints |
| A generic checklist that is the same for every plant | A **dated care plan built per crop** (its own sowing, watering, feeding, pruning, pest and harvest schedule) that starts on the day it is planted |
| A fixed number of plots | A **resizable bed grid** (1 to 10 rows by 1 to 8 columns, for example 2×2 or 4×6) with a warning before any planted bed would be removed |
| One shared garden, or no saved state | **Accounts with private history:** secure sign-in, per-user beds, harvests, soil, location and grid size, and an optional "keep me signed in" |
| Data of unknown quality | **Source labels** on every crop, so the app never presents unchecked values as verified |
| Needs internet or fails without it | **Offline fallback** using cached climate data |
| Tied to one database | **Swappable database:** H2 out of the box, or MySQL/XAMPP with tables visible in phpMyAdmin, with older single-garden databases upgraded in place |
| Heavy frameworks | **Plain Java and a layered, object-oriented design** that is easy to read, test and extend |

## 4. Quick start

1. Install **JDK 17 or newer**.
2. Open the project folder in **NetBeans** or **VS Code** (see [section 5](#5-running-the-system)).
3. Run `com.agristep.Main`.
4. On the sign-in screen, press **Fill in the demo account** and then **Sign in** (username `demo`, password `demo1234`), or create your own account.

## 5. Running the system

**Requirements**

- JDK 17 or newer (the JDK bundled with NetBeans works).
- Maven (bundled with NetBeans; VS Code downloads it through its Java extension, or install it yourself for command-line use).
- Internet access the first time, so Maven can download the libraries. Live weather also needs internet.

### NetBeans

1. Choose **File > Open Project** and select the project folder (it shows a Maven icon).
2. Wait for the libraries to download. This happens only the first time.
3. Right-click the project, choose **Clean and Build**, then press the green **Run** button. If NetBeans asks for a main class, choose `com.agristep.Main`.

### VS Code

1. Install the **Extension Pack for Java** from Microsoft.
2. Choose **File > Open Folder** and select the project folder itself (the one that contains `pom.xml`).
3. Wait until the status bar shows that Java is ready.
4. Open `src/main/java/com/agristep/Main.java` and click **Run** above `main`, or press **F5** and pick the **PlantWise** configuration.

### Command line

```
mvn compile exec:java            # run directly
mvn package                      # build target/plantwise-ph.jar (a single file with everything inside)
java -jar target/plantwise-ph.jar    # run the built jar
mvn test                         # run the automated tests
```

`run.bat` (Windows) and `run.sh` (macOS and Linux) build the jar if it is missing and start it.

> The app saves its data relative to the folder it is started from. Starting it from NetBeans, VS Code, `run.bat` or `run.sh` all use the project folder, so the same data appears each time.

## 6. Using the system

### Signing in

- **Demo account:** username `demo`, password `demo1234`.
- **New account:** choose **Create account**, then enter a username (3 to 20 letters, numbers, dots, dashes or underscores), an optional display name, a password of at least 6 characters, and the same password again. After registering, the Location page opens so you can set where you live.
- **Keep me signed in on this computer:** the next start opens straight into your garden. Anyone using the same computer can open your garden until you press **Sign out**, so use it only on your own computer.
- Five wrong passwords lock sign-in for 30 seconds. Passwords are stored salted and hashed, never as plain text.
- **Sign out** is at the bottom of the side menu, or at the top right on a narrow window.

### Location: tell the app where you garden

Open **Location** and use any one of three ways:

1. **Type it:** enter a place name, latitude and longitude, then press **Save location** (a comma is accepted as the decimal mark; the app explains any invalid value).
2. **Search:** type a town or city, press **Search**, and click a result.
3. **Pick a common place:** choose one of 26 Philippine towns and cities. The list can be filtered and works without internet.

Saving a location returns you to Today with the weather and crop ranking updated for that place.

### Today: what should I do and plant?

- The **banner** greets you and gives a one-sentence summary of the weather, due tasks and beds ready to harvest. Its photo changes with the real weather: green hills for clear skies, misty mountains for clouds, and wet leaves with falling rain for rainy weather.
- The **weather card** shows the current temperature and a 7-day forecast with rain amounts.
- **Choose your soil** (soft garden soil, sandy soil or sticky clay). The ranking updates immediately.
- **What to plant** lists crops from best to worst fit for your location and soil. Each row shows a fit label (Great, Good, Possible or Poor), a score out of 100 and the main reasons. Press **Show all 36 crops** for the full list. Click a row for its detail page, or press **Plant** to put it in an empty bed.

### Garden: manage your beds

- Each **bed** is a tile. The green fill rises as the crop grows, and a yellow tile with "Ready!" means the crop can be harvested. Click a bed to see its details beside the grid (below it on a narrow window).
- **Empty bed:** choose a vegetable from the list and press **Plant here**. The care plan is created with dates counted from today.
- **Planted bed:** the checklist shows each task with its due date and instructions. Tick a task when it is done. Overdue tasks are marked in red, and watering tasks show rain hints. Buttons let you **Harvest** (saves a record to the harvest history and empties the bed), **Clear bed** (empties it without a record) or open the **Care guide** for the crop.
- **Edit grid:** press **Edit grid**, set the rows and columns with the + and − buttons or a preset (2×2, 3×3, 4×4, 4×6, 6×6), then press **Apply grid**. Growing the grid adds empty beds at the end. Shrinking it removes beds from the end, and if any of those beds have plants the app lists them and asks you to confirm with **Remove beds and resize**. The size is saved with your account.
- **Harvest history** at the bottom lists your most recent harvests.

### Crops: the vegetable catalogue

Browse all 36 vegetables and search by Filipino name, English name or scientific name. Each detail page shows the temperature and rainfall it needs, its soil preferences, the day-by-day care plan, care tips, pest tips and the source label for its data. Press **Plant** to put the crop in an empty bed.

### Calendar: what to plant each month

Pick a month to see all 36 crops ranked for it, using that month's average temperature at your location from the last year of weather (the same scoring as Today). Below that, choose one crop to see how all twelve months suit it. Philippine temperatures change little through the year, so the differences are largest for crops that are fussy about heat, and in the highlands. If monthly figures are not saved yet (first start while offline), the screen says so.

### Journal

Write dated notes about the whole garden or about one bed, and attach a photo (JPG, PNG or GIF; it is shrunk to 900 px and stored in `~/.agristep/journal`). Notes show who wrote them. Press **Write a note** on a planted bed in the Garden screen to start a note for that bed. Deleting asks twice.

### Insights: harvest statistics and export

Totals, the average days from planting to harvest, the most harvested crop, a chart of harvests per month for the last 12 months and a count per crop. Three buttons save your records: harvests as CSV, every care task as CSV (both open in Excel or Google Sheets) and a PDF garden report (beds, care plans, harvest history) that can be printed.

### Reminders and weather alerts

- When you open the app with tasks due today or overdue, a message appears in the window and, where the system supports it, as a desktop notification. The Today screen has a **Coming up** card with overdue tasks and tasks due in the next 3 days. There are no email or phone reminders.
- **Weather alerts** appear on Today when the 7-day forecast shows heavy rain (50 mm or more in a day), strong gusts (60 km/h or more), thunderstorms or extreme heat (36 °C or more), with garden advice such as harvesting early and staking tall crops. They are rules applied to forecast numbers and are **not official typhoon bulletins**; the app points to PAGASA for those.

### Companion planting and crop rotation

When you choose a crop for an empty bed, the app compares it with the beds touching it (including diagonally) and shows suggestions such as good neighbours (for example tomato with onion), pairs often kept apart (tomato with cabbage) and a rotation warning if that bed grew the same crop or plant family in the last 12 months. These are traditional gardening guidelines, not laboratory-tested rules, and are worded as suggestions.

### Pest guide

On the Crops screen press **Pest guide** to read the common pests and problems for every vegetable. It is written advice with no photos.

### Account: password, shared garden, language, deleting

- **Change password** asks for the current password first.
- **Shared garden:** invite another account by username. When they accept, they work in your garden (beds, plants, harvests, journal and location) until they leave; their own garden is kept untouched. A person working in someone else's garden cannot invite others, and you must remove your own members before joining another garden.
- **Language:** English or Filipino for the whole app: menus, screens, buttons, messages, weather alerts, planting hints, care plans, tips and pest notes. Crop names stay as printed (Filipino name with the English name beside it). The choice is saved in `~/.agristep/language.txt`. The CSV and PDF exports stay in English.
- **Delete account** asks for the password and a second press, then permanently removes the account, garden, history, journal and photos. People who had joined your garden return to their own.

### Window sizes

On a wide window the menu is a side rail. On a narrow window (phone-shaped) it becomes a bottom bar and the screens switch to a single column. Drag the window edge to see it change.

## 7. Database options (H2, MySQL and phpMyAdmin)

The database is chosen in `agristep.properties` in the project folder.

**H2 (default).** A file database stored in `./data/agristep` inside the project folder. Nothing to install and nothing to start.

**MySQL / XAMPP (and phpMyAdmin).**

1. Open the XAMPP Control Panel and start **MySQL**. Start **Apache** too if you want to use phpMyAdmin in the browser.
2. In `agristep.properties`, change `db.type=h2` to `db.type=mysql` and save.
3. Run the app. It creates a database named `agristep_swing` and all the tables by itself, and adds the 36 crops and the demo account.
4. Open `http://localhost/phpmyadmin` and select `agristep_swing` to see the tables.

| Table | Contents |
|---|---|
| `users` | Accounts, with hashed passwords |
| `plots` | Garden beds, one set per user |
| `care_tasks` | Dated care checklists for planted beds |
| `harvest_records` | Harvest history per user |
| `garden_settings` | Each user's location, soil and grid columns |
| `crops`, `crop_optimal_soils`, `crop_tolerable_soils`, `crop_tips`, `task_templates` | The vegetable catalogue and its care plans |
| `climate_profiles` | Climate data saved for each location, including month-by-month figures |
| `journal_entries` | Journal notes and the names of their photo files |
| `garden_members`, `garden_invites` | Shared-garden membership and pending invitations |

The default MySQL login is XAMPP's (user `root`, empty password). Change `mysql.user`, `mysql.password` or `mysql.url` in the same file if yours differ. H2 and MySQL keep separate data; switching `db.type` switches which database is used.

A database created by an older single-garden version is upgraded in place on first start. Its existing beds, history and location go to the first account that signs in, which is the `demo` account on a first start.

## 8. Where the data comes from

- **Weather and climate:** [Open-Meteo](https://open-meteo.com) (free, no key). Forecasts are cached for 15 minutes. The past year's climate for a location is saved in the database for 24 hours and reused if the internet drops.
- **Crop growing ranges:** the catalogue is `src/main/resources/data/crops.json`.
  - **Talong (eggplant)** is checked against the FAO EcoCrop datasheet (#1965) and is labelled **FAO verified**.
  - The other 35 crops use typical growing ranges and are labelled **Typical range**. Ampalaya has an EcoCrop entry (#7795) that is linked, but its ranges here have not been checked against it.
  - More crops can be verified at <https://gaez.fao.org/pages/ecocrop-search> and edited in `crops.json`. New or changed crops are picked up on the next start, matched by `code`.
- **Place coordinates:** typed by the user, found through Open-Meteo's place search, or taken from the built-in list of 26 Philippine towns and cities.
- **Pictures:** see [section 16](#16-credits).

## 9. How the scoring works

Each crop gets a score from 0 to 100 for the gardener's location:

- **50% temperature:** the recent average temperature compared with the crop's minimum, ideal and maximum range.
- **30% soil:** how well the chosen soil matches the crop's best and tolerable soils.
- **20% rainfall:** the past year's rainfall compared with the crop's range.

Two caps stop a good average hiding a deal-breaker: a temperature fit of zero limits the score to 15, and a soil fit of zero limits it to 45. Scores map to labels: **Great** (75 and above), **Good** (55 and above), **Possible** (30 and above) and **Poor** (below 30). If weather data cannot be fetched at all, crops are ranked by soil only and the app says so.

## 10. Technology and OOP design

**Technology:** Java 17, Swing, JDBC, Maven; H2 2.2.224 and MySQL Connector/J 8.4.0; Jackson 2.17.2 for JSON; the `java.net.http` client for Open-Meteo; JUnit 5 for tests.

| Concept | Where |
|---|---|
| Abstraction and inheritance | `CareTask` (abstract) with `SowingTask`, `WateringTask`, `FeedingTask`, `PruningTask`, `PestControlTask`, `HarvestTask` |
| Polymorphism | each task builds its own `getInstructions()` and `weatherHint(...)`; the screens never check which kind it is |
| Factory | `TaskKind` creates the right subclass; `TaskPlanner` turns a crop's plan into dated tasks |
| Encapsulation | private fields; changes go through methods such as `Plot.plant`, `CareTask.toggle` and `Plot.harvest` |
| Value object | `ClimateRange` (minimum, ideal, maximum with a `fit()` score) |
| Strategy | `SuitabilityScorer` interface with the `ClimateSuitabilityScorer` implementation |
| Interfaces and dependency inversion | `WeatherProvider` (real: `OpenMeteoClient`; tests: a fake), `Repository<T, ID>` and its JDBC implementations |
| Layers | `domain` (rules), `service` (use cases), `repository` and `persistence` (JDBC), `weather`, `ui` (Swing) |
| Security | `AuthService`, `PasswordHasher` (PBKDF2-SHA256), `Session`; every query is filtered by the signed-in user |
| Custom exceptions | `PlotOccupiedException`, `NotFoundException`, `WeatherUnavailableException`, `DataAccessException`, `ValidationException`, `AuthenticationException`, `GridResizeException` |
| Composition root | `AppContext` wires everything once |
| Responsive Swing | custom layouts (`StackLayout`, `FlowGridLayout`, `RowLayout`, `SplitLayout`); slow work runs in `SwingWorker` through `Tasks`, so the window never freezes |

## 11. Project structure

```
pom.xml                         Maven build (Java 17)
agristep.properties             database choice and MySQL login
run.bat / run.sh                build and start helpers
src/main/java/com/agristep/
    Main.java                   starts the app
    AppContext.java             creates and connects all the parts
    domain/                     crops, beds, care tasks, users, settings and their rules
    service/                    garden, grid, settings, accounts, recommendations, weather use cases
    repository/                 repository interfaces
    persistence/                JDBC implementations, schema upgrade
    weather/                    Open-Meteo client and weather types
    config/                     database settings, crop seeding, remembered sign-in
    ui/                         Swing screens, layouts, animation and photo banners
src/main/resources/
    schema.sql                  database tables
    data/crops.json             the 36-crop catalogue
    images/                     banner and sign-in photos
    brand/logo.png              the PlantWise PH logo
    emoji/                      Twemoji pictures
src/test/java/                  automated tests
```

## 12. Settings and customising

- **Database:** `agristep.properties` (see [section 7](#7-database-options-h2-mysql-and-phpmyadmin)).
- **Crops:** edit `src/main/resources/data/crops.json` and restart.
- **Photos:** the files in `src/main/resources/images/` are used as follows: Today follows the weather (`hills`, `mountains`, `wet-leaves`), Garden uses `monstera`, Crops uses `philodendron`, Location uses `mountains`, and the sign-in screen uses `hills` (`houseplants` while creating an account). Replace a file with another photo of the same name to change it.
- **Logo:** `src/main/resources/brand/logo.png`. It is shown on a white circle in the menu, the phone top bar and the sign-in screen, and is used as the window icon.
- **Motion:** buttons, cards and beds react to the pointer, screens fade in, progress bars fill and the photos drift slowly. To turn all motion off, start the app with `-Dplantwise.reduceMotion=true` or set the environment variable `PLANTWISE_REDUCE_MOTION=1`.
- **Remembered sign-in:** stored as the username only, in `~/.agristep/remembered-user.txt`. Press **Sign out** to forget it.

## 13. Testing

`mvn test` runs **187 automated tests**:

- the data rules for all 36 crops, scoring, care-plan planning, weather parsing and caching (with a fake weather service, so no internet is needed);
- every database operation on H2, including closing and reopening a database file and upgrading an old single-garden database;
- accounts: validation, password hashing, duplicate names, lockout, remembered sign-in, and one account never seeing another's garden;
- the garden grid: growing, shrinking with and without planted beds, limits and per-account sizes;
- the Swing screens, driven without a window: every screen at four window widths is checked for overflow and cut-off text, plus planting, ticking tasks, harvesting, searching, location editing, soil choice, grid editing, sign-in and navigation.

The database layer was also run against a real MariaDB server (the database behind XAMPP), including a fresh install, two accounts across a restart, the grid, and an in-place upgrade of an old database.

## 14. Limitations

- It is a **desktop Java application**. The layout adapts to a phone-sized window, but it is not an Android or iPhone app.
- **Live weather needs internet.** Without it the app uses saved climate data, and shows crop suggestions from soil only if no weather data exists at all.
- Only **one crop (talong)** is verified against FAO EcoCrop; the other 35 use typical ranges and are labelled that way.
- The H2 database belongs to one computer. Sharing one garden between computers needs the MySQL option on a shared server.
- "Keep me signed in" is convenient but open to anyone using the same computer until sign-out.

- Reminders are in the window and as desktop notifications only (no email or phone alerts), and weather alerts are rule-of-thumb warnings rather than official bulletins.
- Journal photos are stored on the computer running the app, so people sharing a garden from different computers need the shared MySQL database and will not see each other's photos.
- The Filipino setting covers everything on screen, but the CSV and PDF exports stay in English, and a note you type yourself is shown as you wrote it. The Filipino wording is written by the project, so a Filipino speaker should read it over before the presentation.

## 15. Troubleshooting

| Problem | What to do |
|---|---|
| The window shows "PlantWise couldn't start" and mentions MySQL | Start MySQL in the XAMPP Control Panel and press **Try again**, or set `db.type=h2` in `agristep.properties`. |
| It says the database is already in use | The app is open in another window. Close that window and press **Try again**. |
| `data/crops.json is missing from the application` | Do a **Clean and Build** (NetBeans) or `mvn clean package`, and make sure `src/main/resources/data/crops.json` is present. |
| NetBeans asks for a main class | Choose `com.agristep.Main`. |
| VS Code does not run the project | Open the folder that contains `pom.xml`, wait for Java to finish loading, then run the Command Palette action **Java: Clean Java Language Server Workspace**. |
| "Java version too old" | Install JDK 17 or newer and point NetBeans, VS Code or `JAVA_HOME` at it. |
| The weather card says the service cannot be reached | Check the internet connection. Crop suggestions still work from saved climate data. |
| Sign-in is locked | Wait 30 seconds and try again. |
| MySQL is on another port or has a password | Edit `mysql.url`, `mysql.user` and `mysql.password` in `agristep.properties`. |

## 16. Credits

- Weather and climate data: [Open-Meteo](https://open-meteo.com) (CC BY 4.0).
- Crop ranges: FAO EcoCrop where marked, otherwise typical garden guidance.
- Emoji pictures: [Twemoji](https://github.com/jdecked/twemoji) (CC BY 4.0), credited in the app footer.
- Banner and sign-in photographs: chosen by the project team and stored in `src/main/resources/images/`. Confirm the licence of each photograph before distributing the app publicly.
- Logo: the PlantWise PH logo, supplied by the project team.

