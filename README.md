# Screen Time Tracker (Android / Kotlin) — v2: User + Admin Roles

A native Android app with two clearly separated pages:

- **User Page** — enter a name, grant Usage Access, tap **Start Tracking**.
  Tracking runs in a `ForegroundService` with a persistent notification, so
  it survives the app being minimized, the screen locking, or the user
  switching to other apps.
- **Admin Page** — tap **Stop Session** to end the active session, auto-
  generate a CSV spreadsheet, and permanently log it to a local Room
  database. A `RecyclerView` shows every past session with a
  **Re-Share / Open Spreadsheet** button.

## How to open

1. Android Studio (Koala/Ladybug or newer).
2. `File > Open` → select the `ScreenTimeTracker` folder (the one containing
   `settings.gradle`).
3. Let Gradle sync (Gradle 8.4 / AGP 8.2.2 via the wrapper, JDK 17 — use the
   Embedded JDK if prompted).
4. Run on a device/emulator with API 24+. Android 13+ devices will also be
   asked for notification permission the first time you start tracking.

## Page / role architecture

| Page | Activity | Responsibility |
|---|---|---|
| Launcher | `ui/RoleSelectionActivity` | Lets the person pick User or Admin — keeps the two roles from sharing controls on one screen. |
| User | `ui/UserActivity` | Name entry, Usage Access + notification permission checks, starts the `ForegroundService`. |
| Admin | `ui/AdminActivity` | Live elapsed-time display, Stop Session, CSV export, Room logging, history `RecyclerView`. |

## Where each spec item lives

| Spec | File |
|---|---|
| `User Name` input | `res/layout/activity_user.xml`, `UserActivity.kt` |
| Usage Access permission check (`AppOpsManager`) | `util/UsageAccessHelper.kt` |
| Deep-link to `Settings.ACTION_USAGE_ACCESS_SETTINGS` | `UsageAccessHelper.openUsageAccessSettings()` |
| "Start Tracking" → `ForegroundService` + `startTime` | `UserActivity.startTrackingSession()`, `service/ScreenTimeForegroundService.kt`, `util/TrackingPrefs.kt` |
| Persistent notification | `ScreenTimeForegroundService.buildNotification()` |
| Background survival | `foregroundServiceType="dataSync"` in manifest + `START_STICKY` |
| "Stop Session" + `HH:mm:ss` | `AdminActivity.onStopSessionClicked()`, `util/TimeFormatter.kt` |
| CSV generation (`User Name,Screen Time`) | `util/CsvExporter.kt` |
| Room DB: `ID`, `User Name`, duration, `Timestamp`, `File Path` | `data/SessionEntity.kt`, `data/SessionDao.kt`, `data/AppDatabase.kt` |
| History `RecyclerView` + re-share | `ui/SessionHistoryAdapter.kt`, `res/layout/item_session_history.xml`, `AdminActivity.onReShareClicked()` |
| `FileProvider` secure sharing | `res/xml/file_paths.xml`, manifest `<provider>`, `CsvExporter.buildShareIntent()` |

## How the two roles talk to each other

Both pages read/write the same `TrackingPrefs` (a small SharedPreferences
wrapper) as the single source of truth for "is a session active right now,
for whom, since when":

- **User page** writes `isTracking = true`, `userName`, `startTime` when
  tracking starts.
- **Admin page** reads those same values to show the live elapsed timer and
  to compute the final duration on Stop — it does not need to have been the
  activity that started the session. If you open Admin directly from the
  launcher while a session is already running (started earlier from the
  User page, possibly after fully closing that screen), Admin will still
  correctly show it as active.
- The **ForegroundService** doesn't do any timing math itself; it only
  keeps the process alive and shows the notification, reading the user name
  from `TrackingPrefs` if it's restarted by the system (`START_STICKY`)
  after being killed under memory pressure — so a session is never silently
  lost.

## Fail-safe local persistence (Room)

Every completed session is written to `sessions` table via
`SessionDao.insert()` **before** the app offers to open/share the CSV. That
ordering matters: even if the user backs out of the share sheet, force-
closes the app, or the target app (Excel/Sheets) fails to open, the session
is already durably recorded with its `filePath`, so it can always be
re-shared later from the Admin page's history list. Room uses no destructive
migration fallback, so a future schema change won't silently wipe history.

CSV files themselves live under
`context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)` — app-scoped
external storage that isn't cleared by normal cache-clearing and persists
across app restarts (it's removed only if the app itself is uninstalled).
Each file gets a unique timestamped name, so sessions are never overwritten.

## Design decisions worth knowing about

- **CSV, not `.xlsx`.** CSV opens natively in Excel, Sheets, and Numbers
  with zero extra dependencies. Apache POI (mentioned in the original spec
  as an option) doesn't run cleanly on Android — it depends on `java.awt.*`,
  which isn't part of the Android runtime. If a binary `.xlsx` is a hard
  requirement, swap `CsvExporter` for a pure-Kotlin XLSX writer; the DB
  schema and history UI wouldn't need to change since both approaches
  return a `File` + content `Uri`.
- **`foregroundServiceType="dataSync"`.** Required on Android 14+
  (`targetSdk 34`) to declare what kind of foreground work the service is
  doing. `dataSync` was chosen as the closest fit for a background-tracking
  task that isn't camera/location/media.
- **Notification permission is best-effort.** On Android 13+, if the user
  denies `POST_NOTIFICATIONS`, tracking still starts (the foreground
  service runs fine) — it just won't show a visible notification, which
  matches how most real apps handle a denial gracefully rather than
  blocking the feature entirely.
- **Kapt over KSP for Room.** Used for build-config simplicity/version
  stability; swap to KSP later if you want faster incremental builds.

## Testing the flow end-to-end

1. Launch → **User**. Enter a name → **Start Tracking**. Grant Usage Access
   in Settings if prompted, then come back (the app resumes the flow
   automatically). Grant the notification permission if asked.
2. You should see a persistent "Tracking screen time" notification. Leave
   the app, use your phone for a bit, come back.
3. Launch → **Admin** (or tap the notification, or use "Go to Admin Page"
   from the User screen). You'll see the live elapsed timer. Tap
   **Stop Session**.
4. A CSV is generated, logged to history, and a share sheet opens. Cancel
   it if you like — the session still appears in the **Session History**
   list below with a **Re-Share / Open Spreadsheet** button you can use any
   time afterward.
