# Būsena

Atnaujinta: 2026-10-29

Šis failas skirtas tam, kas tęsia darbus po pertraukos arba kitoje mašinoje. README pasakoja, kas tai per programa. Čia surašyta, kas padaryta, kas liko ir kokie sprendimai dar nepriimti.

## Padaryta

- TV-only programėlė: `LEANBACK_LAUNCHER`, baneris, jokios telefono versijos.
- Automatinis grojimas paleidus: paskutinė stotis, pirmą kartą Lietus.
- Viena fokusuojama zona, jokio vyniojimosi, ↑↓ nieko nedaro.
- Penkios būsenos ir jų tekstai, įskaitant klaidą po 30 sekundžių ir „Pristabdyta" po audio focus.
- „Kas groja" iš ICY meta, fiksuoto aukščio eilutė, jokio slinkimo.
- Paletė iš stoties ženklo spalvos, kontrastas tikrinamas automatiškai visoms dešimčiai stočių.
- Logotipai balti siluetai, apkirpti, sugeneruoti iš `assets/brand`.
- Pranešime tik „Stabdyti", išoriniams valdikliams atimtos rašymo komandos.
- 63 testai, paleidžiami JVM'e be emuliatoriaus.
- README, ekrano nuotraukos, logotipas, TV baneris, AI autorystės disclosure.

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

- Power Hit Radio ir Power Hit Radio Gold nesiunčia `StreamTitle`, todėl „Kas groja" ten tuščia. Tai stoties pusė.
- Stotys įrašytos kode. Pasikeitus srauto adresui reikia naujos versijos.
- Išbandyta viename realiame įrenginyje (Telia DV8919X-LT, Android 14) ir emuliatoriuje. Kituose televizoriuose `onUserLeaveHint` ir screensaverio elgsena gali skirtis.
- Paletės kontrasto testas pririštas prie 960 dp pločio; labai dideliuose ekranuose ilgas dainos pavadinimas teoriškai gali nukristi žemiau 4.5:1.
- Fokuso rėmelio 6 % padidinimui lentyna duoda 6 dp, o reikia 6,24 dp. Subpikselinis nukirpimas, vizualiai nematomas.

## Kaip tęsti su agentu

Agentui užtenka trijų failų: `README.md`, `docs/superpowers/specs/2026-10-28-tv-radio-design.md` ir šio. Pavyzdinis prašymas:

> Perskaityk README.md, docs/superpowers/specs/2026-10-28-tv-radio-design.md ir docs/STATUS.md. Tai Android TV radijo programėlė. Noriu padaryti [X]. Nekeisk to, kas sąraše „Padaryta", be mano sutikimo, ir laikykis taisyklės, kad nauja funkcija negali pridėti ekrano ar pasirinkimo.

Jei dirbama su tuo pačiu repo, prieš pradedant verta paleisti `./gradlew testDebugUnitTest`, kad būtų aišku, jog išeities taškas sveikas.
