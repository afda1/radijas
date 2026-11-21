# Radijas TV — Google TV / Pixel Ambient estetikos šlifavimas

2026-11-21

Ankstesni dizaino dokumentai: `2026-10-28-tv-radio-design.md` ir `2026-10-30-m3-system-and-screen-off-design.md`.

## 1. Tikslas ir kontekstas

Pašalinti stoties logotipo dubliavimą Hero kortelėje (Variantas A) ir išdirbti sąsają arčiau Google TV / Nest Hub estetikos, nepažeidžiant ankstesnių saugiklių: `SchemeTonalSpot` paletės, WCAG ≥ 4.5:1 tekstui ir ≥ 6:1 fokuso rėmeliui per visas 11 stočių, akimirksninio fokuso be animacijos vėlavimo, 5 % overscan, `fontScale = 1.3f`, jokių nustatymų meniu.

## 2. Apsvarstyti variantai (Hero dešinė)

| Variantas | Esmė | Sprendimas |
|---|---|---|
| **A. Be logotipo Hero bloke** | Logotipai gyvena tik karuselėje; viršus-dešinė atiduodamas laikrodžiui ir datai. | **Pasirinktas** |
| B. Logotipas kaip *watermark* | Pusiau skaidrus logotipas fone už laikrodžio. | Atmestas — tekstiniai logotipai (*GOLD FM*, *EHR*) kelia triukšmą fone |
| C. Sumažintas logotipas (~128 dp) | Paliekamas dubliavimas, sumažinus dydį. | Atmestas — neišsprendžia kartojimosi ir siaurina dainos eilutę |

## 3. Penki patvirtinti pakeitimai

1. **Hero viršus-dešinė (`HeroClock`):**
   * `52.sp` (`lineHeight = 56.sp`, `FontWeight.Medium`, `letterSpacing = (-1.5).sp`, `tnum`, `90 % onSurface`) laikrodis viršutiniame dešiniajame kampe.
   * Po juo (`6.dp` tarpas) dešinėje išlygiuota dviejų eilučių data (*Savaitės diena* / *mėnuo D d.*, `18.sp`, `lineHeight = 22.sp`, `FontWeight.Normal`, `74 % onSurface`).
2. **Hero paviršius, kairioji ašis ir ekvalaizeris (`HeroPanel`):**
   * `1.dp` toninis kortelės apvadas (`8 % onSurface`) ant `extraLarge` formos ir platesnis `44 % → 18 % → 0 %` `primaryContainer` švytėjimas (`HeroCoreX = 0.88f`, `HeroCoreY = 0.28f`, `HeroGlowRadiusX = 0.42f`, `HeroGlowRadiusY = 0.85f`).
   * Sutvarkyta *marquee* kairioji įtrauka: pašalintas kairysis padding, išlaikyta lygi ašis su stoties pavadinimu ir ekvalaizeriu, blukimas paliktas tik dešiniajame krašte (`28.dp`), kūrinio tekstas paryškintas iki `74 % onSurface`.
   * Lieknesnis `primary` spalvos ekvalaizeris (`28.dp × 22.dp`, `6.dp` stulpeliai su `11.dp` žingsniu).
3. **Google TV *pill* mygtukas (`ScreenOffControl`):**
   * `CircleShape` kapsulė be neigiamo `offset`; ramybėje `8 % onSurface` fonas su `1.dp` `10 % onSurface` apvadu ir `primary` ikona; fokusuotas — užpildytas `primary` su `onPrimary` tekstu ir ikona.
4. **Karuselė ir vieno fokuso taisyklė (`StationTile`, `NowPlayingScreen`):**
   * Išorinis `3.dp` rėmelis priklauso tik aktyviai fokusuotai plytelei; nefokusuota grojanti stotis žymima `6.dp` akcentiniu tašku ir `FontWeight.Medium`.
   * Nefokusuotos plytelės prigesinamos (`alpha = 0.88f`), o tarpas tarp Hero ir karuselės padidinamas `8.dp → 14.dp`.
5. **Nest Hub tamsusis režimas (`ScreenOffOverlay`):**
   * `80.sp` (`lineHeight = 86.sp`, `FontWeight.Normal`, `letterSpacing = (-1.5).sp`, `38 % onSurface`) laikrodis, po juo data (`30 % onSurface`) ir `24.dp` žemiau — kūrinio / stoties eilutė su subtiliu `6.dp` `primary` (`34 %`) akcentiniu taškeliu (`testTag("screen-off-playing-dot")`).
