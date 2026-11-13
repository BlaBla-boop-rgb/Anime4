# Anime Cosmetics 1.1.0: Riss-Auge und Wirbelhimmel

Dieses Projekt ist der Quellcode von Anime Cosmetics 1.0.0 (aus der .jar dekompiliert) mit zwei neuen Features.
Beide stammen aus der Fähigkeit „Autorität des Todes“ der Ronova-Mod 1.12.3.

## Neue Features

Alles wird im Menü geschaltet, das man mit **R** öffnet. Die Taste lässt sich in der Steuerung ändern („Cosmetics-Menü öffnen“).

**Riss-Auge**: Das ist der Riss aus Ronova: das große Auge mit Feuer, die vier kleinen Augen, Ranken, Lichtströme, Aura und Funken. Er hängt fest im Norden am Himmel, 40 Blöcke entfernt und 24 Blöcke über deinen Augen. Wie die Sonne behält er diese Stelle, wenn du läufst oder dich drehst, und ist so immer in deiner Nähe. Das Auge schaut dich an. Beim Einschalten reißt er auf, beim Ausschalten schließt er sich.

**Wirbelhimmel**: Das sind die purpurnen Wolken, die sich über dir zu einem dunklen Auge zusammenziehen, plus die rote, abgedunkelte Atmosphäre. Die Vanilla-Wolken werden ausgeblendet. Der Wirbelhimmel funktioniert unabhängig vom Riss-Auge.

Beide Features gibt es nur in der Oberwelt. Sie sind standardmäßig aus.

## Geänderte und neue Dateien

Neu:
- `fx/RiftFx.java`: der Riss (Port von Ronovas `DeathAuthorityFx`, ohne Portale, Zersplittern und Kino-Kamera) und die Steuerung (öffnen, halten, schließen)
- `fx/RiftVortex.java`, `fx/RiftAtmosphere.java`: Wolkenwirbel und rote Atmosphäre (Port)
- `fx/FireSim.java`, `fx/DeathRiftEyes.java`: Feuersimulation und die Daten der kleinen Augen (1:1 aus Ronova)
- `fx/RiftTypes.java`: Render-Typen für den Riss. Sie sind getrennt von `FxTypes`, damit alle alten Features genau so aussehen wie vorher.
- `mixin/CloudRenderMixin.java`, `mixin/CloudFadeMixin.java`: blenden Wolken unter dem Wirbel aus (`require = 0`, also kein Absturz, falls eine andere Mod die Wolken ersetzt)
- `assets/animecosmetics/textures/entity/fx/rift_vortex*.png` (+ `.mcmeta`)
- `assets/animecosmetics/sounds/authority_open.ogg`, `authority_close.ogg` und `sounds.json`

Geändert:
- `Toggles.java`: neue Features `RIFT` und `RIFT_SKY`
- `AnimeCosmeticsClient.java`: zwei neue Tasten, `RiftFx.register()`, `RiftFx.tick()`, `RiftFx.clear()` beim Trennen
- `mixin/RenderPipelinesAccessor.java`: zusätzlicher Accessor `GLOBALS_SNIPPET` (für den Wirbel unter Iris-Shadern)
- `animecosmetics.mixins.json`: die beiden Wolken-Mixins
- `lang/de_de.json`, `lang/en_us.json`: Tasten, Feature-Namen und Untertitel
- `fabric.mod.json`: Version 1.1.0 und Beschreibung

## Unterschiede zu Ronova

- Ronovas eigene Partikel (`ronova:spark`, `ronova:ember`) und die Federstöße sind durch rote Vanilla-Staubpartikel ersetzt. Eigene Partikeltypen müssten registriert werden, und das geht in einer reinen Client-Mod nicht sauber, wenn man auf fremden Servern spielt.
- Der Riss liegt waagerecht hinter dir. Neigt man in Ronova beim Wirken den Kopf, kippt der Riss mit, hier nicht.

## Bauen

- `gradle.properties`: **`fabric_api_version` eintragen** (deine Fabric-API-Version für 26.3). Die übrigen Werte entsprechen dem Build der Ronova-Mod (Loom 1.18.4, Loader 0.19.5, MC 26.3).
- `.github/workflows/build.yml` baut mit Java 25 und Gradle 9.7.1 und lädt die .jar als Artifact hoch.
- Hast du das Original-Projekt noch, nimm am besten dein eigenes `build.gradle` und kopiere nur die Dateien aus der Liste oben hinein.

Hinweis: Der übrige Code ist dekompiliert. Er ist funktional gleich, aber Kommentare und ursprüngliche Variablennamen fehlen zum Teil (z. B. `var26`).
