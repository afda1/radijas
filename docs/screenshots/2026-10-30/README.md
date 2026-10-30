# Android TV 36, 1920×1080

Tikros veikiančio debug APK ekrano nuotraukos iš emuliatoriaus (`adb shell screencap`),
ne maketai. Stotys ir dainos yra gyvi duomenys, todėl kito paleidimo metu skirsis.
Kadrai `02`–`06` užfiksuoti prieš hero UI pataisymus ir prieš galutinį albumų viršelių
pašalinimą; `07`–`08` ir `10`–`18` rodo dabartinį ekraną. Visi kadrai rodo tik stočių ženklus. Ankstesnis `01-lietus.png` su viršeliu
pašalintas, kad nebūtų klaidinantis dabartinės programėlės pavyzdys.

- `07-playing-marked.png`: **dabartinis ekranas**. Dainos eilutė, 24 sp būsena, o grojanti
  plytelė pažymėta brando juosta ir ryškesniu vardu, nors fokusas jau ant kitos plytelės.
- `08-paused.png`: „Pristabdyta. Paspausk OK." po pranešimo „Stabdyti"; OK grąžina grojimą.
- `10-m3-ui.png`: M3 paletė iš stoties spalvos (Power Hit Radio Gold); kiekviena plytelė turi savo stoties `primaryContainer`, „Ekranas" mygtukas po logotipu.
- `11-screen-off.png`: ekranas užgesintas, dar matoma užuomina.
- `12-m3-start.png`: paleidimas — paskutinė stotis, fokusas jau ant jos; logotipai optiškai suvienodinti (wordmark'ai per visą plotį, tankūs ženklai mažesni).
- `13-m3-lietus.png`: Lietus; žalias hero, toninis fonas.
- `14-m3-playing-unfocused.png`: groja Lietus, fokusas ant Gold FM; plytelės žymimos tik fokuso rėmeliu, jokios papildomos juostos.
- `15-m3-zip-fm.png`: paskutinė A–Z stotis ZIP FM.
- `16-m3-ehr.png`: pirmoji A–Z stotis European Hit Radio.
- `17-screen-button-focus.png`: „Ekranas" valdiklis būsenos eilutės gale, fokusuotas — užrašas pasirodo tik tada; logotipas vėl 168 dp.
- `18-screen-off-black.png`: grynas juodas kadras po to, kai užuomina išnyksta.
- `02-power-hit-radio.png`: logotipo atsarga. Šio kadro metu svetainė grąžino
  tuščią atlikėją ir „Eteryje - Informacija“, todėl dainos eilutė sąmoningai tuščia.
  Ankstesnėje gyvoje patikroje rodė „GØYA - Hope“.
- `03-power-hit-gold.png`: ilgas stoties ir dainos pavadinimas, logotipo atsarga.
- `04-carousel-end.png`: paskutinė A–Z stotis ZIP FM; visas fokuso rėmelis.
- `05-ehr-carousel-start.png`: pirmoji A–Z stotis EHR; visas fokuso rėmelis.
- `06-logo-fallback.png`: Lietus su stoties logotipu; dabar logotipas rodomas visada,
  nepriklausomai nuo dainos ar laidos (senas failo pavadinimas išsaugotas).

PNG fiksuoja tik vieną garso lygio vizualizacijos kadrą; judėjimo ir garso sinchroniškumo
neįrodo.
- `19-real-tv-ui.png`: tas pats build'as realiame televizoriuje (Telia DV8919X-LT, Android 14) — M3 paletė, plytelių atspalviai, optiškai suvienodinti logotipai.
- `20-real-tv-screen-off.png`: ekranas užgesintas realiame televizoriuje; kadras 0,0,0 juodas, garsas groja toliau.
