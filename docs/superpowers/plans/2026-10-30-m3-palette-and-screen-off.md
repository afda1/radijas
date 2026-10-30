# Radijas: M3 paletė ir ekrano užgesinimas — įgyvendinimo planas

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (šis planas parašytas savam vykdymui) arba superpowers:subagent-driven-development.

**Goal:** Pakeisti rankinę spalvų paletę HCT sugeneruota M3 palete, įjungti `MaterialTheme` tokenus, ir pridėti ekrano užgesinimo mygtuką, kuris pajuodina ekraną nebeatimdamas garso.

**Architecture:** Dev įrankis `tools/palette/generate.mjs` (oficialus Google `material-color-utilities`) sugeneruoja `StationPalette.kt` su 11 omų toninėmis paletėmis; `RadijasTheme` jas paverčia `tv-material` `ColorScheme` ir perduoda `shapes`/`typography`. UI skaito roles iš `MaterialTheme`, o ne iš rankinių funkcijų. Ekrano užgesinimas: `ScreenOffPolicy` (gryna funkcija) + juodas sluoksnis Compose + `dispatchKeyEvent` sugerimas + `FLAG_KEEP_SCREEN_ON`.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv:tv-material` 1.1.0), JUnit4 + Robolectric, Node 24 (dev įrankis).

**Spec:** `docs/superpowers/specs/2026-10-30-m3-system-and-screen-off-design.md`

## Global Constraints

- `tv-material` `ColorScheme` turi roles: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `border`, `borderVariant`, `scrim`. **Nėra** `surfaceContainer*`.
- Nenaudojama `androidx.compose.material3`; nepridedama runtime priklausomybių.
- Kontrastai: tekstas ≥ 4.5:1, fokuso rėmelis (`border`) ≥ 6:1 prie `surface`, `surfaceVariant`, `primaryContainer`.
- Hero fonas `surface`, plytelės `surfaceVariant`, fokusuota plytelė `primaryContainer`.
- Fokusas akimirksniu (jokios animacijos), viena fokusuota eilutė; `↑` veda tik į „Ekranas" mygtuką.
- Tekstai lietuviški, `strings.xml`; jokių naujų nustatymų ar ekranų.
- Testai: JUnit4 + Robolectric, be emuliatoriaus. Visi komentarai/pavadinimai kaip esamame kode.

## Review Focus

1. **Paletė, kuri neišlaiko kontrasto** (stoties atspalvis per šviesus/tamsus) — testas `StationPaletteTest` tikrina 11 × poras.
2. **Rėmelis, nesusiskiriantis nuo fokusuotos plytelės** — `StationPaletteTest` tikrina `border` ≥ 6:1 prie trijų paviršių.
3. **Hero turinys nebetelpa** po logotipo 168→136 ir naujo mygtuko — `HeroPanelTest` tikrina 202 dp biudžetą.
4. **Pirmas klavišas po užgesinimo dar ir suveikia** — `ScreenOffPolicyTest` + `ScreenOffOverlayTest`; realus pultas — įrenginyje.
5. **Garsas nutrūksta užgesinus** — emuliatoriaus `logcat` (`RadijasPlaybackService` „Playing" po užgesinimo) 3 užduotyje.

---

### Task 1: Sugeneruota stoties paletė

**Files:**
- Create: `tools/palette/generate.mjs`, `tools/palette/package.json`
- Create (generated): `app/src/main/java/lt/radijas/tv/ui/theme/StationPalette.kt`
- Test: `app/src/test/java/lt/radijas/tv/StationPaletteTest.kt`

**Interfaces:**
- Produces: `internal data class StationColors(val primary: Long, val onPrimary: Long, val primaryContainer: Long, val onPrimaryContainer: Long, val surface: Long, val onSurface: Long, val surfaceVariant: Long, val onSurfaceVariant: Long, val border: Long)` ir `internal val stationColors: Map<String, StationColors>` (raktas — `RadioStation.id`), faile `ui/theme/StationPalette.kt`.

- [ ] **Step 1: Write the failing test**

```kotlin
package lt.radijas.tv

import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.theme.stationColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationPaletteTest {
    private val stations = RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations

    @Test
    fun `every catalogue station has a generated palette`() {
        assertEquals(11, stations.size)
        assertEquals(stations.map { it.id }.toSet(), stationColors.keys)
    }

    @Test
    fun `text and focus ring keep their contrast on every station`() {
        stations.forEach { station ->
            val c = stationColors.getValue(station.id)
            listOf(
                "onSurface" to (c.onSurface to c.surface),
                "onSurface 66%" to (compose(c.onSurface, c.surface, 0.66f) to c.surface),
                "onSurfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
                "onPrimaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
            ).forEach { (name, pair) ->
                assertTrue("${station.id} $name: ${contrast(pair.first, pair.second)}", contrast(pair.first, pair.second) >= 4.5)
            }
            listOf(c.surface, c.surfaceVariant, c.primaryContainer).forEach { fill ->
                assertTrue("${station.id} border on ${hex(fill)}", contrast(c.border, fill) >= 6.0)
            }
        }
    }
}
```

`compose(fg, bg, alpha)` ir `contrast(a, b)` — testinės pagalbinės (WCAG 2.1 formulė, kaip esamame `BrandPaletteTest`; `compose` sudeda alfa kanalą).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests lt.radijas.tv.StationPaletteTest --offline` (su `JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot`)
Expected: FAIL — unresolved `stationColors`.

- [ ] **Step 3: Write the generator**

`tools/palette/package.json`: `{"name":"radijas-palette","private":true,"type":"module","dependencies":{"@material/material-color-utilities":"0.4.0"}}`

`tools/palette/generate.mjs`:
- `npm install --prefix tools/palette` (vieną kartą, dev).
- Skaito `app/src/main/java/lt/radijas/tv/data/repository/RadioRepository.kt`.
- Regex `id = "([a-z0-9]+)"` ir tame pačiame bloke `brandColor = 0x([0-9A-Fa-f]{8})L`; surinkus turi būti **11** įrašų, kitu atveju `process.exit(1)`.
- Kiekvienam: `new SchemeTonalSpot(Hct.fromInt(argb), true, 0.0)`; iš `MaterialDynamicColors` ima 8 roles; `border`: `primary` šviesinamas po 0,01 link balto, kol ≥ 6:1 prie `surface`, `surfaceVariant`, `primaryContainer` (imamas pirmas tinkamas, daugiausiai 0,18).
- Rašo `StationPalette.kt` su `StationColors(...)` ir `stationColors = mapOf("zip" to StationColors(0xFF…), …)`.
- Įrankiui reikia ESM `.js` plėtinių pataisos (paketas 0.4.0 importuoja be `.js`) — skriptas prieš paleidimą patchina `node_modules` kopiją (`from './x'` → `from './x.js'`), arba naudojamas `--experimental-specifier-resolution`; sprendimą renkasi vykdytojas, svarbu kad `node tools/palette/generate.mjs` veiktų.

- [ ] **Step 4: Run test to verify it passes**

Run: `node tools/palette/generate.mjs && ./gradlew.bat testDebugUnitTest --tests lt.radijas.tv.StationPaletteTest --offline`
Expected: PASS, faile 11 įrašų.

- [ ] **Step 5: Commit**

```bash
git add tools/palette app/src/main/java/lt/radijas/tv/ui/theme/StationPalette.kt app/src/test/java/lt/radijas/tv/StationPaletteTest.kt
git commit -m "feat: generate M3 station palettes from brand colours"
```

---

### Task 2: Tema ir UI spalvos iš rolių

**Files:**
- Modify: `app/src/main/java/lt/radijas/tv/ui/theme/RadijasTheme.kt`
- Modify: `app/src/main/java/lt/radijas/tv/ui/HeroPanel.kt`, `StationTile.kt`, `StationShelf.kt`, `NowPlayingScreen.kt`, `MainActivity.kt`
- Delete: `app/src/test/java/lt/radijas/tv/BrandPaletteTest.kt` (pakeistas 1 užduotyje)

**Interfaces:**
- Consumes: `stationColors` (1 užduotis).
- Produces: `@Composable fun RadijasTheme(stationId: String = StartupStation.DEFAULT_ID, content: @Composable () -> Unit)`; `internal val RadijasShapes: Shapes`; `internal val RadijasTypography: Typography`; `internal fun stationScheme(stationId: String): ColorScheme`.

- [ ] **Step 1: Write the failing test**

`HeroPanelTest` papildymas (jie jau kuria `RadijasTheme { HeroPanel(...) }`):

```kotlin
@Test
fun `hero name and status sit on the station surface`() {
    composeTestRule.setContent {
        RadijasTheme(stationId = "lietus") {
            HeroPanel(station = station, status = PlayerStatus.PLAYING, modifier = Modifier.requiredSize(1200.dp, 400.dp))
        }
    }
    composeTestRule.onNodeWithTag("hero-status").assertIsDisplayed()
}
```

Run → FAIL (nėra `RadijasTheme(stationId)`, `tonalSurface` dar naudojamas).

- [ ] **Step 2: Implement theme tokens**

`RadijasTheme.kt`:
- `internal val RadijasShapes = Shapes(extraSmall = RoundedCornerShape(4.dp), small = 8.dp, medium = 12.dp, large = 16.dp, extraLarge = RoundedCornerShape(28.dp))`.
- `internal val RadijasTypography = Typography(displayLarge = TextStyle(fontSize = 96.sp, lineHeight = 105.6.sp, fontWeight = Bold, letterSpacing = (-1.5).sp), headlineSmall = 26/32, titleMedium = 24/30 SemiBold, labelLarge = 16/22 + 0.1.sp tracking)`.
- `internal fun stationScheme(stationId: String): ColorScheme` — `val c = stationColors[stationId] ?: stationColors.getValue(StartupStation.DEFAULT_ID)`; `darkColorScheme(primary = Color(c.primary), onPrimary = …, primaryContainer = …, onPrimaryContainer = …, background = Color(0xFF141218), onBackground = Color(0xFFF2EDF2), surface = Color(c.surface), onSurface = Color(c.onSurface), surfaceVariant = Color(c.surfaceVariant), onSurfaceVariant = Color(c.onSurfaceVariant), border = Color(c.border), borderVariant = Color(0xFF49454F), scrim = Color.Black)`.
- `RadijasTheme(stationId, content)` → `MaterialTheme(colorScheme = stationScheme(stationId), shapes = RadijasShapes, typography = RadijasTypography, content = content)`.
- **Ištrinti** `heroSurface`, `heroGlowStops`, `tonalSurface`, `onBrand`, `MinTextContrast`, `linearize`, `relativeLuminance`, `contrastRatio` ir `RadijasBackground`/`RadijasContent` naudojimą UI (konstantas palikti tik `colors.xml` atitikmeniui, jei reikia `themes.xml`; jei niekas nenaudoja — ištrinti).

- [ ] **Step 3: UI consumes roles**

- `NowPlayingScreen`: fono `MaterialTheme.colorScheme.background`.
- `StationShelf`: kraštų fade spalva `background`.
- `StationTile`: `containerColor = surfaceVariant`, fokusuota `primaryContainer`; etiketė `onSurfaceVariant` / fokusuota `onPrimaryContainer`; rėmelis `border`; juosta `primary`; formos `MaterialTheme.shapes.extraLarge`.
- `HeroPanel`: paviršius `surface` (nuimti `clip`+`drawWithCache`? **ne** — clip lieka, brush keičiamas); švytėjimas iš `primaryContainer` (trys stopai: `primaryContainer` 0 %, 24 % alfa, 3,5 % alfa); tekstai `onSurface` (66 %/84 % alfa); `heroSurface`/`tonalSurface` nebeliko.
- `MainActivity`: `RadijasTheme(stationId = selectedStation.id)`.

- [ ] **Step 4: Run all tests**

Run: `./gradlew.bat testDebugUnitTest assembleDebug --offline`
Expected: PASS; `BrandPaletteTest` ištrintas; kiti testai žali.

- [ ] **Step 5: Screenshot check (emuliatorius)**

Paleisti AVD `Radijas_TV_36`, įdiegti APK, padaryti 3 kadrus (Lietus, ZIP FM, EHR) ir įrašyti į `docs/screenshots/2026-10-30/`. Jei hero ar plytelės per blankūs — reguliuoti **`primaryContainer` naudojimą**, ne grąžinti rankinį maišymą.

- [ ] **Step 6: Commit**

```bash
git add -A app/src docs/screenshots
git commit -m "feat: paint the TV UI from generated M3 roles"
```

---

### Task 3: Ekrano užgesinimas

**Files:**
- Create: `app/src/main/java/lt/radijas/tv/ScreenOffPolicy.kt`
- Create: `app/src/test/java/lt/radijas/tv/ScreenOffPolicyTest.kt`
- Modify: `MainActivity.kt`, `HeroPanel.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/lt/radijas/tv/ScreenOffOverlayTest.kt`, `HeroPanelTest.kt`

**Interfaces:**
- Produces: `internal enum class ScreenOffAction { PASS, CONSUME, CONSUME_AND_WAKE }`; `internal fun screenOffAction(action: Int, screenOff: Boolean): ScreenOffAction`; `@Composable fun ScreenOffOverlay(hintVisible: Boolean)`.

- [ ] **Step 1: Write the failing policy test**

```kotlin
class ScreenOffPolicyTest {
    @Test fun `while off every key is consumed and only UP wakes`() {
        assertEquals(CONSUME, screenOffAction(KeyEvent.ACTION_DOWN, true))
        assertEquals(CONSUME_AND_WAKE, screenOffAction(KeyEvent.ACTION_UP, true))
    }
    @Test fun `while on every key passes through`() {
        assertEquals(PASS, screenOffAction(KeyEvent.ACTION_DOWN, false))
        assertEquals(PASS, screenOffAction(KeyEvent.ACTION_UP, false))
    }
}
```
Run → FAIL (unresolved).

- [ ] **Step 2: Implement `ScreenOffPolicy.kt`**

`screenOffAction` = `when { !screenOff -> PASS; action == KeyEvent.ACTION_UP -> CONSUME_AND_WAKE; else -> CONSUME }`.

- [ ] **Step 3: Hero button (A1)**

`HeroPanel.kt`:
- `HeroPanel(..., onScreenOff: () -> Unit)`.
- Dešinė kolona: `Column { Image(size 136.dp); Spacer(8.dp); ScreenOffButton(onScreenOff) }`.
- `ScreenOffButton`: 136×48 dp `Box`, `MaterialTheme.shapes.medium`, `surfaceVariant`, tekstas „Ekranas" 16 sp `onSurfaceVariant`, `.focusable()`, `.onKeyEvent` (DPAD_CENTER/Enter, `repeatCount == 0`), `.semantics { role = Role.Button; onClick("Išjungti ekraną") { onScreenOff(); true } }`, fokuso rėmelis `border` 3 dp.
- `HeroPanelTest`: 168 dp assertas → 136 dp; naujas testas `up from the shelf focuses the screen button` (StationShelf + HeroPanel viename ekrane, `performKeyInput { pressKey(Key.DirectionUp) }`, `assertIsFocused` trūksta tag `screen-off-button`).

- [ ] **Step 4: Overlay + wake + keep screen on**

`ScreenOffOverlay(hintVisible)`: `Box(Modifier.fillMaxSize().background(Color.Black).testTag("screen-off-overlay"))`; jei `hintVisible` — centruotas `Text("Ekranas išjungtas. Paspausk bet kurį mygtuką.")`, `onSurface` 70 %.

`MainActivity.kt`:
- `private var screenOff by mutableStateOf(false)`; `private var hintVisible by mutableStateOf(false)`.
- `setContent` viduje: `DisposableEffect(screenOff) { if (screenOff) window.addFlags(FLAG_KEEP_SCREEN_ON) else window.clearFlags(FLAG_KEEP_SCREEN_ON); onDispose { window.clearFlags(FLAG_KEEP_SCREEN_ON) } }`; `LaunchedEffect(screenOff) { if (screenOff) { hintVisible = true; delay(2_500); hintVisible = false } }`.
- Turinys: `Box { NowPlayingScreen(...); if (screenOff) ScreenOffOverlay(hintVisible) }`.
- `onScreenOff = { screenOff = true }`.
- `override fun dispatchKeyEvent(event: KeyEvent): Boolean { when (screenOffAction(event.action, screenOff)) { PASS -> return super.dispatchKeyEvent(event); CONSUME -> return true; CONSUME_AND_WAKE -> screenOff = false; return true } }`.
- `onUserLeaveHint`: `screenOff = false` (be `stop()` pakeitimų).
- `onSaveInstanceState`/`onCreate`: `screenOff` atstatomas iš `savedInstanceState` (HDR/raiškos pakeitimui), bet ne per procesų mirtį.
- `strings.xml`: `screen_off_button` = „Ekranas", `screen_off_hint` = „Ekranas išjungtas. Paspausk bet kurį mygtuką.", `screen_off_action` = „Išjungti ekraną".

- [ ] **Step 5: Overlay test**

```kotlin
@Test fun `overlay is black and shows the hint`() {
    composeTestRule.setContent { RadijasTheme { ScreenOffOverlay(hintVisible = true) } }
    composeTestRule.onNodeWithTag("screen-off-overlay").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ekranas išjungtas. Paspausk bet kurį mygtuką.").assertIsDisplayed()
}
```

- [ ] **Step 6: Run all tests + emulator check**

`./gradlew.bat testDebugUnitTest assembleDebug --offline` → PASS.
Emuliatoriuje: įjungti stotį, paspausti „Ekranas", `logcat` turi rodyti `Playing` be pertraukimo; pirmas `KEYCODE_DPAD_RIGHT` grąžina ekraną ir neperjungia stoties; antras — perjungia. Įrašyti kadrą į `docs/screenshots/2026-10-30/09-screen-off.png`.

- [ ] **Step 7: Commit**

```bash
git add -A app/src docs/screenshots
git commit -m "feat: screen-off button that blacks the panel without stopping audio"
```

---

### Task 4: Dokumentai ir galutinė patikra

**Files:**
- Modify: `README.md`, `docs/STATUS.md`, `docs/superpowers/specs/2026-10-30-m3-system-and-screen-off-design.md` (§12 „Kas įgyvendinta"), `docs/superpowers/specs/2026-10-28-tv-radio-design.md` (§13)

- [ ] **Step 1: README** — pakeisti „paletė apskaičiuojama…" į „paletė generuojama HCT iš stoties spalvos"; pridėti ekrano užgesinimą į „Ką daro" (vienas sakinys + pulto lentelė: `↑` į „Ekranas", `OK` — užgesina, bet kuris klavišas — grąžina); atnaujinti testų skaičių; pašalinti pasenusius teiginius apie `tonalSurface`.
- [ ] **Step 2: STATUS** — „Padaryta" papildyti M3 sistema ir ekrano užgesinimu; „Liko" — realaus TV patikra (miego režimas, LED/OLED, pranešimo elgsena), release build.
- [ ] **Step 3: Galutinė patikra** — `./gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline` (3 komandos rezultatas įrašomas į STATUS) ir `assembleRelease` (R8, be parašo) kaip smoke test.
- [ ] **Step 4: Commit**

```bash
git add README.md docs
git commit -m "docs: describe generated palette and screen-off control"
```

---

## Savikontrolė (atlikta)

- **Spec padengimas:** §7.1–7.7 → 1–2 užduotys; §8.1–8.4 → 3 užduotis; §9 → testai 1 ir 3 užduotyse; §10 → užduočių eilė atitinka; §11 atviri klausimai sąmoningai neįgyvendinami (ikonos pasirinkimas, švytėjimo forma — palikta vykdytojui, kaip leidžia spec).
- **Tipų nuoseklumas:** `stationColors`/`StationColors` naudojami 1 → 2; `RadijasTheme(stationId)` → 2 → 3; `screenOffAction`/`ScreenOffAction` → 3.
- **Sąmoningai neskirta:** atskiras `seeds.json` (generatorius skaito `RadioRepository.kt`; dengimą saugo testas), `Card` migracija, judesys, `BackHandler` (dispatchKeyEvent sugeria Back; predictive back TV nenaudojamas), `tools/palette` versijų katalogo tvarkymas.
