# Būsena

Atnaujinta: 2026-10-30

Šis failas skirtas tam, kas tęsia darbus po pertraukos arba kitoje mašinoje. README pasakoja, kas tai per programa. Čia surašyta, kas padaryta, kas liko ir kokie sprendimai dar nepriimti.

## Padaryta

- TV-only programėlė: `LEANBACK_LAUNCHER`, baneris, jokios telefono versijos.
- Automatinis grojimas paleidus: paskutinė stotis, pirmą kartą Lietus.
- Viena fokusuojama zona, jokio vyniojimosi, ↑↓ nieko nedaro.
- Penkios būsenos ir jų tekstai, įskaitant klaidą po 30 sekundžių ir „Pristabdyta" po audio focus.
- „Kas groja" iš ICY meta, fiksuoto aukščio eilutė, jokio slinkimo.
- Paletė iš stoties ženklo spalvos, kontrastas tikrinamas automatiškai visoms vienuolikai stočių.
- Logotipai balti siluetai, apkirpti, sugeneruoti iš `assets/brand`.
- Pranešime tik „Stabdyti", išoriniams valdikliams atimtos rašymo komandos.
- Vienuolikta stotis: European Hit Radio (`ehr`, `https://stream.ehr.lt:8443/ehr`), ženklas iš oficialaus balto logotipo, spalva `#E20E18` iš to paties logotipo.
- Grįžimas į programėlę nebeperkrauna srauto, jei tas pats kanalas dar groja (`RelaunchPolicy`), todėl nereikalingo buferiavimo ir tuščios „Kas groja" eilutės nebelieka.
- A–Z katalogas; paleidimo stotis ir toliau nustatoma pagal išsaugotą ID, ne sąrašo vietą.
- Power Hit Radio / Gold papildomi duomenys iš oficialios svetainės. Vartotojo sprendimu albumų viršeliai visiškai pašalinti; hero ir karuselėje visada stoties logotipas. Paieškos, atsisiuntimo ir talpyklos kodo neliko.
- Trijų PCM lygių vizualizacija be mikrofono leidimo. Karuselės viewport per visą plotį, saugi paraštė turinyje ir švelnūs kraštai; pašalintas papildomas juodas logotipo kvadratas.
- Po viršelių pašalinimo Java 21 patikra: 78 testai praeina, debug APK susirenka, lint — 0 klaidų / 31 įspėjimas. Išsamūs įrodymai ir screenshotai — `docs/2026-10-30-radio-update.md` ir `docs/screenshots/2026-10-30/`.
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

## Kaip tęsti su agentu

Agentui užtenka trijų failų: `README.md`, `docs/superpowers/specs/2026-10-28-tv-radio-design.md` ir šio. Pavyzdinis prašymas:

> Perskaityk README.md, docs/superpowers/specs/2026-10-28-tv-radio-design.md ir docs/STATUS.md. Tai Android TV radijo programėlė. Noriu padaryti [X]. Nekeisk to, kas sąraše „Padaryta", be mano sutikimo, ir laikykis taisyklės, kad nauja funkcija negali pridėti ekrano ar pasirinkimo.

Jei dirbama su tuo pačiu repo, prieš pradedant verta paleisti `./gradlew testDebugUnitTest`, kad būtų aišku, jog išeities taškas sveikas.
