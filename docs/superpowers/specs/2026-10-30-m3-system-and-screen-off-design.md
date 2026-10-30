# Radijas TV — M3 sistema ir ekrano užgesinimas

2026-10-30

Pirminis dizaino dokumentas: `docs/superpowers/specs/2026-10-28-tv-radio-design.md`. Šis dokumentas jo neperrašo — jis nustato, kas keičiasi dabar, ir kodėl.

## 1. Kodėl

Du prašymai, susiję su tuo pačiu ekranu, todėl daromi vienu etapu:

1. Dizainas turi būti pastatytas pagal Google Material 3 sistemą (spalvų rolės, tipografijos ir formų tokenai), o ne rankomis surašytais `dp`/`sp` ir vienkartine palete.
2. Reikia funkcijos: paspaudus **ekrane** esantį mygtuką, ekranas pajuosta per visą kadrą, o radijas toliau groja.

Abu keičia tą patį ekraną ir fokusavimo modelį, todėl antras valdiklis projektuojamas kartu su sistema, o ne priklijuojamas paskui.

## 2. Ką nusprendėme

**Dizaino kryptis.** Rinkomės „sistemos perkėlimą" (A), o ne „telefono Expressive kopiją" (C) ir ne „tik kodas be išvaizdos pokyčio" (B). TV kontekste telefono Expressive priemonės (maži taikiniai, ripple, banguotos formos, judesys visur) 3 m atstumu ir silpname TV box'e atrodo kaip gedimas; sistema (rolės, tokenai, paletė iš sėklos) yra tai, ką galima patikrinti automatiškai.

**Ekrano užgesinimas.** Bus juodas paveikslas per visą kadrą. Android TV neturi viešos API, kuria programa išjungtų panelę (`DEVICE_POWER` / device admin reikia). OLED televizoriuje tai praktiškai tikra tamsa; LED/QLED lieka foninis apšvietimas ir kambarys matys tamsų stačiakampį. Tai priimta riba.

## 3. Tikslai

- UI naudoja M3 spalvų roles, `MaterialTheme.shapes` ir `MaterialTheme.typography`; savavališki `dp`/`sp` lieka tik ten, kur tokeno nėra.
- Kiekviena stotis turi savo spalvų tapatybę, dabar iš **HCT toninės paletės, sugeneruotos iš stoties spalvos** (ne rankinis maišymas).
- Mygtukas ekrane užgesina ekraną (juodas kadras), garsas nesustoja; bet kuris pulto klavišas grąžina ekraną ir yra sugeriamas.
- Visi esami pažadai lieka: viena fokusuojama eilutė be vyniojimosi, jokio meniu, jokių nustatymų, kontrastas ≥ 4.5:1, fokuso rėmelis ≥ 6:1, saugi 5 % zona.

## 4. Ne tikslai

- `androidx.compose.material3` priklausomybė ir telefono komponentai (projektas jų neturi ir neprideda).
- Dinaminė spalva iš sistemos tapetų: **Android TV neturi tapetų**, Google TV gairės tiesiogiai sako, kad TV neskleidžia vartotojo schemų ir rekomenduoja turiniu pagrįstas schemas per Material Color Utilities. Mūsų paletė iš stoties spalvos yra būtent tai.
- `Carousel` vietoje `LazyRow` — turi savo slinkimo/fokuso animaciją, kuri prieštarauja „lentyna nejuda pati".
- Būsenų, tekstų, stočių sąrašo ar srautų keitimas.
- Tikra panelės išjungimas, miego laikmatis, užgesinimas pagal laiką.
- **M3 Expressive judesys ir M3 Expressive tipo skalė** (žr. §7.1).

## 5. Anti-tikslai

- Nors viena stotis praranda atpažįstamą atspalvį (Lietus tampa oranžinis).
- Nors vienas derinys nukrenta žemiau 4.5:1 (tekstas) arba 6:1 (fokuso rėmelis).
- Ekranas pajuosta netyčia, arba pabudus pirmas klavišas dar ir ką nors suveikia.
- Garso nutrūkimas dėl šių pakeitimų.

## 6. Kaip yra dabar (faktai iš šaltinio ir AAR)

- 14 iš 15 UI elementų statomi rankomis; iš `tv-material` naudojamas tik `Text`. Nėra `Card`, `Surface`, `Button`.
- `RadijasTheme` perduoda tik `colorScheme`; `shapes`/`typography` lieka bibliotekos numatytieji. Šešios rolės apibrėžtos ir **nė karto nepaskaitomos**.
- `tv-material` 1.1.0 `ColorScheme` rolės: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `inversePrimary`, `secondary…`, `tertiary…`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceTint`, `inverseSurface`, `inverseOnSurface`, `error…`, `border`, `borderVariant`, `scrim`. **`surfaceContainer*` rolių nėra** — todėl M3 konteinerių kopėčios šiame projekte neįgyvendinamos be rankinio kodo.
- Biblioteka neturi motion tokenų, `BorderDefaults`, `ListHeader`, Expressive komponentų.
- Bibliotekos numatytasis tamsus `surface` = `#1C1B1F`, `border` = `#938F99`; mūsų `#141218`/`#F2EDF2` jau tamsesnis ir kontrastingesnis už bibliotekos numatytąjį.

## 7. Sprendimas I — M3 sistema

### 7.1 Ką iš M3 Expressive imame, ko ne (su įrodymu)

Tyrimas atliktas oficialiu Google įrankiu `@material/material-color-utilities@0.4.0` (Apache-2.0), generuojant schemas iš 11 realių stoties spalvų.

**Atmetame `SchemeExpressive`:** šis variantas **sąmoningai sukioja atspalvį**. Išmatuota: ZIP FM `#1717E8` (283°) → 163° (žalia); Lietus `#009A61` (161°) → 41° (oranžinė); EHR `#E20E18` (25°) → 265° (mėlyna). Expressive specifikacija skirta tapetams, kur tikslus atspalvis nesvarbus; čia stoties spalva **yra** tapatybė. Tai būtų tiesioginis anti-tikslas.

**Atmetame M3 Expressive tipo skalę:** `displaySmall` 36 sp prieš dabartinį 96 sp vardą — 2,67× mažesnis aukštis (67 dp → 25 dp kapitelis). Prie 3 m tai vienareikšmiškai blogiau. TV gairės M3 skalę naudoja tokią pat kaip telefonas, bet mūsų hero jau viršija ją sąmoningai.

**Atmetame M3 state layers ir Expressive judesį:** nėra žymeklio, o TV gairės reikalauja „instant and distinct" fokuso atsako. Judesys čia yra silpnoji vieta, ne vertė.

**Imame:** toninę paletę iš sėklos, M3 roles, formų skalę (28 dp jau sutampa su `extraLarge`), tipografijos metriką (lineHeight/tracking) su dabartiniais dydžiais, ir fokuso rėmelio kontrasto pakėlimą.

**Paletės variantas: `SchemeTonalSpot`** (Android numatytasis, atspalvį išsaugo). Išmatuota 11 stočių, tamsi schema, contrast level 0, naudojant tik tas roles, kurias turi `tv-material`:

| Stotis | primary | primaryContainer | surface | surfaceVariant | label on surfaceVariant | onSurface on surface | onPrimaryContainer on primaryContainer |
|---|---|---|---|---|---|---|---|
| ZIP FM | `#bfc2ff` | `#3e4278` | `#131318` | `#46464f` | 5.48 | 14.33 | 7.22 |
| Power Hit Radio | `#ffb4ab` | `#73332e` | `#1a1110` | `#534341` | 5.51 | 14.34 | 7.25 |
| Lietus | `#92d5ac` | `#065232` | `#0f1511` | `#404942` | 5.49 | 14.33 | 7.21 |
| Gold FM | `#e7c26c` | `#5a4300` | `#17130b` | `#4d4639` | 5.48 | 14.33 | 7.27 |
| European Hit Radio | `#ffb4aa` | `#73342d` | `#1a1110` | `#534341` | 5.51 | 14.34 | 7.19 |

Minimumas per visas 11 stočių: **label/surfaceVariant 5.47**, **onSurface/surface 14.31**, **onPrimaryContainer/primaryContainer 7.19**, **primary/surface 10.86**. Visi virš 4.5:1; rėmelio 6:1 taip pat pasiekiama iš `primary`.

**Pasekmė, kurią reikia priimti sąmoningai:** `surfaceVariant` (tone 30) yra šviesus, beveik neutralus pilkas, o dabartiniai plytelių paviršiai yra 15 % brandinti. Perejus prie rolės plytelės taps pilkesnės, o stoties spalva labiau matysis `primary`, `primaryContainer`, `border` ir hero švytėjime. Tai „mažiau spalvos fone, daugiau tiksliai ten, kur ji ką nors reiškia". Jei realiame ekrane atrodys per blanku, sodrumas keliamas per `primaryContainer` naudojimą ir hero švytėjimą, o **ne** grįžtant prie rankinio `tonalSurface`.

### 7.2 Paletės generavimas

- Generatorius: `tools/palette/generate.mjs` — dev įrankis, **neįtraukiamas į APK**. Priklausomybė `@material/material-color-utilities@0.4.0` (Apache-2.0). Komanda: `node tools/palette/generate.mjs`.
- Rezultatas: `app/src/main/java/lt/radijas/tv/ui/theme/StationColorSchemes.kt` — 11 schemų, po 10 naudojamų rolių. Failas commit'inamas; runtime priklausomybių nėra.
- Generatorius deterministinis: pasikeitus stoties spalvai paleidžiamas iš naujo, o diff parodo tik pasikeitusias roles.
- Generuojamos rolės (tik tos, kurias naudojame ir kurias turi `tv-material`): `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `secondaryContainer`, `onSecondaryContainer`.
- `background`/`onBackground` lieka fiksuoti `#141218`/`#F2EDF2` (M3 tamsios bazės reikšmė; paleidimo `windowBackground` irgi).
- Sėklų šaltinis lieka `RadioStation.brandColor` (`RadioRepository.kt`) — net jei UI jo nebereikia, tai vienintelė autoritetinga vieta. Generatorius skaito `tools/palette/seeds.json` (id + hex), o **testas** tikrina, kad `seeds.json` sutampa su `RadioRepository.stations` ir kad kiekviena katalogo stotis turi sugeneruotą schemą. Nauja stotis be sėklos ar be schemos tada krenta teste, kaip dabar `BrandPaletteTest`.
- `border` generuojamas atskirai: `primary` šviesinamas link balto, kol pasiekiama **≥ 6:1 prie `surface` ir `primaryContainer`** ir **≥ 4.5:1 prie nefokusuotos plytelės** (pastaroji dabar šviesesnė dėl 8 % elevation tint, todėl 6:1 reikėtų beveik balto rėmelio ir stoties atspalvis dingtų). Rėmelio storis nesikeičia (3 dp) — storis kontrasto nesprendžia.
- `borderVariant` = `#49454F`, `scrim` = juoda 60 %.

### 7.3 Kur kuri rolė naudojama

| UI elementas | Rolė |
|---|---|
| Puslapio fonas | `background` |
| Hero paviršius | `surface` (stoties atspalvio beveik juodas), per jį — brando švytėjimas iš `primaryContainer` |
| Hero stoties vardas | `onSurface` |
| „Kas groja" | `onSurface` 66 % alfa — išmatuota **6.70:1** min. (prie `surfaceVariant` būtų 4.17:1, todėl hero bazei `surfaceVariant` netinka) |
| Būsena | `onSurface` 84 % alfa — išmatuota **10.27:1** min. |
| Plytelė (nefokusuota) | `primaryContainer` **savo stoties** paletės (ne grojamos stoties temos) |
| Plytelė (fokusuota) | ta pati `primaryContainer`; fokusą rodo `border` rėmelis ir 6 % mastelis |
| Plytelės etiketė | `onSurfaceVariant`; fokusuota — `onPrimaryContainer` |
| Fokuso rėmelis | `border` (≥ 6:1 prie visų trijų paviršių) |
| Ekvalaizerio brūkšneliai | `onSurface` |
| Mygtukas „Ekranas" | `surfaceVariant` + `primary` ikona |

### 7.4 Tema

- `RadijasTheme(stationScheme: ColorScheme, content)` → `androidx.tv.material3.MaterialTheme(colorScheme = stationScheme, shapes = RadijasShapes, typography = RadijasTypography)`. **Naudojama `tv-material` `ColorScheme`, o ne `compose.material3`** — `dynamicDarkColorScheme()` čia ne tik neveiksminga (TV neturi tapetų), bet ir netinkamo tipo.
- `RadijasShapes`: `extraSmall 4`, `small 8`, `medium 12`, `large 16`, `extraLarge 28`. Plytelė ir hero naudoja `extraLarge`; 20 dp logotipo dėžė neturi tokeno ir lieka.
- `RadijasTypography`: išlaikomi dabartiniai dydžiai **ir dabartinis tracking** (96/48 auto-fit su `letterSpacing = −1.5 sp`, 26/32, 24/30, 16/22). `−1.5` yra išmatuota reikšmė, kuria „Power Hit Radio Gold" ir „European Hit Radio" telpa 96 sp — M3 metrikos „−0.5" neimame, nes ji išplatintų raides ir abu vardai kryptų į auto-fit mažinimą. Iš M3 metrikos imamas tik `lineHeight` (hero 1,1×) ir etikečių `tracking +0.1 sp`. Vardo auto-fit išėjimas nesikeičia. `.copy(fontSize = …)` grandinės išnyksta, išskyrus vardo auto-fit.
- Trys „mirusios" rolės naudojamos arba ištrinamos. `RadijasBackground`/`RadijasContent` lieka tik XML `windowBackground` atitikmeniui.

### 7.5 Plytelė: sąmoningai NE `Card`

`Card` egzistuoja ir atrodytų kaip „labiau pagal gaires", bet AAR tyrimas parodė tris konkrečias problemas:

1. `CardDefaults.scale(...)` fokuso mastelį **animuoja** (300 ms įjungiant, 500 ms išjungiant). Mūsų plytelė sąmoningai reaguoja akimirksniu, o TV gairės reikalauja „instant and distinct feedback". Norint išlaikyti akimirksnį, tektų apeiti `Card` mastelį ir vėl rašyti savo `graphicsLayer` — tada `Card` nebeduoda nieko.
2. `Card` neturi `stateDescription` atitikmens; accessibility semantiką vis tiek reikėtų rašyti rankomis.
3. Numatytasis `Card` konteineris yra `surfaceVariant` (`#49454F`) — mūsų atveju jis per šviesus; vis tiek reikėtų perrašyti `CardDefaults.colors`.

Sprendimas: plytelė lieka savo komponentas, bet **spalvos, formos ir tipografika imamos iš temos rolių ir tokenų**. Taip „pagal gaires" yra sistemos lygmenyje, o ne varduose.

### 7.6 Fokusas ir judesys

- Fokuso mastelis 1,06× išlieka, **be animacijos** (TV gairės; ir tai yra viena iš dviejų vietų, kur judesys gali atrodyti kaip strigimas).
- Rėmelis 3 dp, spalva `border`, kontrastas prie `surfaceVariant` ir prie `primaryContainer` tikrinamas testu: **≥ 6:1** (ne 3:1, nes realiame prietaise stoties spalva prie savo toninio paviršiaus gali nukristi).
- Jokių kitų animacijų nepridedama.

### 7.7 Kas sąmoningai nepasikeis

- Viena fokusuojama eilutė, jokio vyniojimosi, `↑`/`↓` nejudina fokuso lentynoje (išskyrus naują mygtuką, §8.2).
- Hero vardo auto-fit ciklas 96 → 48 sp (išmatuotas maksimumas, ne skalės pakopa; tokenu netampa).
- Hero švytėjimo geometrija (`HeroCoreX/Y`, `HeroGlowRadiusX/Y`); keičiasi tik spalvų šaltinis.
- Kontrasto taisyklė, saugi 5 % zona, jokio meniu, jokių nustatymų.

## 8. Sprendimas II — ekrano užgesinimas

### 8.1 Ką tai daro

- Paspaudus mygtuką, virš viso turinio parodomas nepermatomas juodas sluoksnis (`Color.Black`), be teksto ir ikonų.
- Kol sluoksnis matomas, langui uždedamas `FLAG_KEEP_SCREEN_ON`: tai **išjungia Android TV Ambient Mode / screensaver**, todėl sistemos ekranas neuždengia mūsų juodo sluoksnio ir pirmas klavišas atkeliauja programėlei. Po pabudimo vėliava nuimama (ir `onDispose`).
- **Sprendimas:** ekranas laikomas įjungtu **visą screen-off sesiją**; energijos taupymo sąmoningai atsisakoma. Priešingu atveju televizorius po 10–20 min. užpieštų savo šviesų Ambient Mode — priešingai funkcijos tikslui, o jei kuri nors televizoriaus versija tokį režimą laikytų „išėjimu", suveiktų `onUserLeaveHint` ir radijas nutiltų. Grojimas nuo to nepriklauso.
- Grojimas nesustoja: `MainActivity` nenustoja veiklos, servisas ir `WAKE_MODE_NETWORK` lieka. Užgesinimas nesukelia `onUserLeaveHint` ar `onStop`, todėl „išėjus nutildome" taisyklė nesuveikia.
- Bet kuris pulto klavišas: ekranas grąžinamas, o **tas klavišas sugeriamas** (OK, Back, ← neveikia turinio).
- Pirmas 2,5 s rodoma blanki užuomina „Ekranas išjungtas. Paspausk bet kurį mygtuką."; po to — grynas juodas.
- Jei paspaudžiamas Home, galioja dabartinė taisyklė: garsas nutyla (Home niekada nepasiekia programėlės), o `onUserLeaveHint` nustato ekrano būseną atgal į įjungtą, kad grįžus nebūtų juodo ekrano.
- Būsena saugoma `rememberSaveable`, kad raiškos/HDR režimo pakeitimas jos nepamestų; **bet ne per procesų mirtį** — po procesų žūties grįžtama prie šviesaus ekrano, o ne įstrigusio juodo.

**Kodėl ne Ambient Mode.** Android TV screensaveris po ~10 min. pats gali užtemdyti ekraną ir leisti groti toliau, bet jis rodo savo turinį (laikrodį, ornamentą) ir įsijungia tik po neveiklumo. Mūsų tikslas kitas: žmogus pats nusprendžia, kada, ir mato gryną juodą. Todėl `FLAG_KEEP_SCREEN_ON` laikomas tik tol, kol sluoksnis matomas, o po pabudimo nedelsiant nuimamas — kad sistema vėl galėtų elgtis savaip.

### 8.2 Kur valdiklis (apsvarstyti variantai)

| Variantas | Privalumai | Trūkumai | Sprendimas |
|---|---|---|---|
| **A. Mygtukas hero kampe, pasiekiamas `↑`** | Matomas, arti, nereikia slinkti per 11 stočių; `↑` šiandien nieko nedaro | Atsiranda antra fokuso zona; `↑↓` nustoja būti „nieko"; netyčia paspaudus ekranas pajuosta | **Pasirinktas** |
| B. 12-a plytelė lentynos gale | Lieka viena fokuso zona | Reikia pereiti per visas 11 stočių; valdiklis sumišamas su stotimis; A–Z tvarka pažeidžiama | Atmestas |
| C. Ilgas OK paspaudimas (≥ 1 s) | Jokio naujo valdiklio | Ne „mygtukas ekrane"; neatskleidžiama; ilgas paspaudimas vyresniam nepažįstamas | Atmestas |

Tikslus išdėstymas — **pasirinktas A1, sprendimas savininko 2026-10-30**. Hero vidus yra ~202 dp, dešinėje buvo viena 168 dp logotipo juosta; 56 dp valdiklis prie jos netilpo (168 + 56 > 202, o horizontaliai 608 dp teksto + 168 dp logotipo = 776 dp, tarpo nėra).

Dešinė hero kolona tampa vertikaliu bloku:

```
[ logotipas 136 dp ]
[ 8 dp tarpas      ]
[ mygtukas 48 dp   ]   = 192 dp ≤ 202 dp
```

- Logotipas mažinamas 168 → 136 dp (19 %). Hero pavadinimo juosta ir teksto stulpelis nesikeičia.
- Mygtukas: 136 dp pločio `Surface`, 48 dp aukščio, ikona + tekstas „Ekranas" (16 sp), toks pat `border` rėmelis kaip plytelės (3 dp), tik fokusuotas rodo rėmelį.
- Fokusas: `↑` iš lentynos patenka į mygtuką, `↓` grąžina į tą pačią plytelę, kurioje buvo. Lentynoje ←/→ elgsena nesikeičia. OK ant mygtuko — ekranas pajuosta.
- `HeroPanelTest` 168 dp assertas keičiamas į 136 dp; pridedamas testas, kad mygtukas ir logotipas telpa 202 dp aukštyje.

Atmestas variantas B (paskutinė lentynos plytelė): būtų išsaugojęs vieną fokuso zoną, bet nuo įprastos stoties reikėtų ~9 paspaudimų, o valdiklis galėtų būti supainiotas su stotimi. Fokuso rėmelis ir spalvos abiem atvejais tos pačios.

### 8.3 Pabudimas

- `MainActivity.dispatchKeyEvent(event)`: jei `screenOff` — **sugeriami visi** to paspaudimo įvykiai (DOWN, pakartojimai, UP), o būsena grąžinama į įjungtą **UP momentu**. Negalima grąžinti būsenos DOWN momentu: tada tas pats paspaudimo UP jau patektų į turinį ir galėtų paleisti stotį.
- `BackHandler(enabled = screenOff)` papildomai padengia predictive back (`targetSdk 36`), nes Back gali ateiti ne per `dispatchKeyEvent`.
- `KEYCODE_HOME` negalima sučiupti: jis niekada nepristatomas programėlei, o nueina į launcher'į → `onUserLeaveHint` → ekrano būsena atstatoma.
- Garso ir vaizdo klavišai (Volume, Power) yra sistemos klavišai ir gali suveikti anksčiau už programėlę; „bet kuris klavišas" dokumentuose reiškia pulto navigacijos klavišus.
- Jei TV pats užgesina ekraną po savo timeout, pabudimą atlieka sistema; mūsų sluoksnis lieka ir pirmas klavišas jį nuima.

### 8.4 Tekstai

- `screen_off_button` = „Ekranas"
- `screen_off_hint` = „Ekranas išjungtas. Paspausk bet kurį mygtuką."

## 9. Rizikos ir kaip jas matuojame

| Rizika | Kaip tikriname |
|---|---|
| Kuri nors paletė nukrenta žemiau 4.5:1 / rėmelis žemiau 6:1 | Testas `StationPaletteTest`: 11 schemų × naudojamos poros; tekstas 4.5:1 (įskaitant 66 % ir 84 % kompozitus), `border` ≥ 6:1 prie `surface`, `surfaceVariant` ir `primaryContainer` |
| Plytelės ir hero tampa per neutralūs | 11 stočių screenshotai; sodrumas keliamas per `primaryContainer` ir hero švytėjimą |
| `border` šviesinimas iki 6:1 nuplauna brandą | Testuoti abi ribas (≥ 6:1 prie puslapio ir fokusuotos plytelės, ≥ 4.5:1 prie nefokusuotos); jei reikia daugiau — mažinti plytelės tintą, ne rėmelio storį |
| Logotipas 136 dp tampa sunkiai atpažįstamas | 11 stočių screenshotai; jei blogai, mygtukas perkeliamas į lentynos galą (variantas B) be kito dizaino keitimo |
| Ekranas pajuosta netyčia | Fokuso kelias tik per `↑`; emuliatoriuje tikrinama, kad iš lentynos `↑` niekur kitur neveda |
| Pabudimo klavišas dar ir suveikia | Testuojama dviem lygiais: (1) gryna `ScreenOffPolicy` (repo šablonas `StopPolicy`/`RelaunchPolicy`) — DOWN/UP pora: pirmas paspaudimas sugeriamas ir būsena grąžinama UP momentu; (2) Compose testas — juodas sluoksnis ir užuomina atsiranda/išnyksta. **Ne** kuriama `createAndroidComposeRule<MainActivity>()` infrastruktūra: `MainActivity.onCreate` paleidžia srautą, o vieno klavišo sugerimas realiame pultelyje patikrinamas įrenginyje. |
| `tv-material` `ColorScheme` neturi rolės, kurios reikia | Rolės sąrašas fiksuotas §7.2; jei trūksta — `tonalSurface` tik konkrečiam elementui, ne visai sistemai |
| R8/release elgsena | `assembleRelease` + įdiegimas į TV (§10, 6 etapas) |
| Google Play taisyklė TV-BA (audio-only neturėtų blokuoti Ambient Mode) | Sideload/F-Droid atveju negalioja. Jei kada eisime į Play — atskiras sprendimas prieš publikavimą, ne šio etapo dalis. |
| Ekranas pajuodsta per procesų mirtį ir lieka juodas | Būsena **nesaugoma** už `rememberSaveable`; po procesų žūties grįžtama į šviesų ekraną. |
| `FLAG_KEEP_SCREEN_ON` pamirštamas nuėmus sluoksnį | `DisposableEffect` + `onDispose` nuima vėliavą; testas tikrina, kad po pabudimo vėliavos nebėra. |

## 10. Apimtis ir eilė

Darbas vidutinis (ne vienas failas). Etapai, kiekvienas atskirai patikrinamas:

1. **Paletė:** generatorius `tools/palette/`, sugeneruotas `StationColorSchemes.kt`, `StationPaletteTest` (11 × poros, 4.5:1 ir 6:1).
2. **Tema:** `RadijasShapes`, `RadijasTypography`, `RadijasTheme(stationScheme)` iš `tv-material` rolių, mirusių rolių sutvarkymas, `BrandPaletteTest` perrašymas.
3. **Spalvos UI:** hero ir plytelės pereina prie rolių (komponentai lieka tie patys), grojančios plytelės juosta `primary`.
4. **Ekrano užgesinimas:** valdiklio vieta pagal §8.2 sprendimą, `dispatchKeyEvent`, juodas sluoksnis, užuomina, tekstai, `ScreenOffPolicy` testas + Compose sluoksnio testas; klavišo sugerimas — įrenginyje.
5. **Kontrasto rėmelis:** `border` ≥ 6:1 visoms 11; testo išplėtimas.
6. **Dokumentai ir release:** README/spec atnaujinimas, `assembleRelease` smoke test, realus TV.

## 11. Ką palieku kūrėjui

- Ar plytelės `surfaceVariant` turi būti šiek tiek brandintas (kompromisas tarp „pagal rolę" ir tapatybės); numatyta — ne, brandas tik `primary`/`primaryContainer`/`border`/hero švytėjime.
- Mygtuko ikona (mėnulis / ekranas) — tekstas „Ekranas" jau nuspręstas.
- Užuominos trukmė (2,5 s) ir ar ji reikalinga.
- Ar hero švytėjimas turi likti radialinis, ar tapti plokštesniu `surfaceTint` sluoksniu.

## 12. Įgyvendinimo pastabos (2026-10-30)

- Paletė: `SchemeTonalSpot`, generuojama `tools/palette/generate.mjs` → `ui/theme/StationPalette.kt`; runtime priklausomybių nėra.
- Hero fonas `surface`, plytelės `surfaceVariant`, fokusuota plytelė `primaryContainer`, rėmelis `border` (≥ 6:1 prie trijų paviršių, šviesinama ≤ 18 %).
- Plytelė liko savas komponentas (be `Card`), fokusas akimirksnis, jokio judesio.
- Logotipas 168 → 136 dp, po juo 136×48 dp mygtukas „Ekranas"; fokusas į jį patenka `↑`, grįžta `↓`.
- Mygtukas suveikia **KeyUp**, ne KeyDown: kitaip tas pats paspaudimas pajuodintų ir iškart pažadintų ekraną (rasta emuliatoriuje).
- `dispatchKeyEvent` su `@SuppressLint("RestrictedApi")` — `onKeyDown` netinka, nes rodykles suvartoja Compose fokusas.
- Ekrano būsena saugoma `onSaveInstanceState` (išgyvena raiškos/HDR pakeitimą), bet ne procesų mirtį.
- **Kiekviena plytelė ima savo stoties schemą** (`stationScheme(station.id)`), o ne `MaterialTheme` (ta yra grojamos stoties). Be to kiekviena plytelė būtų vienodos spalvos — būtent ta klaida ir buvo padaryta pirminėje M3 versijoje.
- Plytelių etiketė `onPrimaryContainer` ant `primaryContainer` = 7.19:1; rėmelis prie `primaryContainer` = 6.0:1.
- **Logotipų optinė normalizacija:** `ui/theme/StationLogoScale.kt` + `StationLogoScaleTest`; dydis nustatomas pagal rašalo plotą (~4200 dp²), ne pagal `ContentScale.Fit`. Wordmark'ai lieka 1,0, kompaktūs ženklai 0,71–0,93.
- **Hallmark audito restyle:** hierarchija atstatyta pagal naudotojo žvilgsnį — vardas 64 sp (buvo 96), daina 32 sp (buvo 26), būsena 28 sp (buvo 24), plytelės etiketė 18 sp, plytelė 200 dp; tipas skaitomas iš `RadijasTypography`. „Ekranas" nefokusuotas — tik glyph, fokusuotas — piliulė su užrašu. `ScreenOffBlack` tokenas juodam kadrui.
- **Grojimo juosta pašalinta (2026-10-30, savininko prašymu):** plytelės žymimos tik fokuso rėmeliu (3 dp `border`) ir 6 % masteliu; jokio papildomo žymėjimo ant grojančios plytelės. „Groja" liko tik accessibility `stateDescription`.
- **Fokuso grįžimas iš „Ekranas" (2026-10-30):** `ui/ShelfFocusState.kt` + `StationShelf` atsimena, kurią plytelę D-pad paliko, ir `↓` grąžina į ją (su `scrollToItem` + `requestFocus`), o ne į geometriškai artimiausią plytelę po valdikliu. Testas: `HeroPanelTest` „down from the screen-off control returns to the station that was left".
