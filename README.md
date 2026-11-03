# WohnPit 0.0.1

Die schlanke Android-Zentrale für den Wohnwagen. WohnPit übernimmt die vorhandene Aufzeichnung und den Anlagenverbund von Watt’n’Pit. Watt’n’Pit bleibt die Anzeige-App; ScootPit begleitet den Roller.

## Geräte und Aufgaben

| Gerät | Apps | Aufgabe |
|---|---|---|
| Moto G5 im Wohnwagen | WohnPit | Victron-Livewerte und Gerätearchive lesen, Daten speichern, Roller-BMS im Stand lesen, Daten im WLAN bereitstellen |
| S20 FE als Hotspot/Fahrhandy | Watt’n’Pit und ScootPit BMS | Energieanzeige, Auswertungen, Rollerfahrt und vorhandene BMS-Übergabe |
| S10 | Watt’n’Pit | Zusätzliche Anzeige und Bedienung |

Android 8.0 oder neuer. Die erste Version übernimmt den aktuellen Watt’n’Pit-Quellstand 1.8.2. Die Java-Paketnamen bleiben für die Wiederverwendung erhalten; die Android-App-Kennung lautet eigenständig `de.wohnpit.app`.

## Was schlanker wird

- Kleine native Status- und Einrichtungsansicht; kein WebView und keine Dashboard-Dateien im APK.
- Die Ansicht aktualisiert sich nur, solange sie sichtbar ist.
- Die lokale Statusansicht fragt keine historischen Statistiken oder Diagramme ab.
- Aufwendige Auswertungen erfolgen über das vorhandene Verfahren auf den Watt’n’Pit-Anzeigehandys.
- Die Kamera- und Barcode-Scanner-Oberfläche entfällt. Die Zentrale zeigt nur den bestehenden Kopplungs-QR-Code an.
- Der vorhandene Archivplaner kann fehlende Daten auch bei geschlossener Statusansicht nachladen.
- Kleine Diagnose mit höchstens 120 gespeicherten Betriebsereignissen; zusätzlich die vorhandene Archiv- und BMS-Übergabediagnose.

Eine konkrete Einsparung von RAM, CPU oder Akkustrom auf dem G5 ist vor dem Vergleich auf dem echten Gerät noch nicht gemessen.

## BMS-Übergabe

Die vorhandenen Produktionsdateien `RollerHandover`, `RollerBle`, `ScootLink`, `RollerStore`, `RollerLedger` und `LanSync` werden unverändert aus Watt’n’Pit übernommen. Es wird kein neues Übergabeprotokoll eingeführt.

Auf dem Fahrhandy bleibt Watt’n’Pit die bestehende, signaturgeprüfte Brücke zu ScootPit. WohnPit übernimmt die Zentrale am anderen Ende des vorhandenen verschlüsselten Anlagenverbunds. ScootPit und die Brücke auf dem S20 werden nicht geändert.

WohnPit 0.0.1 ist für diese getrennte Zentrale vorgesehen. Eine direkte lokale ScootPit-Kopplung mit der neuen WohnPit-App-Kennung auf demselben Handy wird nicht angeboten; die bestehende lokale Kopplung bleibt in Watt’n’Pit.

## Wechsel von der bisherigen Zentrale

1. In Watt’n’Pit auf dem G5 Einstellungen und Verlauf exportieren. Die alte App und ihre Daten zunächst behalten.
2. Die alte Watt’n’Pit-Aufzeichnung pausieren und ihren Anlagenverbund beenden, damit nicht zwei Zentralen gleichzeitig das BMS anfordern.
3. WohnPit installieren. Die Einstellungen und die vollständige Watt’n’Pit-Verlaufs-CSV importieren.
4. SmartSolar und SmartShunt mit Bluetooth-Adresse und Instant-Readout-Schlüssel einrichten. Aus Sicherheitsgründen enthält die vorhandene Einstellungssicherung keine Geräteschlüssel und keine Kopplungsgeheimnisse.
5. Das Roller-BMS mit seiner bestehenden Bluetooth-Adresse einrichten. Erweiterte Rollerwerte lassen sich anschließend wie bisher vom Anzeigehandy aus einstellen.
6. Aufzeichnung starten; Android-Berechtigungen erteilen. Unter Android 8–11 muss für die BLE-Suche der Standortdienst eingeschaltet sein.
7. In WohnPit „Anzeigehandy koppeln“ öffnen. Auf S20/S10 den bisherigen Anlagenverbund verlassen und mit dem WohnPit-Code verbinden.
8. Livewerte, Archivabruf und die bestehende BMS-Übernahme/Rückgabe mit ScootPit am Roller prüfen.

Watt’n’Pit kann weiterhin ohne Zentrale direkt mit den Victron-Geräten arbeiten. WohnPit läuft als eigene App daneben; bestehende Daten werden nicht automatisch zwischen den App-Speichern verschoben.

## Bewusst übernommene Grundlagen

Das Datenbankschema, die genaue Messwertspeicherung, der Wetterabruf mit Cache und die Erfassung von Anlagen-/Wetterkontexten bleiben in 0.0.1 erhalten. Wetter kommt wie bisher von Open-Meteo, sofern ein Wetterort eingerichtet ist. Das verhindert einen zusätzlichen Umbau der bereits funktionierenden Anzeige- und Lernlogik.

Die geplante Verdichtung auf Tagesübersichten mit 15-Minuten-Abschnitten ist noch nicht aktiviert. In dieser ersten Version werden keine alten Rohdaten zugunsten neuer Zusammenfassungen gelöscht. Wasserstand, Gasfüllstand und weitere Sensoren folgen später.

## Bauen

JDK 17, Android SDK 35 und die vorhandene Gradle-Version 8.11.1 verwenden.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Ein Release wird mit `WOHNPIT_KEYSTORE` und `WOHNPIT_SIGNING_PASSWORD` signiert, Alias `wohnpit`. Ohne diese Variablen entsteht eine unsignierte Release-APK und eine gewöhnliche Debug-APK. Signierschlüssel gehören nicht in das Repository.

Die GitHub-Automatik prüft und baut. Sie veröffentlicht keine Releases automatisch.

## Herkunft und Lizenz

Aus Watt’n’Pit von Tim Weißbach, Repository [Tim-Gurke/Watt-n-Pit](https://github.com/Tim-Gurke/Watt-n-Pit), Commit `7aff6c831df84bfd1f23b548ad674ecf739cbf0c` übernommen. Lizenz: MIT, siehe [LICENSE](LICENSE). Eine Dateiübersicht der übernommenen Protokoll- und Speicherbausteine steht in [UPSTREAM.json](UPSTREAM.json).

© Tim Weißbach.

