# Arbeitsstand 3.0.1

Der Arbeitszweig ist `update/3.0.1`, Ausgangsstand `ff7f309`. Noch kein Release veröffentlicht.

Der freigegebene Umfang stammt aus der Sammlung vom 13.11.2026 (29 Punkte).
Diese Datei dokumentiert die Umsetzung und wird vor der Veröffentlichung mit den tatsächlichen Prüfungen abgeglichen.

- Eingebaut, Gesamtprüfung offen: Berechnungs-/Abrufentlastung, Diagrammlücken und frühere Korrekturen.
- Eingebaut, Gesamtprüfung offen: Wetterlayout, Rückschau, 16-Tage-Vorschau, optionaler 46-Tage-Trend.
- Eingebaut, Gesamtprüfung offen: selektive Ansichtssicherung, Zurücksetzen und Wiederherstellen.
- Eingebaut, Gesamtprüfung offen: Solaraufbau, Reglerzuordnung, Netto-/Gesamtstrom.
- Eingebaut, Gesamtprüfung offen: Rollerladung/Wirkungsgrad, Restladezeit, Zellspannungen, Reichweite.
- Eingebaut, Gesamtprüfung offen: alle unterstützten Lastausgangswerte und priorisiertes manuelles Schalten.
- Eingebaut, Gesamtprüfung offen: Formel-Kacheln mit sicherem Parser und unabhängigen Messgrundlagen.
- Eingebaut, Gesamtprüfung offen: dauerhafte Offline-Versorgungseinträge und aussagekräftiges Ladejournal.
- Eingebaut, Gesamtprüfung offen: aktuelle Farbe, App-Palette, eigene Palette und Dashboard-Design.
- Eingebaut, Gesamtprüfung offen: sofort sichtbare Kurzwahlreihenfolge, Ziehen, Auf-/Ab-Pfeile.
- Eingebaut, Gesamtprüfung offen: vereinfachtes Vektorlogo und abgeleitete App-Symbole.
- Eingebaut, Gesamtprüfung offen: BMS-Übergabe mit alten Funktionsständen, Quellen-/Statusbeschriftungen.
- Eingebaut, Gesamtprüfung offen: Menü-/Live-Position und Android-Zurück entlang der Menüebenen.
- Offen: Dokumentation, aktuelle Aufnahmen mit neutralen Daten, signierte APK, erfolgreiche CI und Veröffentlichung; danach höchstens drei Releases.

Hardwareprüfungen am persönlichen G5/S20/BMS sind hier nicht als durchgeführt bestätigt.

## Laufende Validierung

- 258 bestehende JavaScript-Tests und 7 neue Prüfgruppen bestanden.
- Alle Java-Produktionsdateien gegen vorhandene API-35-/ZXing-Bibliotheken mit neutralen R-/BuildConfig-Prüfstubs kompiliert. Dies ist keine APK und kein Android-Gerätetest.
- Lokaler Chromium kann wegen der hier gesperrten Socket-Erstellung nicht starten. Die Browser- und Android-Prüfungen müssen auf GitHub Actions laufen.
- Noch vor Veröffentlichung prüfen/abschließen: Formel-Verläufe, vollständige selektive Übernahme, Messwert-/Reglerzuordnung, Metadaten/Verlauf im Ladejournal, BMS-Rückgabe mit Netzfehlern und Priorität manueller Befehle. Alle 29 Punkte der autoritativen Sammlung erneut abgleichen.
- Laufende Prüfprotokolle liegen außerhalb der Quellliste in /tmp; endgültige Ergebnisse kommen in docs/changes-3.0.1.md.
