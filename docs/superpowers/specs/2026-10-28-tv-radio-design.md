# Radijas TV — dizaino dokumentas

2026-10-28

## 1. Kodėl

Norim Android TV programėlės, kuri įsijungia ir groja radiją. Be meniu, be sąskaitų, be reklamų, be nustatymų. Pagrindiniai naudotojai — du vyresnio amžiaus žmonės, kurie šiuo metu radiją per televizorių įsijungti gali tik su pagalba.

Sėkmė atrodo taip: tėvas paima pultą, randa „Radijas“ televizoriaus meniu, paspaudžia vieną kartą — ir po kelių sekundžių groja Lietus. Be jokių paaiškinimų.

Nesėkmė (net jei techniškai viskas veikia): reikia daugiau nei dviejų paspaudimų iki garso; ekrane yra būsena be matomo fokuso; atsiranda bet koks nustatymų ekranas; programėlė tyliai groja fone, kai žmogus jau žiūri ką nors kitą.

## 2. Kam

- Pirma — mano tėvams, mano televizoriuje.
- Jei pavyksta — nemokamai visiems, per F-Droid arba tiesioginį APK.
- Nieko kito: ne „visiems išmanantys“, ne jaunimui, ne telefonams.

## 3. Tikslai

1. Vienas paspaudimas nuo paleidimo iki garso.
2. Visas appsas valdomas keturiais pulto mygtukais: ← → OK Back.
3. Niekada nebūna ekrano, kuriame nematytų, kur yra fokusas.
4. Veikia be interneto praradimo baimės: nutrūkus srautui — automatinis sugrįžimas.
5. Jokių tinklaraščių, analitikos, reklamų, registracijų.

## 4. Ne tikslai (v1)

- Telefono ar planšetės versija. Tik TV.
- Mėgstamų sąrašas, paieška, filtras pagal žanrą.
- Ekvalaizeris, garso efektų UI.
- Miego laikmatis (televizorius turi savo).
- Daugiau nei 10 stočių. Stotys — fiksuotas sąrašas kode. (2026-10-30 sąmoningai pakelta iki vienuolikos: pridėta European Hit Radio, žr. §6.5. Tai atskiras sprendimas, ne bendra taisyklė.)
- Nustatymų ekranas. Nėra ką nustatyti.
- Dienos/naktinės temos perjungimas. Tik tamsi.
- Kalbų perjungimas. Tik lietuvių.

Jei ko nors iš šito prireiks — tai bus atskiras sprendimas, ne „dar vienas mygtukas“.

## 5. Apribojimai

- **Platforma:** Android TV / Google TV. Ne Fire TV, ne telefonas.
- **Įrenginys:** tikras Android TV, kuriame APK įrašomas be kliūčių (sideload leidžiamas). Tikslus modelis ir Android versija užfiksuojami pirmą kartą prisijungus `adb` — nuo to priklauso `minSdk` ir fono serviso niuansai. Kol tai nepatvirtinta, laikom `minSdk 24`.
- **Kalba ir įrankiai:** Kotlin + Jetpack Compose. Pradedama nuo Fogwave forko.
- **Licencija:** Fogwave yra GPL-3.0-only. Forkas lieka GPL-3.0-only. Jei kada publikuosim — šaltinis turi būti prieinamas. Uždaros ar mokamos versijos negalės būti.
- **Be Google Play Services, be Firebase, be jokio SDK, kuris siunčia telemetriją.** Vienintelis tinklo srautas — radijo stoties URL.
- **Testavimas:** Android TV emuliatorius + bent vienas fizinis televizorius. Be fizinio įrenginio nelaikom versijos paruošta.

## 6. Nuspręsta

Žymėjimas: **[mūsų]** — sutarta pokalbyje, **[mano]** — mano sprendimas, kurį galima pakeisti.

### 6.1 Struktūra

**[mūsų]** Pasirinktas C variantas: viršuje didelis „dabar groja“ blokas, apačioje horizontali stočių lentyna.

**[mano]** Hero blokas yra **tik informacija, ne valdymas**. Jis nefokusuojamas, jame nėra mygtukų. Vienintelė fokusuojama vieta visame appse — stočių lentyna. Tai reiškia, kad fokusas niekada negali „pabėgti“ į kitą zoną, o ↑ ↓ mygtukai išvis nieko nedaro. Dėl to C yra tokia pat paprasta kaip vieno stulpelio sąrašas, tik gražesnė.

**[mūsų]** Jokių skirtukų, meniu, „Apie“ ekrano pirmame plane.

### 6.2 Paleidimas ir grojimas

**[mūsų]** Appsas paleidžiamas → iškart groja. Jokio „paspausk groti“.

**[mūsų]** Pirmą kartą paleidus — **Lietus**. Vėliau — paskutinė klausyta stotis.

**[mūsų]** Nėra pauzės mygtuko. Groja tol, kol appsas ekrane. Išėjus į Home / kitą appsą / paspaudus Back — tyla ir servisas sustabdomas.

**[mano]** Išimtis: jei užgęsta ekranas (TV screensaver), grojimas tęsiasi, kad būtų galima užmigti klausant.

**[mano]** Nutrūkus srautui, hero rodo tą pačią stotį su užrašu „Bandau vėl…“ — ekranas niekada netampa tuščias ir fokusas niekur nedingsta. Bandymai **nesibaigia**: 1→2→4→8→15→30 s, po to kas 30 s neribotai. Kai praeina ~30 s be sėkmės, hero papildomai parodo „Nepavyksta prisijungti. Paspausk OK.“ — bet appsas fone bando toliau. OK bando iš karto, nelaukiant kito intervalo. Tai reiškia, kad naktį trumpam dingus Wi-Fi, radijas atsigauna pats, o nepasilieka tylus iki ryto.

**[mano]** Kol ekranas užgesęs, bandymai taip pat nesustoja — kitaip vienintelė „pataisymo“ priemonė būtų įjungti televizorių ir pamatyti klaidą.

**[mano]** Grįžus iš screensaverio ar atsiradus tinklui, grojimas atnaujinamas automatiškai nuo gyvo transliacijos krašto, ne nuo ten, kur sustojo.

### 6.3 Valdymas

| Mygtukas | Veiksmas |
|---|---|
| ← → | Fokusas tarp stočių. Lentyna slenka pati. |
| OK | Groti fokusuotą stotį. Jei ji jau groja — nieko nedaro (jokio pauzės). |
| Back | Išeiti ir sustabdyti garsą. |
| ↑ ↓ | Nieko. |
| Home | Išeiti ir sustabdyti garsą. |

**[mano]** OK ant jau grojančios stoties sąmoningai nieko nedaro. Alternatyva (pauzė) sukurtų būseną, kurioje reikia suprasti, kodėl tylu.

**[mano]** **Sistemos medijos valdymas išjungtas.** Nei pranešime, nei Google Assistant, nei TV medijos paviršiuose nėra play/pause. Pranešime lieka tik „Stabdyti“. Be to, tai vienintelė vieta, kur sistema galėtų tyliai įjungti pauzę, kurios naudotojas nesuprastų.

**[mano]** Fokusas paleidžiant — ant grojančios stoties. Jei įmanoma, ji centruojama; prie sąrašo krašto — prispaudžiama, kad visas sąrašas liktų matomas.

### 6.4 Vizualinė kalba

**[mūsų]** Material You įkvėpta: spalva ateina iš turinio, ne iš fiksuotos paletės.

**[mano]** Kadangi TV neturi ekrano fono, iš kurio „Material You“ imtų spalvas, paletė imama iš **stoties ženklo spalvos**. Perjungus stotį, persidažo hero fonas, akcentai ir fokuso rėmelis. Tai tas pats principas, tik šaltinis kitas.

**[mano]** Tamsi tema, bet ne gryna juoda: paviršius `#141218`, tekstas `#F2EDF2`. Gryna juoda ant OLED televizorių atrodo kaip išjungtas ekranas ir praranda formą.

**[mano]** Tipografika: hero pavadinimas — didžiausias elementas ekrane (~78 sp @1080p, skalė nuo ekrano pločio), stoties metaduomenys — 18–20 sp, lentynos pavadinimai — 16 sp. Svoris 600–700, ne 400.

**[mano]** Fokuso indikatorius: 3–4 dp šviesus rėmelis + 6 % padidinimas + toninio paviršiaus pašviesėjimas. Jokio mėlyno „Android“ rėmelio. Kontrastas ≥ 3:1 prie bet kurios stoties spalvos.

**[mano]** Saugi zona: 5 % paraštės iš visų pusių (48 dp horizontaliai, 27 dp vertikaliai @1080p). Ne visi televizoriai rodo visą kadrą.

**[mano]** Grojimo būsena žymima ne vien spalva: mažas ekvalaizerio simbolis + žodis „Groja“. Spalva – papildoma, ne vienintelė informacija.

### 6.5 Stotys

Tas pats sąrašas kaip Fogwave (10 stočių, URL jau patikrinti ir veikiantys), plius vienuolikta stotis, pridėta 2026-10-30:

| Stotis | Ženklo spalva |
|---|---|
| Lietus | `#0E8FD1` |
| ZIP FM | `#FF5A1F` |
| Power Hit Radio | `#E5007D` |
| Power Hit Radio Gold | `#C9A227` |
| Rock FM | `#D7263D` |
| Relax FM | `#35B08A` |
| M-1 | `#FFC400` |
| M-1 Plius | `#1E6FE8` |
| Gold FM | `#B98B3E` |
| RC | `#E4322B` |
| European Hit Radio | `#E20E18` |

European Hit Radio srautas — oficialus lietuviškas `https://stream.ehr.lt:8443/ehr` (ne Latvijos EHR tinklo feed), ženklas iš `europeanhitradio.lt` balto logotipo.

**[mano]** Lentyna rodo 6–7 plyteles, likusios pasiekiamos slenkant. Fokusas centruojamas, kai įmanoma; prie pirmos ir paskutinės stoties jis prispaudžiamas prie krašto. **Sąrašas nesivynioja per galus:** paspaudus ← prie pirmos stoties nieko neįvyksta. Vyresniam naudotojui vyniojimasis atrodo kaip gedimas („aš jau buvau pradžioje, kodėl vėl rodo pabaigą“).

### 6.6 „Kas groja“

**[mūsų]** Po stoties pavadinimu — viena tyli eilutė su tuo, ką siunčia pati stotis (ICY `StreamTitle`, dažniausiai „Atlikėjas — Daina“).

**[mano]** Dydis ~36 % stoties pavadinimo, spalva balta 66 % permatomumo, svoris 500. Pavadinimas lieka vienintelis didelis elementas ekrane.

**[mano]** Trys taisyklės, kurios svarbesnės už patį tekstą:

1. **Jokio filtravimo ir „gražinimo“.** Stotis siunčia reklamą ar DJ tekstą — rodome reklamą ar DJ tekstą. Spėlioti, kas ten „gražu“, būtų neteisinga.
2. **Niekada nešokinėja ir neslenka.** Fiksuota vieta, viena eilutė, nukirpimas „…“. Slenkantis tekstas vyresniam žmogui atrodo kaip gedimas, o kintantis aukštis – kaip persikrovęs ekranas.
3. **Kai duomenų nėra — tuščia, bet vieta lieka.** Bitrate ir žanras iš hero šalinami: tai inžinieriaus informacija, ne tėvų.

**[mano]** Atnaujinimas be judesio arba su 150–200 ms crossfade. Jokio „flash“ kas 20 sekundžių.

### 6.7 Ko sąmoningai nedarom

**[mano]** Neatnaujinam `MediaItem.mediaMetadata` su dainos pavadinimu. Medijos pranešime valdymo nėra, tad ten ši informacija nieko nepasiektų, o papildomas atnaujinimo kelias servise būtų dar viena vieta, kuri gali sugesti.

## 7. Techninis planas

### 7.1 Forkas

Forkinam `kalarina-bit/Fogwave`, laikom git istoriją. Nekeliame PR į upstream (jų CONTRIBUTING draudžia UI pakeitimus, o mes keičiam viską). Upstream `player/` pataisymus galima traukti rankiniu būdu.

Šalinam: visą `ui/` paketą, `MainActivity`, `fastlane` metaduomenis, `automotive_app_desc.xml`, baterijų optimizavimo dialogą.

Paliekam ir perrašom minimaliai: `data/`, `player/`.

### 7.2 Ką naudojam iš Fogwave

| Failas | Likimas |
|---|---|
| `player/FogwavePlaybackService.kt` | Naudojam beveik kaip yra: ExoPlayer, `MediaLibraryService`, reconnect backoff, tinklo callback, ICY header, live-edge seek. Pridedam amžiną retry ciklą ir terminalinę būseną UI. Medijos pranešime transporto mygtukų nebelieka (žr. §6.3). |
| `player/RadioPlayerManager.kt` | Naudojam. Pridedam `stop()` ir „sustabdyti kai appsas išeina“ logiką. |
| `player/StationMediaItems.kt` | Naudojam. |
| `player/FogwavePlaybackService.kt` (metadata) | **Naujas darbas.** ICY meta šiandien neskaitomas. Media3 `ProgressiveMediaPeriod` ICY meta pateikia kaip atskirą `MimeTypes.APPLICATION_ICY` taką; `MetadataDecoderFactory` turi `IcyDecoder`, o `MetadataRenderer` (įjungtas `DefaultRenderersFactory`) jį iššifruoja į `IcyInfo`, kuris ateina per `Player.Listener.onMetadata`. Reikia: `onMetadata` → `IcyInfo.title` → UI būsena, atnaujinti tik pasikeitus, išvalyti perjungus stotį. |
| `player/AudioEffectsEngine.kt` | **Pašalinta 0.1.0 versijoje.** Be UI jo neįmanoma įjungti, o kiekvienas grotuvas vis tiek kurdavo `Equalizer`, `LoudnessEnhancer` ir `DynamicsProcessing` ant garso sesijos. Televizoriaus garso kelyje tai nereikalinga rizika. |
| `data/model/RadioStation.kt` | Naudojam, išmetam `ViewMode`, `IconSizeStep`, `AppTab`, `ExtraPlayerTab`. |
| `data/repository/RadioRepository.kt` | Naudojam stotims ir `last_station_id`. Išmetam favorites/recents/view mode. |
| `ui/**` | Trinam. |

### 7.3 Nauja struktūra

Vienas modulis, toliau `:app`.

```
app/src/main/java/<pkg>/
  MainActivity.kt
  RadijasApp.kt
  data/            ← iš Fogwave, apkarpyta
  player/          ← iš Fogwave, apkarpyta
  ui/
    NowPlayingScreen.kt
    HeroPanel.kt
    StationShelf.kt
    StationTile.kt
    theme/         ← tonalinė paletė iš stoties spalvos
    Focus.kt       ← fokuso rėmelio modifier
```

### 7.4 Android TV manifestas

```xml
<uses-feature android:name="android.software.leanback" android:required="true" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />

<activity android:name=".MainActivity"
          android:exported="true"
          android:screenOrientation="landscape">
  <intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
  </intent-filter>
</activity>
```

- `android:banner` — 320×180 drawable, privalomas TV launcheriui.
- **Neeksportuojam** `LAUNCHER` kategorijos: appsas skirtas tik TV.
- `minSdk 24` (Android TV 7.0), `targetSdk` — naujausias stabilus.
- `androidx.tv:tv-material:1.1.0` + tas pats Compose BOM kaip Fogwave.

### 7.5 Gyvavimo ciklas

- `MainActivity.onCreate` → paleidžiam `FogwavePlaybackService` (foreground, `mediaPlayback`), perduodam stotį.
- **Back** → `finish()` → `onDestroy` → `stopService()`. Tyla.
- **Home / perėjimas į kitą appsą** → `onUserLeaveHint()` → `stopService()`. Tyla. Šis signalas patikimesnis už `onStop`, nes screensaveris taip pat kutena `onStop`, bet `onUserLeaveHint` nekutena.
- **Screensaveris** → `onStop` be `onUserLeaveHint` → grojam toliau.
- **TV užgesinimas / standby** → grojam toliau. Praktiškai garso negirdėti, o sustabdymas reikštų, kad radijas „dingo“ naktį ir nebeatsinaujins.
- **HDMI įėjimo perjungimas** → jei sistema duoda `onUserLeaveHint` — sustojam; jei ne, grojimas lieka. Tai priimtina: garsas per soundbarą šiuo atveju yra mažesnė bėda nei radijas, kuris tyliai mirė.
- `onDestroy` → atleidžiam `MediaController`.
- Audio focus — ExoPlayer numatytasis elgesys. Praradus focusą — tyliai pauzuojam, atgavus — tęsiam.

Ši taisyklė sąmoningai remiasi vienu signalu (`onUserLeaveHint`), o ne ekrano būsena. Kūrėjas turi patikrinti ją fiziniame televizoriuje — elgsena tarp gamintojų skiriasi, ir tai viena iš dviejų vietų, kur emuliatorius gali meluoti.

### 7.6 Būsenos

`Connecting` → hero rodo stoties pavadinimą + „Jungiamasi…“, jau matomas visas ekranas.
`Playing` → „Gyvai“ + ekvalaizerio simbolis.
`Reconnecting` → ta pati stotis + „Bandau vėl…“.
`NoConnection` → ta pati stotis + „Nepavyksta prisijungti. Paspausk OK.“ — **nėra galutinė būsena**, bandymai tęsiasi.
`Paused` → „Pristabdyta. Paspausk OK.“ — atsiranda tik tada, kai garsą perima kita programėlė (audio focus). Tai ne pauzės mygtukas, o sąžininga būsena, kad žmogus nesėdėtų prie tylios „Gyvai“.

Jokioje būsenoje ekranas neturi mažiau elementų nei `Playing` — niekas nešokinėja.

## 8. Kaip žinosim, kad pavyko

- Šaltas startas televizoriuje → garsas per ≤ 5 s.
- Visas appsas įveikiamas vien ranka ant pulto, nenaudojant jokių kitų mygtukų.
- Iš bet kurios būsenos iki bet kurios stoties — ≤ 2 paspaudimai.
- Nė vieno ekrano be matomo fokuso.
- Dainos eilutė niekada neslenka ir nekeičia išdėstymo; stotis be metaduomenų atrodo identiškai, tik be teksto.
- Teksto kontrastas ≥ 4.5:1, fokuso rėmelio ≥ 3:1.
- `adb shell dumpsys package` rodo nė vieno tinklo leidimo, išskyrus `INTERNET`.
- Appsas neina į foną ir nepalieka grojančio serviso.

## 9. Kas lieka kūrėjui

- Package name ir galutinis apps'o pavadinimas (siūlytas „Radijas“).
- Ikona ir TV banneras (320×180).
- Konkretūs animacijos trukdžiai ir kreivės.
- Ar `AudioEffectsEngine` apskritai reikalingas, jei be UI ir išjungtas.
- Tiksli klaidų tekstų formuluotė.
- Ar verta `Roborazzi` screenshot testų TV ekranams.
- Kaip tiksliai nuimti transporto mygtukus nuo medijos pranešimo: savas `MediaNotificationProvider` ar paprastas foreground servisas be `MediaSession`. Pirmasis palieka duris „Now Playing“ integracijai, antrasis paprastesnis.

## 10. Rizikos

- **Srauto URL gali numirti.** Revma naudoja trumpalaikius tokenus su redirectu — variklis tai jau sprendžia, bet stotys laikas nuo laiko keisis.
- **Pigūs TV box'ai prastai elgiasi su Compose fokusu.** Būtinas testas fiziniame įrenginyje.
- **Overscan.** Dalis televizorių nukerpa 3–5 % kadro; dėl to saugi zona yra 5 %.
- **HDMI garso vėlavimas** pirmą sekundę po paleidimo — normalu, ne klaida.
- **Upstreamas gyvas silpnai** (0 žvaigždučių, vienas autorius). Ilgainiui variklį gali tekti prižiūrėti patiems.

## 11. 2026-10-30 patvirtinti papildymai

Ši dalis turi pirmenybę prieš aukščiau esančius pradinius vizualinius ir tinklo apribojimus:

- Vienuolika stočių rikiuojamos A–Z. Paleidimas visada pagal paskutinį išsaugotą ID;
  pirmą kartą arba dingus išsaugotai stočiai — Lietus. Rikiavimas nepakeičia šios taisyklės.
- Karuselė slenka per visą ekrano plotį; 48 dp saugi horizontali paraštė yra turinio,
  ne viewport dalis. Kraštai švelniai išnyksta, fokuso masteliui paliekama 8 dp vertikaliai.
- Didesni apvalinimai ir vientisi toniniai paviršiai; nėra vidinio juodo logotipo kvadrato.
  Tai Material You įkvėpta TV kryptis, ne naujas nustatymų ar temų pasirinkimas.
- Hero ir karuselėje **visada stoties ženklas**. Išbandyti albumų viršeliai
  vėlesniu vartotojo sprendimu pašalinti visiškai, kartu su jų paieška ir talpykla.
- Power Hit Radio ir Gold duomenims leidžiamos tiesioginės užklausos į oficialią
  svetainę. Užklausų į muzikos katalogus ar paveikslėlių CDN nėra. Jokios svetainių
  JavaScript, reklamų ar analitikos integracijos. Privatumo aprašas atnaujinamas.
- Prie „Gyvai“ leidžiami trys maži tikro PCM garso lygio brūkšneliai. Jie nekeičia
  maketo, nenaudoja mikrofono, neįrašo ir nesiunčia garso. Ne dažnių spektro grafikas.
- Vienas ekranas, keturi pulto mygtukai, jokių papildomų pasirinkimų ar registracijos.

## 12. 2026-10-30 re-baseline (autoritetingos reikšmės)

Ši dalis fiksuoja, ką kodas daro **dabar**; ankstesnės dalys lieka istorinės. Patikrinta 2026-10-30 17:39 pagal išeities kodą ir `app/build/test-results/testDebugUnitTest/`. Kur ankstesnė dalis prieštarauja šiai, galioja ši.

### 12.1 Stotys ir spalvos

Šaltinis: `app/src/main/java/lt/radijas/tv/data/repository/RadioRepository.kt:12-93`.

| id | Stotis | `brandColor` |
|---|---|---|
| `zip` | ZIP FM | `#1717E8` |
| `powerhit` | Power Hit Radio | `#ED1C24` |
| `powerhitgold` | Power Hit Radio Gold | `#C9A13D` |
| `rock` | Rock FM | `#D72828` |
| `relax` | Relax FM | `#7C4DB3` |
| `lietus` | Lietus | `#009A61` |
| `m1` | M-1 | `#1638C6` |
| `plus` | M-1 Plius | `#C92636` |
| `goldfm` | Gold FM | `#C9A13A` |
| `rc` | Radiocentras | `#F05A28` |
| `ehr` | European Hit Radio | `#E20E18` |

`RadioRepository.kt:93` rikiuoja `stations` pagal pavadinimą (`sortedBy { it.name.lowercase(Locale.ROOT) }`), todėl rodoma tvarka A–Z yra: `ehr`, `goldfm`, `lietus`, `m1`, `plus`, `powerhit`, `powerhitgold`, `rc`, `relax`, `rock`, `zip`.

**§6.5 lentelė (113–125 eil.) pasenusi**: jos spalvos neatitinka nė vienos stoties (pvz., joje Lietus `#0E8FD1`, kode — `#009A61`; ZIP FM `#FF5A1F`, kode — `#1717E8`).

### 12.2 Hero

Šaltinis: `app/src/main/java/lt/radijas/tv/ui/HeroPanel.kt`.

- Stoties pavadinimas: didžiausias **96 sp**, mažinamas po 4 sp iki **48 sp** grindų, viena eilė (`HeroPanel.kt:160,163,199-211`). §6.4 (~78 sp) nebegalioja.
- Pavadinimo juosta `heightIn(min = 113.dp)`, kad ilgas vardas nejudintų žemesnių eilučių (`HeroPanel.kt:193`).
- „Kas groja": **26 sp**, 66 % baltos, fiksuotas 44 dp blokas (`HeroPanel.kt:224-241`).
- Būsena: **24 sp**, `lineHeight = 30.sp`, `SemiBold` (`HeroPanel.kt:254-269`); anksčiau buvo 16 sp. Būsenos tekstas eina pirma, ekvalaizerio brūkšneliai — **po jo** (`HeroPanel.kt:270-271`); brūkšneliai komponuojami visose būsenose ir tampa permatomi, kai negroja.
- Hero viršuje **nebėra „Radijas" užrašo** — `Column` prasideda stoties pavadinimu (`HeroPanel.kt:100-115`).
- Hero vidinė paraštė 44 dp horizontaliai / 20 dp vertikaliai (`HeroPanel.kt:103`), logotipas 168 dp (`HeroPanel.kt:124`).

### 12.3 Lentyna, plytelė ir accessibility

Šaltiniai: `ui/StationShelf.kt`, `ui/StationTile.kt`.

- Plytelės plotis 176 dp, logotipas 156 dp, pavadinimas **16 sp** / 22 sp eilutės aukštis, svoris `Normal` (fokusuota — `SemiBold`) (`StationTile.kt:98,165,177,181-191`).
- Fokusavimas **ne centruojamas**: `StationShelf.kt:64-76` vieną kartą paslenka sąrašą `scrollToItem(revealIndex)` (plytelė atsiduria pradžioje), o vėliau sąrašas nebejudinamas. §6.5 (129 eil.) teiginys „Fokusas centruojamas, kai įmanoma…" prieštarauja kodui ir nebegalioja.
- Accessibility: plytelė — `Role.Button` su `onClick` veiksmu „Rinktis stotį"; grojanti plytelė turi `stateDescription = "Groja"` ir atskirą 48×3 dp ženklą (`StationTile.kt:107-114,141-152`; `app/src/main/res/values/strings.xml:20-21`). `StationShelf.kt:108` perduoda `isPlaying`.
- `ui/Focus.kt`, nurodytas §7.3 (188 eil.), **neegzistuoja**; fokuso rėmelis piešiamas tiesiai plytelėje (`StationTile.kt:153-159`).

### 12.4 Leidimai

Šaltinis: `app/src/main/AndroidManifest.xml`.

- `ACCESS_NETWORK_STATE` **yra** (`AndroidManifest.xml:3`), taip pat `WAKE_LOCK`, `FOREGROUND_SERVICE` ir `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (`:4-6`). §8 (243 eil.) teiginys, kad yra tik `INTERNET`, klysta.

### 12.5 Būsenos (2026-10-30 pataisymai)

- „Gyvai" sprendžiama pagal girdimumą (`isPlaying`), ne pagal `playWhenReady`: `player/PlayerStateMapping.kt:20-36`, kviečiama iš `player/RadioPlayerManager.kt:102-113`.
- OK ant grojančios plytelės — **nieko** (`StationAction.IGNORE`); ant nepavykusios ar pristabdytos — `RETRY`; ant kitos stoties — `SELECT` (`StationSelection.kt:30-38`, kviečiama `MainActivity.kt:65-77`).
- Sustabdytas grotuvas (`Idle` be ketinimo groti) rodomas „Pristabdyta", o ne „Jungiamasi…" (`ui/PlayerStatus.kt:37-46`).
- Testai: **96**, 0 klaidų (XML 2026-10-30 17:39). Iš jų `PlayerStateMappingTest` 8, `StationSelectionTest` 6, `PlayerStatusTest` 20, `StationShelfTest` 10.

### 12.6 Dokumentų pastabos

- README „ekranas nešokinėja" galioja **1.0 šrifto masteliui**; didesnis `fontScale` gali padidinti fiksuoto aukščio juostas (`HeroPanel.kt:193`).
- README testų skaičius dabar **96**, o ne 78 ar 94; istoriniai skaičiai palikti `docs/2026-10-30-radio-update.md` su istorine įžanga.
- Screenshotai `docs/screenshots/2026-10-30/` daryti prieš šiuos UI pataisymus (juose dar yra „Radijas" užrašas ir mažesnė būsenos eilutė).
