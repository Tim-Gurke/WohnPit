# Versionshistorie

Die Dokumentation bleibt vollständig erhalten, auch wenn ältere GitHub-Releases gemäß der Drei-Release-Regel entfernt werden.

## Projektpflege · 13.11.2026

Das bestehende GitHub-Repository heißt jetzt [`Tim-Gurke/AutarPie`](https://github.com/Tim-Gurke/AutarPie). Download- und Dokumentationslinks sind auf die neue Adresse umgestellt. Diese Änderung betrifft die Projektdokumentation; die veröffentlichte APK bleibt Version 3.0.0.

## Veröffentlichte Versionen

### 3.0.5 · Vorhandenen Verlauf vollständig fürs Verbrauchslernen abgleichen

Auch bei bereits importierten Lernabschnitten den älteren Rohverlauf einmalig abrufen und neu bewerten. Gespeicherte stündliche Einstrahlung am damaligen Standort und mit damaliger Geometrie wiederverwenden. Ausschlussgründe je Anwesenheitsprofil erhalten, ältere nicht zuordenbare Gründe getrennt zeigen; lokalen Archivumfang sichtbar machen. Erneuten historischen Lernabgleich und Diagnoseexport der gekoppelten Anlage auf Anzeigehandys unter Daten & Sicherung anbieten. Eigene Handydaten, Layouts und bestehende Erkenntnisse erhalten. [Update, Bedienung und Prüfgrenzen](docs/changes-3.0.5.md).

### 3.0.4 · Lerndaten erhalten, frühere Wetterkorrektur und manueller Relaisbetrieb

Geänderte Messdaten löschen nicht mehr unwiederbringlich frühere Lernabschnitte: betroffene Blöcke gezielt neu bewerten, nicht neu berechenbare Erkenntnisse erhalten und sichtbar aufschieben, Datengrundlage im offenen Dialog aktualisieren. Beobachtete Roller-Nutzungstage auch bei zentraler Erfassung auf den Anzeigen auswerten. Letzten BMS-Datenstand bei fehlender Reichweite mit Zeitpunkt erhalten; Steuerung weiterhin nur mit frischen Daten. Relaisbetrieb ausdrücklich manuell wählbar, direkter Ladeweg bleibt Standard; historischen Wirkungsgrad als Referenz übernehmen, Spulenmessung und geschätzten Ladebedarf unterscheiden. Vorläufige Gas-Temperaturkorrektur nach erstem geeigneten Flaschenabschnitt, spätere persönliche Modellprüfung. Symbol-Kurzwahlen, neue Darstellungsvorgabe für neue AutarPie-Installationen, Solarertrag und gemessener Abend-Akkustand in der Wetterrückschau. Android-10/11-Hintergrundberechtigung und BLE-Neustart verbessern den gespeicherten Autostart. [Installation, Bedienung und Prüfgrenzen](docs/changes-3.0.4.md).

### 3.0.3 · Dashboardfarben eindeutig zuordnen

Der Verlauf verwendet sein eigenes Design oder die gemeinsamen Vorgaben, unabhängig von der zuvor geöffneten Seite. Der Farbeditor bearbeitet die sichtbare Seite; Speichern und „auf alle Kacheln anwenden“ verändern weder das zuletzt geöffnete andere Dashboard noch seine Kachelanordnung. Diagramme verwenden die wirksamen Seitenfarben. Fehlgeschlagenes Speichern lässt sich für dieselbe Seite wiederholen. Mit tatsächlichen Seitenwechseln, Speicherung, Neustart und Hoch-/Queransicht gegen Rückfälle geprüft. Zentralen-Hotfix 3.0.2 bleibt enthalten. [Update und Prüfgrenzen](docs/changes-3.0.3.md).


### 3.0.2 · Zentrale reagiert wieder

Hotfix für den in 3.0.1 eingeführten Deadlock zwischen Reglerabfrage und Repository-Snapshot. Gerätekonfiguration ohne gegenseitige Monitorsperre lesen, Lastarchivierung außerhalb der gemeinsamen Livewertsperre ausführen und die native Regleranzeige im Hintergrund abfragen. Nachgestellter 3.0.1-Fehler, paralleler Regressionstest und zusätzlicher Android-Test für einen belegten Regler. Bestehende Daten, Kopplungen, Layouts und Updatesignatur bleiben erhalten. [Ursache, Installation und Prüfung](docs/changes-3.0.2.md).

### 3.0.1 · Anzeige, Messkacheln und Wiederherstellung

Begrenzte Neuberechnungen und stabile Diagramme; Wetterrückschau, 16-Tage-Vorschau und optionaler Langfristtrend; selektives Zurücksetzen/Wiederherstellen; Farbpaletten und Dashboard-Design; sichtbare Kurzwahlreihenfolge, Menü-/Live-Position und Android-Zurück. Neue Roller-/Zell-/Reichweiten-/Wirkungsgrad- und Formel-Kacheln, mehrere getrennte Regler-Lastausgänge, priorisierte Schaltwünsche, ausführliches Ladejournal und dauerhafte Offline-Versorgungseinträge. Bestätigte BMS-Rückgabe und zeitweilige Verbindungsfehler abgesichert. Präziseres schlankes Logo, Originalpaket und Updatesignatur erhalten. [Umfang, Update und Prüfgrenzen](docs/changes-3.0.1.md).

### 3.0.0 · AutarPie

Großes Cockpit-Update mit Ressourcen-Kreislauf-Symbol, frei wählbaren Bereichen, eigenen Dashboards, gemeinsamen Farben/Verläufen und bestätigter Kachel-/Größenbearbeitung. Native Zentralenoberfläche, Berechnung auf Anzeigen/eigenständigen Geräten, zeitlich begrenzte Überschusspläne, Lastausgang und 608PD-Grundintegration, gemeinsame Solareinrichtung mit stabilen Kreiskennungen und gemeinsamen Reglergrenzen, PV-Spannung, Phasenzeitleiste und Autostart. Paket und Updatesignatur bleiben erhalten. [Umfang, Update und Prüfgrenzen](docs/changes-3.0.0.md).


| Version | Änderungen |
|---|---|
| [2.0.3](docs/changes-2.0.3.md) | Vollständige Statistikstände bei wechselnden Grundlagen erhalten und aktualisieren; sichtbare Kacheln vor dem Lernabgleich laden. Fehler mit echter TCP-/SQLite-Strecke und tatsächlicher Oberfläche nachgestellt. |
| [2.0.2](docs/changes-2.0.2.md) | Gas-/Wasserbedarf in Flaschen und Kanistern, einstellbare Behältergrößen, sichtbarer Wetter-Lernstand und passende Symbole. WohnPit klar als Zusammenführung von Watt’n’Pit und der bisherigen Zentrale gekennzeichnet; öffentliche Vorgeschichte und Anleitungen hier erhalten. |
| [2.0.1](docs/changes-2.0.1.md) | Ersten Statistikabruf auf Anzeigehandys bei laufender Aufzeichnung repariert; vollständige Ergebnisse sichtbar behalten und wartende Diagramme gleichmäßig laden. |
| [2.0.0](docs/changes-2.0.0.md) | WohnPit und Watt’n’Pit zusammengeführt; Zentrale, Anzeigehandy und eigenständiger Victron-Betrieb, private Vollsicherung, gemeinsame Messwertfarben und belegter Wetterrückblick. |
| [0.0.5](docs/changes-0.0.5.md) | Private vollständige Übernahmesicherung für den Wechsel zur gemeinsamen WohnPit-App. |
| [0.0.4](docs/changes-0.0.4.md) | Gemeinsames Gas-/Wasserjournal, Wetterrückblicke, Temperatur-/Winddaten und Statusanzeige; Bedienung und Auswertung in Watt’n’Pit ab 1.9.0. |
| [0.0.3](docs/changes-0.0.3.md) | Gespeicherte Lernabschnitte und Prognosevergleiche aus unterstützten Watt’n’Pit-Diagnosen übernehmen. |
| [0.0.2](docs/changes-0.0.2.md) | Historische Verbrauchsgrundlagen und Anlagen-/Wetterkontexte beim Umzug ergänzen. |
| 0.0.1 | Eigenständige native Zentrale aus Watt’n’Pit 1.8.2: Messwerte, Archive, verschlüsselter Anlagenverbund und vorhandene BMS-Übergabe. |

Die frühere [Watt’n’Pit-Versionshistorie](docs/wattnpit-archiv/CHANGELOG.md) bleibt vollständig erhalten und wird ab WohnPit 2.0.0 hier gemeinsam fortgeführt.
