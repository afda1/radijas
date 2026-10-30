# 2026-10-30 pakeitimų patikra

## Naujausias sprendimas: viršelių atsisakyta

Vartotojo prašymu albumų viršeliai pašalinti visiškai: visada rodomas stoties
logotipas. Pašalinti `AlbumArtwork`, paveikslėlio parametras ir šaka hero bloke,
Deezer paieška, atitikimo taisyklės, bitmap atsisiuntimas ir talpykla.
Liko tik siauras `PowerMetadataClient` oficialiems Power dainų duomenims;
PCM vizualizacija, EHR ir A–Z nepakeisti. Papildomų priklausomybių nebuvo.

- RED: `testDebugUnitTest --tests lt.radijas.tv.PowerMetadataTest` sustojo tik ties
  dar nesukurta `PowerMetadataClient` klase, kurią naujas testas kviečia kitoms stotims.
- GREEN: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`,
  Java 21 — **BUILD SUCCESSFUL**, 36 s. XML: **78 testai, 0 klaidų**, lint —
  **0 klaidų / 31 įspėjimas**, APK atnaujintas.
- Iš 80 testų pašalinti 3 nebenaudojamos viršelių funkcijos testai, pridėtas testas,
  kad Power klientas nesikreipia dėl kitų stočių. Du UI testai dabar tikrina, kad
  logotipas lieka matomas ir tokio pat dydžio keičiantis dainai bei visoms būsenoms.
- Ankstesnis screenshotas su albumo viršeliu pašalintas; README rodo logotipą.
  Kiti screenshotai yra ankstesnės patikros, ne naujas emuliatoriaus paleidimas.
- Commit / push neatlikti. Padengimo procentas ir tikro tinklo nutrūkimo scenarijus
  neišmatuoti; ankstesni prototipo įrodymai apačioje yra istorija, ne dabartinė funkcija.

Savivertinimo atnaujinimas: tikslumas 4/5 (78 testai ir lint, be naujo emuliatoriaus
kadro), pilnumas 4/5 (pašalintas visas viršelių kelias, transporto atšaukimo testų dar
nėra), aiškumas 4/5 (README ir istorija atskirti, senas fallback failo pavadinimas
liko), pritaikomumas 4/5 (APK yra, viešas leidimas neparuoštas), glaustumas 4/5
(nebeliko paieškos ir talpyklos, istorinis patikros dokumentas išlieka ilgas).
Vidurkis 4,0/5; toliau vertingiausia patikimai išbandyti tinklo nutrūkimą realiame TV.
Vartotojo galutinis dizaino įvertinimas dar negautas.

## Ankstesnio prototipo istorija (viršeliai nebegalioja)

Vartotojo patvirtinta kryptis: A–Z stotys, paskutinė klausyta stotis paleidus,
minimalus toninis TV dizainas, sklandi karuselės riba, albumo viršelis vietoje
hero logotipo (nepavykus — logotipas) ir subtili tikro garso vizualizacija.
Jokių naujų ekranų ar valdiklių.

## Šaltiniai ir ribos

- Oficialus Power Hit Radio grotuvas naudoja tuos pačius Revma URL kaip programa.
  Svetainės kodas dainą pasiima atskirai per `/Pwr/lastSong/` ir `?gold=1`.
  2026-10-30 abu HTTPS JSON adresai grąžino `artistName`, `songTitle`, `date`.
  Tai viešos svetainės vidinis adresas, ne garantuotai versijuojama API.
- Deezer vieša paieška grąžino „The Killers — Spaceman“ ir albumo `cover_big`.
  Katalogas nėra išsamus: KATSEYE „Animal“ paieška tą dieną buvo tuščia.
  Saugus rezultatas tokiais atvejais yra stoties logotipas, ne panaši daina.
- Viršelių atvaizdavimo ir tiekėjo sąlygas būtina peržiūrėti prieš viešą leidimą;
  viešas URL savaime nėra autorinių teisių licencija. Viršeliai neįtraukiami į repo.
- Media3 1.11.0 `TeeAudioProcessor.AudioBufferSink` leidžia matuoti PCM srautą
  jo neįrašant, nekeičiant ir neprašant mikrofono leidimo. Tai garso lygis,
  ne dažnių spektro analizė; garso išvesties buferis gali lemti nedidelį vėlavimą.

## Testų garantijos

| Elgsena | Testas | Būsena |
|---|---|---|
| A–Z katalogas | `StationCatalogTest` | PASS (Java 21 vykdymas) |
| Išsaugota stotis nepriklauso nuo sąrašo eilės | `StartupStationTest` | PASS (Java 21 vykdymas) |
| Švieži Power metaduomenys, pasenusių / blogų duomenų atmetimas | `TrackLookupTest` | PASS (Java 21 vykdymas) |
| Tikslus atlikėjo ir pilno pavadinimo atitikimas, HTTPS paveikslėlių hostas | `TrackLookupTest` | PASS (Java 21 vykdymas) |
| Viršelis arba logotipas toje pačioje vietoje | `NowPlayingScreenTest` | PASS (galutinis visas rinkinys) |
| PCM tyla, amplitudė, nepakeistas buferis | `PcmLevelMeterTest` | PASS |

## Vykdymo įrodymai

- Pradinis `gradlew.bat testDebugUnitTest --tests lt.radijas.tv.StationCatalogTest`
  sustojo ties trūkstamomis SDK licencijomis. Tai **ne RED įrodymas**.
- JDK 17 ir Android SDK platforma 37.0 / build-tools 36.0.0 įdiegti.
- Nauji testai parašyti prieš naujos elgsenos realizaciją; galutinis skaičius — 80.
- Pakartotinis `testDebugUnitTest` po testų importų pataisymo sustojo tik ties
  dar neegzistuojančia funkcija `albumArt`, `PcmLevelMeter`, `SongQuery`,
  `parsePowerTrack`, `selectCoverUrl` — compile-time RED naujai funkcijai.
- `gradlew.bat assembleDebug` su JDK 17: **BUILD SUCCESSFUL**.
- Pirmas runtime testų vykdymas: 44 rezultatai, 11 klasių nepasileido, nes
  Robolectric Android 36 reikalauja Java 21. Tai aplinkos klaida, ne GREEN.
- Emuliatoriuje Android TV 36 (1920×1080, 320 dpi) APK įdiegtas ir paleistas.
  Lietus grojo, gautas ICY tekstas ir tikras Rondo albumo viršelis. Pirmas
  screenshotas atskleidė stačiakampę toninio švytėjimo ribą — taisoma prieš
  galutinius screenshotus. Vaizdų katalogas: `docs/screenshots/2026-10-30/`.
- Git commit ir push neatlikti: vartotojas nori pirmiausia testuoti.
- Pirmas Java 21 viso rinkinio vykdymas: **80 testų, 1 klaida** — testinis
  `ImageBitmap(16, 16)` nulūžo Robolectric legacy bitmap paskirstymo shadow viduje.
  Vietoje to testas dekoduoja egzistuojantį PNG fixture ir tą patį bitmap perduoda
  per albumo viršelio šaką; elgsenos assertai nepašalinti ir nesusilpninti.
- `testDebugUnitTest --tests lt.radijas.tv.NowPlayingScreenTest` su Java 21 po
  fixture pataisos: **BUILD SUCCESSFUL**, abu UI testai praeina.
- Visas Java 21 rinkinys po fixture pataisos: **80 testų praeina**, APK susirenka.
  Tame pačiame vykdyme `lintDebug` rado **19 klaidų ir 31 įspėjimą** esamame Media3
  kode: 2 `WrongConstant` ir 17 `UnsafeOptInUsageError`. Be baseline / taisyklių
  išjungimo pakeistos dvi klaidų konstantos į `SessionError.ERROR_BAD_VALUE` ir
  pridėtas aiškus `@OptIn(UnstableApi::class)` politikai bei callback klasei.
  Pakartojus tą patį visą patikros rinkinį: **BUILD SUCCESSFUL** (2 min. 28 s).

### Galutiniai rezultatai

Komanda: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`, JDK 21.

- Testų XML: **80 testų, 0 failures, 0 errors**.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` — surinktas.
- Android Lint: **0 klaidų, 31 įspėjimas**. Daugiausia naujesnės bibliotekų versijos,
  TV manifesto / eksportuoto medijos serviso ir Compose stiliaus rekomendacijos.
  Priklausomybės automatiškai neatnaujintos; neribota scope migracija nedaryta.
- `git diff --check`: švarus. Commit ir push neatlikti.
- 6 tikri screenshotai su indeksu: `docs/screenshots/2026-10-30/README.md`.
- Testinis proxy pašalintas, emuliatoriaus tinklo greitis atkurtas; emuliatorius
  po patikros išjungtas, kad nenaudotų kompiuterio resursų.

## Emuliatoriaus patikra

- `adb install -r` ir `am start`: sėkmingi. Emuliatorius Android TV 36,
  1920×1080, 320 dpi, Windows WHPX.
- A–Z pradžia EHR ir pabaiga ZIP FM pasiekiamos D-pad; abiejuose kraštuose
  matomas visas padidintas fokuso rėmelis. Ilgi stočių pavadinimai hero telpa.
- Abi Power stotys grojo ir parodė oficialios svetainės dainos tekstą.
- Lietus parodė Rondo, vėliau Taylor Swift viršelius. „ORAI“ metu liko stoties ženklas.
- Home → paleidimas iš naujo: loguose vėl paleista paskutinė ZIP FM, ne A–Z pirmoji EHR.
- Vizualizacija skirtinguose kadruose turi skirtingus lygius; PCM tikrinamas ir unit testais.
  Garso / vaizdo vėlavimas fiziniame TV dar nematuotas.
- Bandymas išjungti tik emuliatoriaus Wi-Fi ir riboti konsolės tinklo greitį
  **neatjungė Android TV Ethernet**; tai nelaikoma ryšio dingimo įrodymu.
  Wi-Fi ir greitis grąžinti. Nepasiekiamo HTTP proxy bandymas taip pat nenutraukė
  jau grojančio garso; proxy įrašas pašalintas. „Nėra ryšio“ emuliatoriaus
  screenshotas šioje iteracijoje **nepatvirtintas**, nes tinklas nebuvo patikimai izoliuotas.
- `testDebugUnitTest --tests lt.radijas.tv.PcmLevelMeterTest --tests lt.radijas.tv.RelaunchPolicyTest`
  su Java 17: **BUILD SUCCESSFUL**, 7 testai. Visas Java 21 rinkinys vėliau taip pat praėjo.

Realaus TV patikra būtina: D-pad fokusas, karuselės kraštai, garsas perjungiant,
vizualizacijos vėlavimas, viršelio ir garso atitikimas (interneto metaduomenys
gali aplenkti garso buferį). Automatinis padengimo procentas dar neišmatuotas.

## Savikritika (ne nepriklausoma peržiūra)

Rezultatas tinkamas tolesniam vietiniam bandymui; tai nėra viešo leidimo patvirtinimas.

| Kriterijus | Balas | Įrodymas ir konkretus tobulinimas |
|---|---|---|
| Tikslumas | 4/5 | APK ir tikri screenshotai; viršeliai tik pagal tekstą, ne garso atpažinimą. Toliau tikrinti bendrų atlikėjų / versijų atitikimą. |
| Pilnumas | 4/5 | EHR, A–Z, paskutinė stotis, Power JSON, viršelis su atsarga, PCM ir karuselė realizuoti. Tinklo dingimo gyvas vaizdas nepatvirtintas; nėra HTTP atšaukimo / perjungimo race testų. |
| Aiškumas | 4/5 | README aprašo išorines užklausas, screenshotų indeksas atskiria gyvus duomenis nuo garantijų. Tikslaus atitikimo konservatyvumas vis dar gali nustebinti vartotoją, nes logotipas lieka dažniau. |
| Pritaikomumas | 4/5 | Yra debug APK, JDK 21 / SDK ir vykdymo komandos. Viešam platinimui dar reikia tiekėjo sąlygų ir įprasto release patvirtinimo. |
| Glaustumas | 4/5 | Nėra naujų bibliotekų, ekranų ar pasirinkimų; HTTP, parseris ir PCM atskirti. Patikros chronologiją ateityje galima sutrumpinti iki galutinių rezultatų lentelės. |

Vidurkis: **4,0/5**. Prioritetai: (1) deterministiniai transporto klaidų ir greito
stoties perjungimo testai, (2) fizinio TV garso / vaizdo sinchroniškumas,
(3) viršelių tiekėjo sąlygų peržiūra prieš viešą platinimą.
Ar vartotojas sutiktų? Tikėtina dėl matomų screenshotų ir veikiančio APK, tačiau
dizaino patvirtinimas ir galutinis sprendimas priklauso vartotojui.
