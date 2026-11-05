# WohnPit 2.0.1 – Statistiken auf den Anzeigehandys

Livewerte kamen an, während Solarertrag, Verbrauch und Diagramme leer blieben. Bei einem langsamen ersten Abgleich konnten neue Messwerte den noch laufenden Abruf überholen: Das vollständige Ergebnis wurde verworfen und die Warteschlange zurückgesetzt. Dadurch wurde wieder dieselbe erste Kachel geladen, während weitere Diagramme warteten.

## Korrektur

- Vollständige erste Ergebnisse für dieselbe Anlage und denselben Zeitraum werden sichtbar. Wenn inzwischen neue Messwerte vorliegen, erscheint der Hinweis **Letzter Stand · neue Daten werden geladen …**. Danach wird weiter aktualisiert.
- Neue Livewerte entfernen andere wartende Diagramme nicht mehr. Wiederholte Abfragen derselben Kachel werden zusammengefasst, ohne ihre Position in der Warteschlange zu verändern.
- Ein Anlagenwechsel oder ein anderer Archivlauf verwirft weiterhin unpassende ausstehende Antworten. Messwerte anderer Anlagen, Kennzahlen oder Zeiträume werden nicht übernommen.
- Fehler und fehlende Messwerte bleiben ausdrücklich erkennbar; eine fehlende Statistik wird nicht als gemessene Null dargestellt.

## Installieren

**Auf S20/S10 beziehungsweise den Anzeigehandys über WohnPit 2.0.0 installieren. Nicht deinstallieren, keine Daten löschen und nicht neu koppeln.** Zentrale mit WohnPit 2.0.0 kann weiterlaufen; die Korrektur betrifft die Statistikabrufe in der Anzeige. Die gemeinsame 2.0.1-APK enthält weiterhin alle drei Betriebsarten.

Ein bereits abgeschlossener Umzug muss nicht wiederholt werden. Wer noch die frühere WohnPit-Zentrale verwendet, beachtet weiterhin die [private Umzugsanleitung](https://github.com/Tim-Gurke/WohnPit/blob/main/docs/ZUSAMMENFUEHRUNG.md). Persönliche Umzugsdateien, Kennwörter und Geräteschlüssel werden nicht veröffentlicht.

[Statistiken prüfen und Fehler eingrenzen](https://github.com/Tim-Gurke/WohnPit/blob/main/docs/ANZEIGE-STATISTIK.md) · [Vollständige Versionshistorie](https://github.com/Tim-Gurke/WohnPit/blob/main/CHANGELOG.md)

## Prüfungen und Grenzen

Die Regression wurde zunächst mit der bisherigen Lesebrücke reproduziert: Der erste vollständige Statistikstand blieb leer und vier wartende Kennzahlen führten wiederholt nur zur ersten Abfrage. Dieselben Tests bestehen mit der Korrektur. Zusätzliche Prüfungen decken Anlagenwechsel, Archivlaufwechsel, verzögerte Antworten, Fehler, unbekannte Werte und die begrenzte Warteschlange ab.

Der Browserlauf prüft den ersten Aufbau des tatsächlichen Cockpits mit fortlaufend wechselnder Messdatenrevision und verzögerten Antworten, anschließend Diagramme, Tagesarchiv, echte Nullwerte, Aktualisierungsfehler und beide Farbschemata. Alle verwendeten Messwerte und Geräte sind erfunden. Android-Tests, Lint, Build und die vorhandenen Archiv-/Verbundprüfungen gehören zur automatischen Freigabe.

Ein Test mit den persönlichen Daten und den tatsächlichen Funkverbindungen auf G5, S20 und S10 wurde hier nicht durchgeführt.
