# Radijas TV — implementacijos planas

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android TV programėlė, kuri įsijungia ir iškart groja radiją, valdoma keturiais pulto mygtukais, be meniu ir nustatymų.

**Architecture:** Forkinam Fogwave ir pasiliekam jo Media3/ExoPlayer garso sluoksnį bei stočių saugyklą. Visą telefono UI ištrinam ir vietoj jo statom vieną Compose-for-TV ekraną: nefokusuojamas „dabar groja“ blokas viršuje ir vienintelė fokusuojama zona — horizontali stočių lentyna apačioje.

**Tech Stack:** Kotlin 2.4.20, Jetpack Compose (BOM 2026.09.00), `androidx.tv:tv-material:1.1.0`, Media3 1.11.0, AGP 9.2.0, Gradle wrapper iš upstream.

**Spec:** `docs/superpowers/specs/2026-10-28-tv-radio-design.md`

## Global Constraints

- `namespace = "lt.radijas.tv"`, `applicationId = "lt.radijas.tv"`, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 37`.
- Kompiliuojama su JDK 17 ar naujesniu. `JAVA_HOME` šioje mašinoje: `/home/paulius/.local/share/mise/installs/java/temurin-21.0.13+9.0.LTS`. SDK: `ANDROID_HOME=/home/paulius/.local/share/android-sdk`.
- `androidx.tv:tv-material:1.1.0` — vienintelė nauja UI priklausomybė. Compose BOM lieka upstreaminis.
- Leidimai tik: `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- Jokių Google Play Services, Firebase, Coil, analitikos, crash reporterių, reklamų.
- Licencija `GPL-3.0-only`. `LICENSE` failas nekeičiamas; `README.md` perrašomas nurodant, kad tai Fogwave forkas.
- Visi naudotojui matomi tekstai — lietuviški, `res/values/strings.xml` (be `values-lt`).
- Fokusuojama tik stočių lentyna. ↑ ir ↓ nieko nedaro.
- Back ir Home sustabdo garsą. Screensaver ir TV standby — grojimas tęsiasi.
- Jokių play/pause valdymų jokiame paviršiuje.
- Appsas skirtas tik TV: `LEANBACK_LAUNCHER` be `LAUNCHER`.

## Review Focus

Šie atvejai specifikacijoje numatyti, bet joks „laimingas kelias“ jų nepadengia. Kiekvienas turi savo testą tame taške, kuris at odgovoran už kodą (nurodyta skliaustuose).

1. **Srautas miręs / nėra interneto.** Naudotojas turi matyti tą pačią stotį su „Nepavyksta prisijungti“, appsas neturi kristi ir turi toliau bandyti. (Task 3, Task 5)
2. **Stotis nesiunčia ICY meta arba siunčia labai ilgą/reklaminį tekstą.** Eilutė lieka rezervuota, išdėstymas nešokinėja, ilgas tekstas nukerpamas. (Task 6)
3. **Pulto ← laikomas prie pirmos stoties.** Fokusas prispaudžiamas, sąrašas nesisuka ratu, nieko nekartojasi. (Task 4)
4. **Screensaveris grojant, tada Home.** Screensaveris garso nestingdo, bet Home iš screensaverio turi sustabdyti. (Task 3)
5. **Greitas stočių perjungimas.** Negroja dvi stotys vienu metu ir nepasilieka senos stoties dainos pavadinimas. (Task 6)

---

### Task 1: Forkas, bazinis build'as, TV emuliatorius

**Files:**
- Create: `.gitignore` (papildymas), `README.md`
- Delete: nieko

**Interfaces:**
- Consumes: nieko
- Produces: veikiantis `./gradlew assembleDebug` ir paleidžiamas emuliatorius, į kurį vėlesni taškai diegia APK.

- [ ] **Step 1: Inicializuoti repo ir paimti upstream kodą**

```bash
cd /home/paulius/Projects/Radijas
git init -b main
git remote add upstream https://github.com/kalarina-bit/Fogwave.git
git fetch upstream main
git checkout -b main upstream/main
```

Tikėtina: darbinėje direktorijoje atsiranda `app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, o `docs/` ir `.superpowers/` lieka nepaliesti (jie untracked).

- [ ] **Step 2: Patikrinti, kad niekas nepradingo**

Run: `ls && git status --short`
Expected: matosi `app`, `gradle`, `build.gradle.kts`, `gradlew`, `settings.gradle.kts`, `docs/`; `git status` rodo tik `?? docs/` ir `?? .superpowers/`.

- [ ] **Step 3: Įtraukti `.superpowers/` į `.gitignore`**

Pridėti eilutę `.superpowers/` prie esamo `.gitignore`.

- [ ] **Step 4: Bazinis build'as**

```bash
export JAVA_HOME=/home/paulius/.local/share/mise/installs/java/temurin-21.0.13+9.0.LTS
export ANDROID_HOME=/home/paulius/.local/share/android-sdk
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`, failas `app/build/outputs/apk/debug/app-debug.apk`.

Jei nepavyksta dėl `compileSdk 37` — įrašyti `local.properties` su `sdk.dir=/home/paulius/.local/share/android-sdk` ir kartoti. Jei vis tiek nepavyksta, SUSTOTI ir pranešti: tolesni taškai priklauso nuo šio build'o.

- [ ] **Step 5: Sukurti TV emuliatorių**

```bash
export ANDROID_HOME=/home/paulius/.local/share/android-sdk
echo "no" | $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
  -n radijas-tv -k "system-images;android-36;android-tv;x86_64" -d tv_1080p
$ANDROID_HOME/emulator/emulator -avd radijas-tv -no-snapshot -no-audio &
$ANDROID_HOME/platform-tools/adb wait-for-device
```

Expected: `adb devices` rodo `emulator-5554  device`.

Jei nėra KVM (`/dev/kvm`), emuliatorius nepakils. Tada naudoti fizinį televizorių per `adb connect <ip>:5555` ir toliau visur „emuliatorius“ reiškia tą įrenginį.

- [ ] **Step 6: Įdiegti ir paleisti upstream appsą (bazinis patikrinimas)**

```bash
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_HOME/platform-tools/adb shell am start -n cc.skysparkle.fogwave/.MainActivity
```

Expected: telefoniška Fogwave UI atsiranda TV ekrane ir nėra TV launcheryje. Tai įrodo, kad problema, kurią sprendžiame, yra tikra.

- [ ] **Step 7: Commit**

```bash
git add .gitignore docs/
git commit -m "chore: import Fogwave upstream, add design docs"
```

---

### Task 2: Nuimti telefono appsą, pervadinti paketą, TV manifestas

**Files:**
- Delete: `app/src/main/java/cc/skysparkle/fogwave/ui/**`, `app/src/main/java/cc/skysparkle/fogwave/FogwaveApp.kt`, `app/src/test/java/cc/skysparkle/fogwave/StationItemScreenshotTest.kt`, `app/src/androidTest/**`, `fastlane/**`, `app/src/main/res/xml/automotive_app_desc.xml`
- Rename: `app/src/main/java/cc/skysparkle/fogwave/**` → `app/src/main/java/lt/radijas/tv/**`
- Modify: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `gradle/libs.versions.toml`
- Create: `app/src/main/res/drawable/banner.xml`, `app/src/main/java/lt/radijas/tv/MainActivity.kt`

**Interfaces:**
- Consumes: Task 1 build.
- Produces: appsas su `LEANBACK_LAUNCHER`, kuris paleidžiamas iš TV launcherio ir rodo tuščią tamsų ekraną su tekstu „Radijas“. `MainActivity` yra `ComponentActivity`, turinti `setContent { }`.

- [ ] **Step 1: Ištrinti telefono UI ir su juo susijusius failus**

```bash
rm -rf app/src/main/java/cc/skysparkle/fogwave/ui
rm -rf app/src/androidTest fastlane
rm -f app/src/main/java/cc/skysparkle/fogwave/FogwaveApp.kt
rm -f app/src/test/java/cc/skysparkle/fogwave/StationItemScreenshotTest.kt
rm -f app/src/test/java/cc/skysparkle/fogwave/AppInfoTest.kt
rm -f app/src/main/res/xml/automotive_app_desc.xml
rm -f app/src/main/res/xml/locales_config.xml
rm -f app/src/main/res/xml/backup_rules.xml
rm -f app/src/main/res/xml/data_extraction_rules.xml
rm -f app/src/main/res/xml/network_security_config.xml
```

`AppInfoTest.kt` šalinamas todėl, kad jis importuoja ištrintą `ui/` ir tikrina pašalintus `RadioStation` laukus. Jo vertingos patikros (unikalūs id, https, jokių `rj-tok`) perkeliamos į Task 3 kaip `StationCatalogTest`.

`app/src/main/res/drawable-nodpi/ic_station_*.webp` (10 failų) **palikti** — jie bus naudojami.

- [ ] **Step 2: Pervadinti paketą**

```bash
mkdir -p app/src/main/java/lt/radijas/tv
git mv app/src/main/java/cc/skysparkle/fogwave/data app/src/main/java/lt/radijas/tv/data
git mv app/src/main/java/cc/skysparkle/fogwave/player app/src/main/java/lt/radijas/tv/player
git mv app/src/main/java/cc/skysparkle/fogwave/MainActivity.kt app/src/main/java/lt/radijas/tv/MainActivity.kt
rmdir app/src/main/java/cc/skysparkle/fogwave app/src/main/java/cc/skysparkle 2>/dev/null || true
```

Pakeisti `package cc.skysparkle.fogwave` → `package lt.radijas.tv` ir `import cc.skysparkle.fogwave.` → `import lt.radijas.tv.` visuose likusiuose `.kt` failuose.

- [ ] **Step 3: Sutvarkyti `app/build.gradle.kts`**

- `namespace = "lt.radijas.tv"`, `applicationId = "lt.radijas.tv"`.
- Pašalinti `roborazzi` pluginą **tik iš `:app`** (`plugins { }` bloke) ir visas `roborazzi*` priklausomybes. Šakninio `build.gradle.kts` ir `libs.versions.toml` `roborazzi` įrašų **neliesti** — šakninis `alias(libs.plugins.roborazzi) apply false` turi likti, kitaip Gradle nepaleis konfigūracijos.
- Pašalinti `coil-compose` priklausomybę.
- Pašalinti `BuildConfig` laukus, kurie mini pašalintas bibliotekas; palikti `MEDIA3_VERSION` ir `KOTLIN_VERSION`.
- Pridėti `implementation(libs.androidx.tv.material)`.
- Palikti `testOptions { unitTests { isIncludeAndroidResources = true } }`.

- [ ] **Step 4: Pridėti `tv-material` į version catalog**

`gradle/libs.versions.toml`:

```toml
androidx-tv-material = { group = "androidx.tv", name = "tv-material", version = "1.1.0" }
```

- [ ] **Step 5: Perrašyti `AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <uses-feature android:name="android.software.leanback" android:required="true" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <application
        android:name=".RadijasApp"
        android:allowBackup="false"
        android:banner="@drawable/banner"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Fogwave">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".player.FogwavePlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaLibraryService" />
                <action android:name="android.media.browse.MediaBrowserService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

`android:name=".RadijasApp"` reikalingas — sukuriama `app/src/main/java/lt/radijas/tv/RadijasApp.kt` su tuščiu kūnu (`class RadijasApp : Application()`). Coil `ImageLoaderFactory` nebenaudojamas.

- [ ] **Step 6: Placeholder banneras**

`app/src/main/res/drawable/banner.xml` — `layer-list`: tamsus fonas `#141218` ir per vidurį `@drawable/ic_station_lietus`, `gravity="center"`, dydis 96dp. Tikras banneras — vėliau, ne šiame taške.

- [ ] **Step 7: Minimalus `MainActivity`**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // androidx.tv.material3.MaterialTheme — ne material3, ne TvMaterialTheme.
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF141218))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Radijas", color = Color(0xFFF2EDF2))
                }
            }
        }
    }
}
```

- [ ] **Step 8: Sutvarkyti `strings.xml`**

Palikti tik `app_name` = `Radijas` ir tai, ką naudoja `player/` (klaidų tekstai `stream_unavailable`, `network_unavailable`). Ištrinti visas `station_desc_*`, `genre_*`, `tab_*`.

- [ ] **Step 9: Build'as ir diegimas**

Run: `./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Patikrinti launcherį**

```bash
adb shell cmd package resolve-activity --brief lt.radijas.tv
adb shell monkey -p lt.radijas.tv -c android.intent.category.LEANBACK_LAUNCHER 1
```

Expected: appsas matomas TV launcheryje ir paleidžiamas; ekrane tamsus fonas su „Radijas“.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: strip phone UI, rename package, add TV manifest"
```

---

### Task 3: Paleidimas ir sustabdymas

**Files:**
- Modify: `app/src/main/java/lt/radijas/tv/MainActivity.kt`, `app/src/main/java/lt/radijas/tv/player/RadioPlayerManager.kt`, `app/src/main/java/lt/radijas/tv/player/FogwavePlaybackService.kt`, `app/src/main/java/lt/radijas/tv/data/repository/RadioRepository.kt`, `app/src/main/java/lt/radijas/tv/data/model/RadioStation.kt`
- Test: `app/src/test/java/lt/radijas/tv/StartupStationTest.kt`, `app/src/test/java/lt/radijas/tv/StopPolicyTest.kt`

**Interfaces:**
- Consumes: `MainActivity`, `RadioRepository`, `RadioPlayerManager.playStation(RadioStation)`.
- Produces:
  - `RadioRepository.lastStationId(): String` — grąžina paskutinę klausytą arba `"lietus"`.
  - `RadioRepository.setLastStationId(id: String)`.
  - `StartupStation.resolve(repository: RadioRepository): RadioStation`.
  - `StopPolicy.shouldStop(userLeftHint: Boolean): Boolean`.
  - `RadioPlayerManager.stop()`.

- [ ] **Step 1: Parašyti nepavykstantį testą — pirmas paleidimas**

`app/src/test/java/lt/radijas/tv/StartupStationTest.kt` (Robolectric, `@RunWith(RobolectricTestRunner::class)`):

```kotlin
@Test fun `first launch plays lietus`() {
    val repo = RadioRepository(ApplicationProvider.getApplicationContext())
    assertEquals("lietus", StartupStation.resolve(repo).id)
}

@Test fun `second launch plays last station`() {
    val repo = RadioRepository(ApplicationProvider.getApplicationContext())
    repo.setLastStationId("zip")
    assertEquals("zip", StartupStation.resolve(repo).id)
}

@Test fun `unknown saved id falls back to lietus`() {
    val repo = RadioRepository(ApplicationProvider.getApplicationContext())
    repo.setLastStationId("deleted-station")
    assertEquals("lietus", StartupStation.resolve(repo).id)
}
```

- [ ] **Step 2: Paleisti ir įsitikinti, kad krenta**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.StartupStationTest"`
Expected: FAIL — `StartupStation` neegzistuoja.

- [ ] **Step 3: Sutrumpinti `RadioStation` ir įgyvendinti `StartupStation`**

`RadioStation` palikti tik: `id: String`, `name: String`, `streamUrl: String`, `@DrawableRes iconResId: Int`, `brandColor: Long`. Pašalinti `descriptionRes`, `genres`, `bitrate`, `lightBackdrop`. `brandColor` reikšmės — iš spec §6.5 lentelės (`0xFF0E8FD1L` ir t. t.).

`StartupStation.kt`:

```kotlin
object StartupStation {
    const val DEFAULT_ID = "lietus"
    fun resolve(repository: RadioRepository): RadioStation =
        repository.stations.firstOrNull { it.id == repository.lastStationId() }
            ?: repository.stations.first { it.id == DEFAULT_ID }
}
```

`RadioRepository.lastStationId(): String` grąžina `prefs.getString("last_station_id", null) ?: DEFAULT_ID`. Ištrinti favorites, recents, view mode, size step, battery prompt metodus.

- [ ] **Step 4: Testai praeina**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.StartupStationTest"`
Expected: PASS (3 testai).

- [ ] **Step 4b: Perkelti stoties katalogo patikras iš ištrinto `AppInfoTest`**

`app/src/test/java/lt/radijas/tv/StationCatalogTest.kt`:

```kotlin
@Test fun `ids are unique`() {
    val stations = RadioRepository(context).stations
    assertEquals(stations.size, stations.map { it.id }.toSet().size)
}

@Test fun `all streams are https`() {
    RadioRepository(context).stations.forEach { assertTrue(it.streamUrl, it.streamUrl.startsWith("https://")) }
}

@Test fun `no expiring revma tokens`() {
    RadioRepository(context).stations.forEach { assertFalse(it.streamUrl, it.streamUrl.contains("rj-tok")) }
}

@Test fun `there are ten stations and lietus is one of them`() {
    val stations = RadioRepository(context).stations
    assertEquals(10, stations.size)
    assertEquals("Lietus", stations.first { it.id == "lietus" }.name)
}
```

Šis testas neturi naujo produkto kodo — jis turi praeiti iš karto. Jei krenta, šiame taške buvo sugadintas stočių sąrašas.

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.StationCatalogTest"`
Expected: PASS (4 testai).

- [ ] **Step 5: Parašyti nepavykstantį testą — sustabdymo taisyklė**

`StopPolicyTest.kt`:

```kotlin
@Test fun `leaving via home stops playback`() = assertTrue(StopPolicy.shouldStop(userLeftHint = true))
@Test fun `screensaver does not stop playback`() = assertFalse(StopPolicy.shouldStop(userLeftHint = false))
```

- [ ] **Step 6: Paleisti ir įsitikinti, kad krenta**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.StopPolicyTest"`
Expected: FAIL — `StopPolicy` neegzistuoja.

- [ ] **Step 7: Įgyvendinti `StopPolicy` ir sujungti `MainActivity`**

```kotlin
object StopPolicy {
    fun shouldStop(userLeftHint: Boolean): Boolean = userLeftHint
}
```

`MainActivity`:
- `onCreate`: `val station = StartupStation.resolve(repository)` → `playerManager.playStation(station)`; `repository.setLastStationId(station.id)`.
- `onUserLeaveHint()`: `if (StopPolicy.shouldStop(true)) { playerManager.stop(); repo.setLastStationId(...) }`.
- `onDestroy`: `playerManager.stop(); playerManager.release()` (jei `isFinishing`).
- `RadioPlayerManager.stop()`: `controller?.stop(); controller?.clearMediaItems()`.

`FogwavePlaybackService`: pašalinti `MAX_RETRY_ATTEMPTS` ribą — `scheduleReconnect()` visada planuoja kitą bandymą, `retryDelaysMs` išlieka 1→30 s, o po paskutinio elemento visada 30 s. Vietoj `player.pause()` ties riba palikti `player.prepare()` pakartojimą.

- [ ] **Step 8: Testai praeina**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, įskaitant `AppInfoTest` (jei jis dar egzistuoja ir mini seną paketą — atnaujinti importus).

- [ ] **Step 9: Rankinis patikrinimas emuliatoriuje**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p lt.radijas.tv -c android.intent.category.LEANBACK_LAUNCHER 1
adb logcat -s RadioPlayerManager FogwavePlaybackService
```

Expected: loguose `Station requested: lietus`, po kelių sekundžių `Playing`. Paspaudus Home (`adb shell input keyevent KEYCODE_HOME`) — `stopService` ir tyla.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: auto-play last station on launch, stop on leave"
```

---

### Task 4: Ekranas su fokusuojama lentyna

**Files:**
- Create: `app/src/main/java/lt/radijas/tv/ui/NowPlayingScreen.kt`, `ui/HeroPanel.kt`, `ui/StationShelf.kt`, `ui/StationTile.kt`, `ui/theme/RadijasTheme.kt`
- Modify: `app/src/main/java/lt/radijas/tv/MainActivity.kt`
- Test: `app/src/test/java/lt/radijas/tv/StationShelfTest.kt`

**Interfaces:**
- Consumes: `RadioStation` (Task 3), `RadioRepository.stations`.
- Produces:
  - `@Composable fun StationShelf(stations: List<RadioStation>, playingId: String, onSelect: (RadioStation) -> Unit, modifier: Modifier = Modifier)`
  - Kiekviena plytelė turi `Modifier.testTag("station-tile-${station.id}")`.
  - `@Composable fun HeroPanel(station: RadioStation, modifier: Modifier = Modifier)`
  - `@Composable fun NowPlayingScreen(station: RadioStation, stations: List<RadioStation>, onSelect: (RadioStation) -> Unit)` — Task 5 papildys šią signatūrą `status` ir `trackTitle` parametrais.

- [ ] **Step 1: Parašyti nepavykstantį fokuso testą**

`StationShelfTest.kt` (Robolectric + `createComposeRule`):

```kotlin
@Test fun `focus starts on playing station`() {
    composeTestRule.setContent { StationShelf(stations, playingId = "m1", onSelect = {}) }
    composeTestRule.onNodeWithTag("station-tile-m1").assertIsFocused()
}

@Test fun `right moves focus to next station`() {
    composeTestRule.setContent { StationShelf(stations, playingId = "zip", onSelect = {}) }
    composeTestRule.onNodeWithTag("station-tile-zip").performKeyInput { pressKey(Key.DirectionRight) }
    composeTestRule.onNodeWithTag("station-tile-powerhit").assertIsFocused()
}

@Test fun `left at first station keeps focus`() {
    composeTestRule.setContent { StationShelf(stations, playingId = "zip", onSelect = {}) }
    composeTestRule.onNodeWithTag("station-tile-zip").performKeyInput { pressKey(Key.DirectionLeft) }
    composeTestRule.onNodeWithTag("station-tile-zip").assertIsFocused()
}

@Test fun `up and down do nothing`() {
    composeTestRule.setContent { StationShelf(stations, playingId = "zip", onSelect = {}) }
    composeTestRule.onNodeWithTag("station-tile-zip").performKeyInput { pressKey(Key.DirectionDown) }
    composeTestRule.onNodeWithTag("station-tile-zip").assertIsFocused()
}

@Test fun `ok on focused station calls onSelect`() {
    var selected: RadioStation? = null
    composeTestRule.setContent { StationShelf(stations, playingId = "m1", onSelect = { selected = it }) }
    composeTestRule.onNodeWithTag("station-tile-m1").performKeyInput { pressKey(Key.DirectionCenter) }
    assertEquals("m1", selected?.id)
}
```

`stations` — 10 elementų sąrašas su `id` tokiais kaip spec §6.5 (`zip`, `powerhit`, `powerhitgold`, `rock`, `relax`, `lietus`, `m1`, `plus`, `goldfm`, `rc`), surikiuotas taip, kad `zip` būtų pirmas.

- [ ] **Step 2: Paleisti ir įsitikinti, kad krenta**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.StationShelfTest"`
Expected: FAIL — `StationShelf` neegzistuoja.

- [ ] **Step 3: Įgyvendinti `StationTile` ir `StationShelf`**

- `StationTile`: `Box` su `Modifier.focusable()` + `.onFocusChanged { isFocused = it.isFocused }` + `.testTag("station-tile-${station.id}")` + `.onKeyEvent { Key.DirectionCenter -> onSelect(); true }`. Fokusuotam — `border(3.dp, Color.White)`.
- `StationShelf`: `LazyRow` su `rememberLazyListState()`, `contentPadding = PaddingValues(horizontal = 48.dp)`, tarpas 16.dp. Kiekvienai plytelei `FocusRequester`; `LaunchedEffect(playingId, stations) { listState.scrollToItem(index); requesters[playingId]?.requestFocus() }` — **`scrollToItem` būtinas prieš `requestFocus`**, nes `LazyRow` nesukomponuoja už ekrano esančių elementų ir fokusas į juos tiesiog neįvyktų. Kai fokusas pasikeičia — `animateScrollToItem`, kad plytelė būtų matoma.
- **Jokio `focusGroup`, jokio vyniojimosi.** `LazyRow` su `Modifier.focusable()` neturi peršokti per galus; jei Compose peršauna, pridėti `focusProperties { right = if (last) FocusRequester.Cancel else FocusRequester.Default }` paskutinei plytelei ir analogiškai `left` pirmai.

- [ ] **Step 4: Testai praeina**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.StationShelfTest"`
Expected: PASS (5 testai).

Jei Robolectric nepalaiko `performKeyInput` su TV fokusu — perkelti šiuos testus į `app/src/androidTest` ir paleisti emuliatoriuje (`./gradlew connectedDebugAndroidTest`). Testų turinys nesikeičia.

- [ ] **Step 5: Įgyvendinti `HeroPanel` ir `NowPlayingScreen`**

`HeroPanel`: stoties pavadinimas (`displaySmall`, 600 svoris), `brandColor` gradientas fone, stoties ikona dešinėje. **Nefokusuojamas** — jokių `clickable`/`focusable` modifikatorių.

`NowPlayingScreen`: `Column` — `HeroPanel` su `weight(1f)`, po juo `StationShelf`. Fono spalva `#141218`, saugi zona 48.dp horizontaliai / 27.dp vertikaliai.

- [ ] **Step 6: Sujungti `MainActivity`**

`setContent { NowPlayingScreen(...) }` su laikinu `NowPlayingUiState` iš `RadioRepository.stations` ir `playingId` iš `StartupStation.resolve(...)`.

- [ ] **Step 7: Patikrinti emuliatoriuje**

Run: `./gradlew installDebug && adb shell monkey -p lt.radijas.tv -c android.intent.category.LEANBACK_LAUNCHER 1`
Expected: matosi hero ir lentyna; pulto ← → judina baltą rėmelį; ↑ ↓ nejudina; OK ant kitos stoties — nieko dar nevyksta (prijungiama Task 5).

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add now-playing screen with focusable station shelf"
```

---

### Task 5: Būsenos ir tekstai

**Files:**
- Create: `app/src/main/java/lt/radijas/tv/ui/NowPlayingUiState.kt`, `ui/PlayerStatus.kt`
- Modify: `ui/NowPlayingScreen.kt`, `ui/HeroPanel.kt`, `MainActivity.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/lt/radijas/tv/PlayerStatusTest.kt`

**Interfaces:**
- Consumes: `RadioPlayerManager.playerState: StateFlow<PlayerState>`, `PlayerState` (`Idle`, `Connecting`, `Playing`, `Paused`, `Error(message)`).
- Produces:
  - `enum class PlayerStatus { CONNECTING, PLAYING, RECONNECTING, NO_CONNECTION, PAUSED }`
  - `fun playerStatus(playerState: PlayerState, playWhenReady: Boolean, silentForMs: Long): PlayerStatus`
  - `data class NowPlayingUiState(val station: RadioStation, val status: PlayerStatus, val trackTitle: String?)`
  - `const val NO_CONNECTION_AFTER_MS = 30_000L`

- [ ] **Step 1: Parašyti nepavykstantį testą**

```kotlin
@Test fun `playing maps to PLAYING`() =
    assertEquals(PLAYING, playerStatus(PlayerState.Playing, true, 0))

@Test fun `buffering while playing maps to CONNECTING`() =
    assertEquals(CONNECTING, playerStatus(PlayerState.Connecting, true, 0))

@Test fun `failure maps to NO_CONNECTION immediately`() =
    assertEquals(NO_CONNECTION, playerStatus(PlayerState.Error("x"), true, 0))

@Test fun `long silence maps to NO_CONNECTION`() =
    assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 31_000))

@Test fun `short silence still retrying`() =
    assertEquals(RECONNECTING, playerStatus(PlayerState.Connecting, true, 5_000))

@Test fun `paused while playWhenReady false is PAUSED`() =
    assertEquals(PAUSED, playerStatus(PlayerState.Paused, false, 0))

@Test fun `idle is CONNECTING`() =
    assertEquals(CONNECTING, playerStatus(PlayerState.Idle, true, 0))
```

- [ ] **Step 2: Paleisti ir įsitikinti, kad krenta**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.PlayerStatusTest"`
Expected: FAIL — `playerStatus` neegzistuoja.

- [ ] **Step 3: Įgyvendinti `playerStatus`**

Taisyklės eilės tvarka: `Error` → `NO_CONNECTION`; `Playing` → `PLAYING`; `Paused && !playWhenReady` → `PAUSED`; `silentForMs >= NO_CONNECTION_AFTER_MS` → `NO_CONNECTION`; `silentForMs > 0` → `RECONNECTING`; kitaip `CONNECTING`.

- [ ] **Step 4: Testai praeina**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.PlayerStatusTest"`
Expected: PASS (7 testai).

- [ ] **Step 5: Įtraukti būsenas į UI**

`HeroPanel` gauna `status` ir rodo po pavadinimu:
- `CONNECTING` → „Jungiamasi…“
- `PLAYING` → „Gyvai“ + trys ekvalaizerio brūkšneliai
- `RECONNECTING` → „Bandau vėl…“
- `NO_CONNECTION` → „Nepavyksta prisijungti. Paspausk OK.“
- `PAUSED` → „Pristabdyta. Paspausk OK.“

`MainActivity` laiko `silentSince` (nustatomas kai būsena nustoja būti `Playing`) ir perduoda `playerStatus(...)`. OK ant fokusuotos stoties kviečia `playerManager.playStation(...)`; kai `status == NO_CONNECTION || PAUSED`, OK kviečia `playerManager.retry()`.

- [ ] **Step 6: Testai ir rankinis patikrinimas**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

Rankiniu būdu: išjungti emuliatoriaus tinklą (`adb shell svc wifi disable` / `svc data disable`), paleisti appsą — po ~30 s pasirodo „Nepavyksta prisijungti“, bet loguose bandymai tęsiasi kas 30 s. Įjungus tinklą — grįžta „Gyvai“ be naudotojo veiksmo.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: map playback states to on-screen status copy"
```

---

### Task 6: „Kas groja“ eilutė iš ICY meta

**Files:**
- Create: `app/src/main/java/lt/radijas/tv/player/TrackTitleParser.kt`
- Modify: `player/FogwavePlaybackService.kt`, `player/RadioPlayerManager.kt`, `ui/HeroPanel.kt`
- Test: `app/src/test/java/lt/radijas/tv/TrackTitleParserTest.kt`

**Interfaces:**
- Consumes: `Player.Listener.onMetadata` (Media3), `IcyInfo`.
- Produces:
  - `fun parseTrackTitle(raw: String?): String?` — grąžina `null`, jei tuščia, jei tekstas lygus stoties pavadinimui, arba jei trumpesnis nei 2 simboliai.
  - `RadioPlayerManager.trackTitle: StateFlow<String?>`

- [ ] **Step 1: Parašyti nepavykstantį testą**

```kotlin
@Test fun `keeps artist and title`() = assertEquals("Dua Lipa — Levitating", parseTrackTitle("Dua Lipa — Levitating"))
@Test fun `trims whitespace`() = assertEquals("ABBA — SOS", parseTrackTitle("  ABBA — SOS  "))
@Test fun `null stays null`() = assertNull(parseTrackTitle(null))
@Test fun `blank becomes null`() = assertNull(parseTrackTitle("   "))
@Test fun `single character becomes null`() = assertNull(parseTrackTitle("x"))
@Test fun `ad text is kept as-is`() = assertEquals("Reklama: Ledo ritulys", parseTrackTitle("Reklama: Ledo ritulys"))
```

- [ ] **Step 2: Paleisti ir įsitikinti, kad krenta**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.TrackTitleParserTest"`
Expected: FAIL.

- [ ] **Step 3: Įgyvendinti `parseTrackTitle`**

`raw?.trim()?.takeIf { it.length >= 2 }`. **Jokio filtravimo pagal turinį** — reklama ir DJ tekstas rodomi kaip yra (spec §6.6).

- [ ] **Step 4: Prijungti `onMetadata`**

`FogwavePlaybackService`: `Player.Listener.onMetadata(metadata: Metadata)` → `metadata.getFirstEntryOfType(IcyInfo::class.java)?.title` → perduoti į `RadioPlayerManager` (per `MediaSession` `player` listener grandinę arba per `RadioPlayerManager` prisijungusį `MediaController` listenerį).

Paprasčiausias kelias, kuris veikia: `RadioPlayerManager` jau turi `MediaController`; pridėti `override fun onMetadata(metadata: Metadata)` į `playerListener` ir ten kviesti `_trackTitle.value = parseTrackTitle(icy.title)`. `clearMediaItems()` / `playStation()` išvalo `_trackTitle`.

- [ ] **Step 5: Testai praeina**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Įtraukti eilutę į hero**

`HeroPanel`: fiksuotas `height(48.dp)` blokas po pavadinimu, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, spalva `Color.White.copy(alpha = 0.66f)`, `fontSize = 26.sp`. Kai `trackTitle == null` — blokas lieka, tekstas tuščias. **Jokio `basicMarquee`.**

Patikrinti, kad pavadinimo keitimas nejudina išdėstymo: hero pavadinimas ir eilutė turi būti tame pačiame `Column` su fiksuotu eilutės aukščiu.

- [ ] **Step 7: Rankinis patikrinimas**

Run: `adb logcat -s RadioPlayerManager | grep -i track`
Expected: kas ~10–30 s pasirodo skirtingi dainų pavadinimai. Perjungus stotį — senas pavadinimas išnyksta iš karto, ne po kelių sekundžių.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: show ICY track title in hero"
```

---

### Task 7: Vizualinė kalba

**Files:**
- Modify: `ui/theme/RadijasTheme.kt`, `ui/HeroPanel.kt`, `ui/StationTile.kt`, `ui/StationShelf.kt`
- Test: `app/src/test/java/lt/radijas/tv/BrandPaletteTest.kt`

**Interfaces:**
- Consumes: `RadioStation.brandColor: Long`.
- Produces:
  - `fun tonalSurface(brand: Long, amount: Float): Color` — `brand` sumaišyta su `#141218` santykiu `amount`.
  - `fun onBrand(brand: Long): Color` — tekstui ant toninio paviršiaus, kontrastas ≥ 4.5:1.

- [ ] **Step 1: Parašyti nepavykstantį testą**

```kotlin
@Test fun `zero amount returns base surface`() =
    assertEquals(Color(0xFF141218), tonalSurface(0xFF0E8FD1, 0f))

@Test fun `full amount returns brand color`() =
    assertEquals(Color(0xFF0E8FD1), tonalSurface(0xFF0E8FD1, 1f))

@Test fun `mix is between the two`() {
    val mixed = tonalSurface(0xFF0E8FD1, 0.5f)
    assertTrue(mixed.red in 0.05f..0.12f)
    assertTrue(mixed.blue in 0.6f..0.9f)
}

@Test fun `every station brand has readable text`() {
    STATION_BRANDS.forEach { brand ->
        val contrast = contrastRatio(onBrand(brand), tonalSurface(brand, 0.34f))
        assertTrue("contrast $contrast for $brand", contrast >= 4.5f)
    }
}
```

`contrastRatio` — pagalbinė funkcija teste (WCAG 2.1 santykinis šviesumas).

- [ ] **Step 2: Paleisti ir įsitikinti, kad krenta**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.BrandPaletteTest"`
Expected: FAIL.

- [ ] **Step 3: Įgyvendinti paletę**

`tonalSurface` — tiesinis RGB interpoliacijos tarp `#141218` ir `brand`. `onBrand` — `brand` šviesinama tol, kol kontrastas prieš `tonalSurface(brand, 0.34f)` pasiekia 4.5. Jei pasiekiama balta, grąžinti baltą.

- [ ] **Step 4: Testai praeina**

Run: `./gradlew testDebugUnitTest --tests "lt.radijas.tv.BrandPaletteTest"`
Expected: PASS visoms 10 stočių.

- [ ] **Step 5: Pritaikyti vizualą**

- Hero fonas: `Brush.radialGradient` iš `tonalSurface(brand, 0.55f)` į `#141218`, kaip makete.
- Plytelės fonas `tonalSurface(brand, 0.15f)`; fokusuota — `0.34f` + `border(3.dp, onBrand(brand))` + `scale(1.05f)`.
- Saugi zona: `padding(horizontal = 48.dp, vertical = 27.dp)` visame ekrane.
- Tipografija: hero pavadinimas `96.sp`, `FontWeight.Bold`, `letterSpacing = (-1.5).sp`; dainos eilutė `26.sp`; plytelės pavadinimas `16.sp`.

- [ ] **Step 6: Rankinis patikrinimas**

Run: `./gradlew installDebug`
Expected: perjungiant stotis hero ir fokuso rėmelis keičia spalvą; nė viena stotis neturi neįskaitomo teksto; ekrano kraštai nekerpa turinio.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: derive tonal palette and focus ring from station brand colour"
```

---

### Task 8: Medijos pranešimas be transporto valdymo

**Files:**
- Create: `app/src/main/java/lt/radijas/tv/player/RadijasNotificationProvider.kt`
- Modify: `player/FogwavePlaybackService.kt`, `AndroidManifest.xml`

**Interfaces:**
- Consumes: `MediaSessionService`.
- Produces: pranešimas, kuriame yra tik stoties pavadinimas ir „Stabdyti“ veiksmas.

- [ ] **Step 1: Įgyvendinti `RadijasNotificationProvider`**

`MediaNotification.Provider` implementacija, kuri grąžina `MediaNotification` be `COMMAND_PLAY_PAUSE`, `COMMAND_SEEK_*`. `DefaultMediaNotificationProvider` perrašyti taip, kad `NotificationCompat.Builder` neturėtų `MediaStyle` transporto mygtukų; vietoj jų — vienas `PendingIntent` į `MainActivity` su veiksmu `ACTION_STOP`.

- [ ] **Step 2: Prijungti servise**

`FogwavePlaybackService.onCreate`: `setMediaNotificationProvider(RadijasNotificationProvider(this))`.

- [ ] **Step 3: Patikrinti**

```bash
adb shell dumpsys notification --noredact | grep -A 20 lt.radijas.tv
```

Expected: pranešime nėra `action=androidx.media3.session.action.PLAY` nei `PAUSE`; yra `lt.radijas.tv.STOP`.

- [ ] **Step 4: Patikrinti Assistant kelią**

`adb shell cmd media_session list-sessions` → pasirinkti `lt.radijas.tv` sesiją → `adb shell cmd media_session dispatch play` turi būti atmesta (`ERROR`), nes `COMMAND_PLAY_PAUSE` neleidžiamas.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: notification with stop only, no transport controls"
```

---

### Task 9: Tikro televizoriaus patikra ir release

**Files:**
- Modify: `app/build.gradle.kts` (`versionCode`, `versionName`), `README.md`

**Interfaces:**
- Consumes: visa ankstesnė darbo šaka.
- Produces: pasirašytas APK, įdiegtas tikrame televizoriuje.

- [ ] **Step 1: Prijungti televizorių**

```bash
adb connect <TV_IP>:5555
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.model
```

Užfiksuoti modelį ir SDK lygį `README.md` skyriuje „Testuota ant“. Jei SDK < 24 — peržiūrėti `minSdk` sprendimą prieš einant toliau.

- [ ] **Step 2: Pereiti spec §8 kontrolinį sąrašą**

Kiekvieną punktą pažymėti `[x]` arba `[ ]` su priežastimi:

- [ ] Šaltas startas → garsas ≤ 5 s.
- [ ] Visas appsas įveikiamas vien pultu.
- [ ] Iš bet kurios būsenos iki bet kurios stoties ≤ 2 paspaudimai.
- [ ] Nė vieno ekrano be fokuso.
- [ ] Dainos eilutė neslenka ir nejudina išdėstymo.
- [ ] Back ir Home nutildo; screensaveris — ne.
- [ ] Home iš screensaverio nutildo.
- [ ] Pranešime nėra play/pause.
- [ ] `adb shell dumpsys package lt.radijas.tv | grep -i permission` — tik tie penki leidimai.

- [ ] **Step 3: Patikrinti overscan**

`adb shell settings put global overscan 20,20,20,20` (jei palaikoma), paleisti appsą — tekstas neturi liestis ekrano krašto. Atstatyti `0,0,0,0`.

- [ ] **Step 4: Release build**

```bash
export KEYSTORE_PATH=/home/paulius/Projects/Radijas/radijas-release.jks
export STORE_PASSWORD=... KEY_ALIAS=radijas KEY_PASSWORD=...
./gradlew assembleRelease
```

Keystore **niekada** neįtraukiamas į git (`.gitignore` jau turi `*.jks`).

- [ ] **Step 5: Įdiegti release ir pakartoti 2 žingsnį**

Run: `adb install -r app/build/outputs/apk/release/app-release.apk`
Expected: elgsena identiška debug, išskyrus `minifyEnabled`.

- [ ] **Step 6: Atnaujinti `README.md`**

Nurodyti: tai Fogwave forkas, GPL-3.0-only, ką pakeitėme, kaip įdiegti, kaip sukurti naują versiją.

- [ ] **Step 7: Commit ir tag**

```bash
git add -A
git commit -m "docs: document TV fork, verified on real device"
git tag v0.1.0
```
