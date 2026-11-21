# Radijas TV — Google Nest Hub estetikos atnaujinimas

2026-11-21

Atsarginė ankstesnės būsenos šaka: `backup/pre-google-nest` (`6b9983d`).
Ankstesni dokumentai: `2026-10-30-m3-system-and-screen-off-design.md`, `2026-11-21-google-tv-polish-design.md`.

## Keturi patvirtinti pakeitimai

1. **Ramios toninės plytelės karuselėje (`StationTile`):**
   * Ramybės būsenoje plytelės turi tamsų toninį foną (`lerp(surface, primaryContainer, 0.22f)`), subtilų `1.dp` apvadą (`9 % onSurface`) ir švelnią `80 % onSurface` etiketę.
   * Grojanti, bet nefokusuota plytelė švyti `65 % primaryContainer` tonu su `14 % onSurface` `1.dp` apvadu, akcentiniu `6.dp` tašku ir `Medium` pavadinimu (be išorinio fokuso rėmelio).
   * Tik aktyviai fokusuota plytelė užsidega pilnu `primaryContainer`, gauna `3.dp` `border` fokuso rėmelį (≥ 6:1) ir akimirksniu padidėja `1.06×`.
   * Horizontali „Nest“ proporcija: `208.dp × 168.dp` kortelė su `164.dp × 106.dp` logotipo dėžute (išlaikant esamą `LogoInkTarget` normalizaciją).

2. **Atmosferinis fonas per visą ekraną (`NowPlayingScreen`, `HeroPanel`):**
   * Pagrindinio ekrano viršuje per `#141218` foną skleidžiasi minkštas radialinis stoties `primaryContainer` švytėjimas (`26 % → 10 % → 0 %`).
   * Hero kortelė tampa pusiau skaidria „smoked“ dūmine plokštuma (`74 % surface` su platesniu `38 % → 15 % → 0 %` švytėjimu), o tarpas tarp Hero ir karuselės padidinamas `14.dp → 18.dp`.

3. **Nest Hub Ambient tamsusis režimas (`ScreenOffOverlay`):**
   * Centre lieka `116.sp` Regular laikrodis (`-3.0.sp` tracking, `36 % onSurface`) ir `24.sp` lietuviška data (`28 % onSurface`).
   * Grojanti daina arba stotis nuleidžiama į atskirą apatinę *Now Playing* eilutę (`24.sp`, `28 % onSurface`, `6.dp` `primary` taškelis `34 %`), išlaikant tą patį minutinį pixel-shift poslinkį ir saugų atstumą virš pabudimo užuominos.

4. **Švelnus spalvų persiliejimas ir lengvesnė tipografija (`RadijasTheme`, `HeroClock`, `HeroPanel`):**
   * D-pad fokusas lieka akimirksninis, o fono / Hero / ekvalaizerio spalvos keičiant stotį persilieja per `450 ms` (`animateColorAsState`).
   * `displayLarge` lengvinamas iš `Bold` į `SemiBold` (`letterSpacing = (-1.2).sp`), o Hero laikrodis tampa `60.sp` `FontWeight.Normal` (`letterSpacing = (-2.0).sp`, `92 % onSurface`).
