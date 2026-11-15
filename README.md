<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Radijas programėlės ikona" width="120">

# Radijas

**Įjungi ir groja.**

Lietuviškas radijas tavo Android TV: vienas ekranas, 11 stočių ir jokio meniu.

</div>

<img src="docs/screenshots/current/lietus.png" alt="Radijas Android TV ekrane: groja Lietus, matoma daina, laikas, data ir stočių lentyna" width="100%">

| Gold FM | Pasirinktas mygtukas „Patamsinti ekraną“ | Užtemdytas ekranas |
|---|---|---|
| <img src="docs/screenshots/current/gold-fm.png" alt="Gold FM groja su dabartiniu laiku ir data" width="100%"> | <img src="docs/screenshots/current/screen-control.png" alt="Pultu pasirinktas mygtukas Patamsinti ekraną, grojanti EHR plytelė pažymėta žymekliu" width="100%"> | <img src="docs/screenshots/current/screen-off.png" alt="Užtemdytas ekranas juodame fone rodo blankų laiką ir grojamą kūrinį" width="100%"> |

Ekrano nuotraukos sugeneruotos iš dabartinės programėlės Compose UI (1920×1080).

## Kam tai skirta?

Pirmiausia savo tėvams: žmonėms, kurie nori įsijungti radiją televizoriuje nesukdami galvos dėl paskyrų, meniu ar nustatymų. Ir visiems kitiems, kuriems norisi tiesiog klausytis.

## Ką gali?

- **Iškart groja** paskutinę klausytą stotį; pirmą kartą groja Lietus.
- **11 lietuviškų stočių**, kurias lengva pasirinkti televizoriaus pultu; grojanti stotis karuselėje pažymėta taškeliu ir rėmeliu net kai fokusas perkeltas kitur.
- **Automatiškai bando prisijungti iš naujo**, nutrūkus ryšiui.
- **Rodo, kas groja**, kai stotis pateikia dainos informaciją, taip pat **dabartinį laiką ir datą** pagrindiniame ekrane.
- **Patamsina ekraną**, kai norisi tik garso: juodame fone blankiai lieka galioti dabartinis laikas ir grojantis kūrinys (arba stoties pavadinimas, jei kūrinio meta nėra) su kasminutiniu poslinkiu OLED ekranų apsaugai. Garso mygtukai veikia neatjungiant tamsaus režimo, o bet kuris kitas pulto mygtukas grąžina vaizdą. Tai juodas vaizdas, o ne fizinis ekrano išjungimas.
- **Jokių paskyrų, reklamų ar analitikos.** Radijo stočių ir metaduomenų serveriai mato įrenginio IP adresą.

## Kodėl taip paprastai?

Programėlė sukurta televizoriui ir valdymui pultu. Didelės, aiškios stotys, vienas ekranas ir jokio bereikalingo pasirinkimo padeda iki muzikos nueiti kuo mažiau žingsnių. Nėra nustatymų, paieškos ar pauzės mygtuko: paprastumas čia yra sąmoningas sprendimas.

## Kaip sukurta?

Projektas sukurtas **dirbtinio intelekto pagalba, žmogui vadovaujant**. Produkto ir dizaino sprendimus priėmė žmogus; kodas, testai ir dokumentacija rašyti pasitelkiant DI modelius per OpenCode. Programėlė tikrinta emuliatoriuje ir realiame Android TV įrenginyje.

Sukurta su **Kotlin**, **Jetpack Compose for TV** ir **Media3 / ExoPlayer**. Grojimo variklis ir stočių katalogas paremti [Fogwave](https://github.com/kalarina-bit/Fogwave).

## Įdiegimas

Išleisto APK kol kas nėra. Jį paskelbus, jis bus prieinamas [GitHub Releases](https://github.com/afda1/radijas/releases). Programėlei reikia Android TV ir interneto ryšio.

Norint sukurti iš kodo, reikia JDK 21 ir Android SDK su `platforms;android-37.0`:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Windows sistemoje naudok `gradlew.bat`. Išleidimo informacija: [RELEASING.md](RELEASING.md).

---

Platinama pagal [GPL-3.0-only](LICENSE). Stočių pavadinimai ir ženklai priklauso jų savininkams; daugiau informacijos: [NOTICE](NOTICE).
