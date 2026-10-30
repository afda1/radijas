# Būsena

Atnaujinta: 2026-10-30 (paskutinė testų ir lint patikra — 17:39)

Šis failas skirtas tam, kas tęsia darbus po pertraukos arba kitoje mašinoje. README pasakoja, kas tai per programa. Čia surašyta, kas padaryta, kas liko ir kokie sprendimai dar nepriimti.

## Padaryta

- TV-only programėlė: `LEANBACK_LAUNCHER`, baneris, jokios telefono versijos.
- Automatinis grojimas paleidus: paskutinė stotis, pirmą kartą Lietus.
- Viena fokusuojama zona, jokio vyniojimosi, ↑↓ nieko nedaro.
- Penkios būsenos ir jų tekstai, įskaitant klaidą po 30 sekundžių ir „Pristabdyta" po audio focus.
- Būsenos tiesos pataisymai (2026-10-30): „Gyvai" rodoma tik tada, kai grotuvas girdimas (`isPlaying`), o ne pagal `playWhenReady` (`player/PlayerStateMapping.kt`, `player/RadioPlayerManager.kt`); OK ant grojančios stoties nieko nedaro ir nebeperkrauna srauto (`StationSelection.kt`); sustabdytas grotuvas rodo „Pristabdyta", o ne „Jungiamasi…" (`ui/PlayerStatus.kt`).
- „Kas groja" iš ICY meta, fiksuoto aukščio eilutė, jokio slinkimo.
- Paletė iš stoties ženklo spalvos, kontrastas tikrinamas automatiškai visoms vienuolikai stočių.
- Logotipai balti siluetai, apkirpti, sugeneruoti iš `assets/brand`.
- Pranešime tik „Stabdyti", išoriniams valdikliams atimtos rašymo komandos.
- Vienuolikta stotis: European Hit Radio (`ehr`, `https://stream.ehr.lt:8443/ehr`), ženklas iš oficialaus balto logotipo, spalva `#E20E18` iš to paties logotipo.
- Grįžimas į programėlę nebeperkrauna srauto, jei tas pats kanalas dar groja (`RelaunchPolicy`), todėl nereikalingo buferiavimo ir tuščios „Kas groja" eilutės nebelieka.
- A–Z katalogas; paleidimo stotis ir toliau nustatoma pagal išsaugotą ID, ne sąrašo vietą.
- Power Hit Radio / Gold papildomi duomenys iš oficialios svetainės. Vartotojo sprendimu albumų viršeliai visiškai pašalinti; hero ir karuselėje visada stoties logotipas. Paieškos, atsisiuntimo ir talpyklos kodo neliko.
- Trijų PCM lygių vizualizacija be mikrofono leidimo. Karuselės viewport per visą plotį, saugi paraštė turinyje ir švelnūs kraštai; pašalintas papildomas juodas logotipo kvadratas.
- Hero UI atnaujintas (2026-10-30): pašalintas „Radijas" užrašas, būsenos eilutė padidinta iki 24 sp (`SemiBold`), ekvalaizerio brūkšneliai pervesti po būsenos teksto (`ui/HeroPanel.kt`).
- Accessibility: stoties plytelė semantikoje yra `Role.Button` su „Rinktis stotį" veiksmu, o grojanti plytelė skelbia „Groja" ir turi atskirą ženklą (`ui/StationTile.kt`, `res/values/strings.xml`). Testai — `StationShelfTest`.
- **M3 sistema (2026-10-30):** paletė nebėra rankinis maišymas. `tools/palette/generate.mjs` (dev įrankis, oficialus Google `material-color-utilities`, neįtraukiamas į APK) iš 11 stoties spalvų sugeneruoja `ui/theme/StationPalette.kt`; `RadijasTheme` perduoda `colorScheme`, `shapes` ir `typography`. UI spalvos imamos iš `MaterialTheme.colorScheme` rolių; `tonalSurface`/`onBrand`/`heroSurface` ištrinti, `BrandPaletteTest` pakeistas `StationPaletteTest` (11 × poros, tekstas 4.5:1, rėmelis 6:1).
- **Ekrano užgesinimas (2026-10-30):** hero dešinėje, po logotipu (136 dp), mygtukas „Ekranas". Paspaudus rodomas juodas sluoksnis, garsas nesustoja; `FLAG_KEEP_SCREEN_ON` tik kol sluoksnis matomas. Bet kuris pulto klavišas grąžina ekraną ir yra sugeriamas (`dispatchKeyEvent`, pabudinama UP momentu, kad tas pats paspaudimas neįjungtų stoties). Būsena išgyvena raiškos pakeitimą, bet ne procesų mirtį. Patikrinta emuliatoriuje: garsas nenutrūko, pabudimo klavišas stoties nepakeitė.
- **Release build (2026-10-30, pirmą kartą):** `assembleRelease` su R8 ir resursų suspaudimu sukompiliuotas → `app/src/../build/outputs/apk/release/app-release-unsigned.apk` (2,21 MB, nepasirašytas). Pirmą kartą reikia interneto (`compose-group-mapping` nėra Gradle cache) ir `--no-configuration-cache`, nes AGP 9.2 Compose mapping task'as neserializuojamas configuration cache. Tai release build patikra, ne pasirašytas leidimas.
- **Logotipų optinė normalizacija (2026-10-30):** stoties ženklas nebėra tempiasi į 156 dp kvadratą. `ui/theme/StationLogoScale.kt` laiko vieno ženklo pločio dalį; wordmark'ai (EHR, Gold FM, Lietus, Relax, Rock, ZIP) lieka per visą plotį, o tankūs kompaktiški ženklai mažinami (M-1 Plius 0,71, M-1 0,80, PHR Gold 0,83, RC 0,84, Power Hit 0,93), kol jų „rašalo" plotas siekia ~4200 dp². `StationLogoScaleTest` perskaičiuoja tai iš PNG, tad lentelė negali savaime išsiderinti.
- **Hallmark audito restyle (2026-10-30):** tipografinė skalė perkelta į temą ir apversta hierarchija ištaisyta — stoties vardas 96→64 sp, daina 26→32 sp, būsena 24→28 sp, plytelės etiketė 16→18 sp, plytelė 176→200 dp; „Ekranas" nefokusuotas nebėra piliulė (tik glyph), o fokusuotas rodo užrašą. Juodas ekrano kadras gavo `ScreenOffBlack` tokeną. Pridėtas kontrolės glyph'o kontrasto testas (4.67:1). Įrašyta dizaino dokumente §12.
- **Plytelių žymėjimas ir fokuso grįžimas (2026-10-30):** pašalinta grojimo juosta nuo plytelės (lieka fokuso rėmelis); „Ekranas" → atgal `↓` grąžina į tą pačią plytelę, kurią palikai (`ShelfFocusState` + scroll + focus; testas `HeroPanelTest`).
- Testų būsena (2026-10-30 18:25): **99 testai, 0 klaidų** (`app/build/test-results/testDebugUnitTest`), lint **0 klaidų / 31 įspėjimas**. Iš jų 18 nauji: `PlayerStateMappingTest` (8), `StationSelectionTest` (6), `PlayerStatusTest` (+2), `StationShelfTest` (+2 accessibility). Ankstesnis 78 testų skaičius yra to paties dokumento istorija (žr. `docs/2026-10-30-radio-update.md` įžangą).
- Po viršelių pašalinimo Java 21 patikra: 78 testai praeina, debug APK susirenka, lint — 0 klaidų / 31 įspėjimas (to meto skaičius; dabar — 96, žr. aukščiau). Išsamūs įrodymai ir screenshotai — `docs/2026-10-30-radio-update.md` ir `docs/screenshots/2026-10-30/`.
- README, ekrano nuotraukos, logotipas, TV baneris, AI autorystės disclosure.
- Išvalyta prieš išleidimą: pašalintas ekvalaizerio kodas, nenaudojami piešiniai, `Coil`, `material3`, `viewmodel-compose`, `roborazzi`, instrumentation testų priklausomybės ir visi `Fogwave` pavadinimai kode (`RadijasPlaybackService`, `Theme.Radijas`, `radijas_prefs`).

## Liko iki išleidimo

1. **Įrašyti į realų televizorių ir patikrinti.** Paskutiniai pakeitimai (fokusas paleidžiant, „Jungiamasi…", Power Hit Radio Gold ženklas) buvo patikrinti tik emuliatoriuje.
2. **Patikrinti, ar pranešimo „Stabdyti" pasiekiamas televizoriuje.** Atidarius pranešimų panelę, `onUserLeaveHint` gali sustabdyti grojimą ir pašalinti pranešimą anksčiau, nei žmogus spės jį paspausti. Tai vienintelis nebaigtas techninis klausimas.
3. **Pasirašytas release APK.** Reikia keystore ir `./gradlew assembleRelease`. Keystore į git nepatenka (`.gitignore` jau turi `*.jks`).
4. **Repo padaryti viešą** ir įkelti APK į GitHub Releases.
5. **Parašyti Fogwave autoriui** (jų Gitea, ne GitHub), kai repo bus viešas. Tekstas paruoštas, bet dar neišsiųstas.

## Atviri sprendimai

- **Miego laikmatis.** Radijas groja ir užgesus ekranui, todėl gali groti visą naktį. Jei taip yra nutikę, tai vienintelė funkcija, kurią verta pridėti. Jei ne, nepridedama nieko.
- **Ar balsu paleisti stotį.** „Ok Google, paleisk Lietus" būtų nulinė UI kaina, bet reikalauja atidaryti dalį sesijos komandų Assistantui ir patikrinti, ar dėžutėje jis veikia.
- **„Nėra interneto" atskirti nuo „stotis neveikia".** Viena teksto eilutė, tas pats ekranas.

## Žinomi apribojimai

- Power Hit Radio / Gold svetainės JSON nėra garantuotai versijuojama API; nepavykus gauti šviežių metaduomenų tekstas tuščias. Interneto metaduomenys gali nesutapti su keliomis sekundėmis vėluojančiu garso buferiu.
- Stotys įrašytos kode. Pasikeitus srauto adresui reikia naujos versijos.
- Išbandyta viename realiame įrenginyje (Telia DV8919X-LT, Android 14) ir emuliatoriuje. Kituose televizoriuose `onUserLeaveHint` ir screensaverio elgsena gali skirtis.
- Paletės kontrasto testas pririštas prie 960 dp pločio; labai dideliuose ekranuose ilgas dainos pavadinimas teoriškai gali nukristi žemiau 4.5:1.
- Lentynos vertikali paraštė padidinta iki 8 dp, kad 6 % fokuso padidinimas nebūtų nukerpamas.
- Būsenų išdėstymo „nešokinėjimo" garantija galioja 1.0 šrifto masteliui; didesnis sistemos `fontScale` gali padidinti fiksuoto aukščio juostas (`ui/HeroPanel.kt`).

## Kaip tęsti su agentu

Agentui užtenka trijų failų: `README.md`, `docs/superpowers/specs/2026-10-28-tv-radio-design.md` ir šio. Pavyzdinis prašymas:

> Perskaityk README.md, docs/superpowers/specs/2026-10-28-tv-radio-design.md ir docs/STATUS.md. Tai Android TV radijo programėlė. Noriu padaryti [X]. Nekeisk to, kas sąraše „Padaryta", be mano sutikimo, ir laikykis taisyklės, kad nauja funkcija negali pridėti ekrano ar pasirinkimo.

Jei dirbama su tuo pačiu repo, prieš pradedant verta paleisti `./gradlew testDebugUnitTest`, kad būtų aišku, jog išeities taškas sveikas.
