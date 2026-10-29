# Išleidimas

Šis failas skirtas tam, kas ruošia naują versiją. Visi komentarai yra apie tai, ką daryti, o ne apie tai, kas jau padaryta.

## Šaka

`main` yra integracijos šaka. Release paruošimas vyksta atskiroje šakoje, pavyzdžiui `release/v0.1.0`:

```bash
git checkout main
git pull
git checkout -b release/v0.1.0
```

Joje keliama versija, daromi paskutiniai pataisymai ir tikrinama įrenginyje. Kai viskas veikia, šaka sujungiama į `main` ir pažymima žyma.

## Versija

`app/build.gradle.kts`:

```kotlin
versionCode = 1          // sveikas skaičius, kiekviename išleidime +1
versionName = "0.1.0"    // rodoma naudotojui
```

`versionCode` negali mažėti, kitaip Android neleis atnaujinti. Jo niekada nekartok tarp išleidimų.

## Pasirašymas

Reikia keystore failo. Jis kuriamas **vieną kartą** ir saugomas amžinai: jei jį pametusi, negalėsi išleisti atnaujinimo tiems patiems naudotojams, teks prašyti, kad jie iš naujo įdiegtų programą.

```bash
keytool -genkeypair -v \
  -keystore ~/radijas-release.jks \
  -alias radijas \
  -keyalg RSA -keysize 4096 -validity 10000
```

Saugok du dalykus atskirai: patį `.jks` failą (ne git repozitorijoje) ir slaptažodį (slaptažodžių saugykloje). Abu prarasti yra ta pati problema.

## Build'as

```bash
export JAVA_HOME=/kelias/iki/jdk-17
export ANDROID_HOME=/kelias/iki/android-sdk

export KEYSTORE_PATH=~/radijas-release.jks
export STORE_PASSWORD='...'
export KEY_ALIAS=radijas
export KEY_PASSWORD='...'

./gradlew clean testDebugUnitTest assembleRelease
```

Jei `KEYSTORE_PATH` nerastas, `assembleRelease` vis tiek sukurs APK, bet nepasirašytą. Tai tyli klaida, todėl po build'o būtinai patikrink parašą.

## Parašo patikra

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Komanda turi išvardinti sertifikatą. Jei rašo, kad APK nepasirašytas, sustok ir tvarkyk aplinką, o ne tęsk.

Sertifikato SHA-256 pirštų atspaudą įrašyk į README skyrių „Įdiegimas", kad žmonės galėtų patikrinti, ar atsisiuntė tikrą failą.

## Patikra įrenginyje

Prieš išleidžiant, būtinai:

1. Įrašyti `app-release.apk` į realų televizorių ir paleisti.
2. Pereiti `docs/STATUS.md` skyrių „Liko iki išleidimo".
3. Patikrinti, kad groja iš karto, kad pultas judina fokusą, kad Back ir Home nutildo, kad pranešime nėra play ar pauzės.

Emuliatorius šitam netinka: `onUserLeaveHint` ir screensaverio elgsena jame skiriasi nuo tikro įrenginio.

## Žyma ir publikavimas

```bash
git add -A
git commit -m "release: 0.1.0"
git tag -a v0.1.0 -m "Radijas 0.1.0"
git push origin release/v0.1.0
git push origin v0.1.0

gh release create v0.1.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "Radijas 0.1.0" \
  --notes "Pirmas išleidimas."
```

Po to sujungti šaką į `main` ir įkelti:

```bash
git checkout main
git merge --no-ff release/v0.1.0
git push origin main
```

## Po išleidimo

- README skyrių „Įdiegimas" papildyti nuoroda į GitHub Releases ir sertifikato pirštų atspaudu.
- Pakelti `versionCode` ir `versionName` kitoje šakoje, o ne tame pačiame commite.
- Jei pasikeitė stoties srauto adresas, tai yra pataisymas, kuris išeina kaip nauja versija. Stotys yra kode, todėl kito kelio nėra.
