# Arbeitsstand 3.0.1

Ausgangsstand ist `ff7f309`; die Veröffentlichung wird aus dem vollständig geprüften Arbeitsstand erstellt. Umfang und Bedienung sind in [Änderungen 3.0.1](changes-3.0.1.md) dokumentiert.

Der freigegebene Umfang stammt aus der Sammlung vom 13.11.2026 (29 Punkte).
Diese Datei dokumentiert die Umsetzung und wird vor der Veröffentlichung mit den tatsächlichen Prüfungen abgeglichen.

- Implementiert: Berechnungs-/Abrufentlastung, Diagrammlücken und frühere Korrekturen.
- Implementiert: Wetterlayout, Rückschau, 16-Tage-Vorschau, optionaler 46-Tage-Trend.
- Implementiert: selektive Ansichtssicherung, Zurücksetzen und Wiederherstellen.
- Implementiert: Solaraufbau, Reglerzuordnung, Netto-/Gesamtstrom.
- Implementiert: Rollerladung/Wirkungsgrad, Restladezeit, Zellspannungen, Reichweite.
- Implementiert: alle unterstützten Lastausgangswerte und priorisiertes manuelles Schalten.
- Implementiert: Formel-Kacheln mit sicherem Parser und unabhängigen Messgrundlagen.
- Implementiert: dauerhafte Offline-Versorgungseinträge und aussagekräftiges Ladejournal.
- Implementiert: aktuelle Farbe, App-Palette, eigene Palette und Dashboard-Design.
- Implementiert: sofort sichtbare Kurzwahlreihenfolge, Ziehen, Auf-/Ab-Pfeile.
- Implementiert: vereinfachtes Vektorlogo und abgeleitete App-Symbole.
- Implementiert: BMS-Übergabe mit alten Funktionsständen, Quellen-/Statusbeschriftungen.
- Implementiert: Menü-/Live-Position und Android-Zurück entlang der Menüebenen.
- Veröffentlichung: signierte APK mit Original-Updatesignatur, zugehöriger erfolgreicher Prüflauf, neutrale Aufnahmen und Anleitung im Release. Der genaue geprüfte Commit steht im Veröffentlichungsmanifest.

Hardwareprüfungen am persönlichen G5/S20/BMS sind hier nicht als durchgeführt bestätigt.

## Validierung

Die Android-Pipeline enthält JavaScript-/Java-Tests, vollständigen Android-Build und Lint, SQLite-/TCP-Verbundprüfungen, Browserfälle sowie drei Emulatortests. Neue Fälle prüfen unter anderem getrennte Lastregler, fehlende Lastspannung, Messlücken, Formel-Zeitzuordnung, selektive und vollständige Wiederherstellung, Offline-Neustart, bestätigte BMS-Rückgabe und unveränderte Quellen-/Signaturgrenzen. Der Release verweist auf den zugehörigen erfolgreichen Lauf; dessen Prüfergebnis ist maßgeblich.
