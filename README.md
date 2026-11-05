# WohnPit 0.0.5 · Übernahmeversion
**[WohnPit 0.0.5 herunterladen](https://github.com/Tim-Gurke/WohnPit/releases/download/v0.0.5/WohnPit-v0.0.5.apk)** · [Ausführliche Gas-/Wasseranleitung](docs/GAS-WASSER.md) · [Versionshistorie](CHANGELOG.md)

## Neu: Private Sicherung vor dem Umzug

Die bisherige Zentrale erhält eine vollständige **verschlüsselte Übernahmesicherung** für den Wechsel zur gemeinsamen WohnPit-App. Aufzeichnungen, Wetter, Journal, Lernstände und Einstellungen sichern; Geräte bei Bedarf neu verknüpfen. **Die persönliche .wohnpit-Datei bleibt privat und gehört niemals auf GitHub.** [Schritt-für-Schritt-Anleitung](docs/changes-0.0.5.md).

## Datenbasis für Gas, Wasser und Wetter

WohnPit speichert Wasserbeschaffungen, Gaswechsel und bekannte Restvorräte. Es sammelt Wettervorhersagen sowie historische Temperatur-/Winddaten. **Watt’n’Pit ab 1.9.0 übernimmt sämtliche Einträge und Einstellungen sowie Berechnungen, Verbrauchslernen und Anzeige auf S20/S10.** Reichweiten erscheinen dort beispielsweise als **1 Tag 17 Stunden**. [Alle Änderungen und Prüfgrenzen](docs/changes-0.0.4.md).

**Update-Reihenfolge:** zuerst WohnPit 0.0.4, danach Watt’n’Pit 1.9.0. Vorhandene Installationen aktualisieren; Messdaten und Kopplungen bleiben erhalten. ScootPit benötigt hierfür kein Update.

## Screenshots der Zentrale

Android-8-Emulator ohne eingerichtete Geräte oder persönliche Messdaten. Die Versorgungsberechnungen und deren Screenshots stehen in der [Gas-/Wasseranleitung](docs/GAS-WASSER.md).

| Übersicht | Journal- und Wetterstatus |
|---|---|
| ![WohnPit 0.0.4 – native Statusübersicht](https://raw.githubusercontent.com/Tim-Gurke/WohnPit/assets/wohnpit-0.0.4/docs/images/wohnpit-0.0.4-overview.png) | ![WohnPit – Gas-/Wasserjournal und Wetterstand](https://raw.githubusercontent.com/Tim-Gurke/WohnPit/assets/wohnpit-0.0.4/docs/images/wohnpit-0.0.4-supplies.png) |

## Zusammenpassende Versionen

| WohnPit | Watt’n’Pit | Nutzung |
|---|---|---|
| 0.0.4 | ab 1.9.0 | Energie sowie gemeinsame Gas-/Wassereinträge, Wetter und Versorgungsauswertung |
| 0.0.4 | 1.8.x | Bestehende Energiedaten und bisherige Bedienung; Gas/Wasser nach Anzeige-Update |
| 0.0.3 | 1.9.0 | Bestehende Energienutzung; Gas-/Wassererfassung verlangt zuerst das Zentralen-Update |

Je Repository bleiben die drei neuesten Releases veröffentlicht. Die vollständige dokumentierte Versionshistorie bleibt erhalten.


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
- Nach einer ersten Prüfung 15 Sekunden nach Aufzeichnungsbeginn werden fehlende Archivdaten standardmäßig stündlich geprüft. Unter Energiegeräte → Automatischen Abruf einstellen sind 15 Minuten, 30 Minuten, stündlich oder nur manuell wählbar. Eine zusätzliche Geräteverbindung entsteht nur bei fehlenden Daten oder fälligen Geräteinformationen. Auch nach einem erfolgreichen oder gescheiterten Abruf wird das gewählte Intervall eingehalten; nach Abbruch mindestens 30 Minuten. Manuelles Nachladen bleibt möglich. Bluetooth-Livewerte, WLAN-Anzeige und BMS-Übergabe bleiben davon unabhängig.
- Eigenes App-Symbol: ein mintfarbenes Haus mit Akku kennzeichnet die Zentrale; Androids einfarbige Symbole erhalten ebenfalls das Hausmotiv.
- Nativer App-Kopf mit dem Zentralen-Symbol, WohnPit-Schriftzug und Versionsnummer.
- Statusfarben und passende Schaltflächen: Bei laufender Aufzeichnung ist Starten ausgegraut, Pausieren verfügbar. Ein Archivabruf lässt sich nur während eines laufenden Abrufs abbrechen; ohne eingerichtete Geräte ist das Nachladen gesperrt. BMS-Neuverbindung und Anzeige-Kopplung richten sich ebenfalls nach dem aktuellen Zustand.
- Kleine Diagnose mit höchstens 120 gespeicherten Betriebsereignissen; zusätzlich die vorhandene Archiv- und BMS-Übergabediagnose.

Eine konkrete Einsparung von RAM, CPU oder Akkustrom auf dem G5 ist vor dem Vergleich auf dem echten Gerät noch nicht gemessen.

## BMS-Übergabe

Die vorhandenen BMS-Bausteine `RollerHandover`, `RollerBle`, `ScootLink`, `RollerStore` und `RollerLedger` bleiben unverändert aus Watt’n’Pit übernommen. Der bestehende `LanSync`-Transport ergänzt ab 0.0.4 das Gas-/Wasserjournal im Bundle und in der Konfigurationsrevision; seine Herkunft und die geprüfte Änderung stehen in `UPSTREAM.json`. Es wird kein neues BMS-Übergabeprotokoll eingeführt.

Auf dem Fahrhandy bleibt Watt’n’Pit die bestehende, signaturgeprüfte Brücke zu ScootPit. WohnPit übernimmt die Zentrale am anderen Ende des vorhandenen verschlüsselten Anlagenverbunds. ScootPit und die Brücke auf dem S20 werden nicht geändert.

WohnPit 0.0.1 ist für diese getrennte Zentrale vorgesehen. Eine direkte lokale ScootPit-Kopplung mit der neuen WohnPit-App-Kennung auf demselben Handy wird nicht angeboten; die bestehende lokale Kopplung bleibt in Watt’n’Pit.

## Wechsel von der bisherigen Zentrale

Alle Exporte auf der bisherigen G5-Zentrale in Watt’n’Pit erstellen. Auf dem Anzeigehandy sind die Verlaufs- und Diagnoseexporte absichtlich gesperrt.

| In Watt’n’Pit auf dem G5 antippen | Sicherungsdatei |
|---|---|
| Einstellungen → Einstellungen sichern → Einstellungen exportieren | Watt-n-Pit-Einstellungen.json |
| Einstellungen → Lokal. Ohne Konto. → CSV exportieren | Watt-n-Pit-Verlauf.csv |
| Einstellungen → Energieplanung → Mess- und Lerndiagnose exportieren | Watt-n-Pit-Diagnose-VERSION.zip |

Bei jeder Android-Dateiauswahl zum Beispiel den lokalen Ordner Download wählen und Speichern antippen. Die CSV enthält die gespeicherten Messwertzeilen aller Zeitbereiche. Erst die zusätzliche Diagnose enthält die vorhandenen historischen Anlagenzustände, archivierten Modelldaten sowie die gespeicherten Lernabschnitte und Prognosevergleiche. Die CSV allein erhält diesen Lernstand nicht.

1. Die alte Watt’n’Pit-Aufzeichnung in Übersicht über das Pausezeichen pausieren. Unter Einstellungen → Anlagenverbund → Geräte verknüpfen den Verbund beenden und die Nachfrage bestätigen. Die alte App und ihre Daten zunächst behalten.
2. WohnPit installieren. Unter Einstellungen und Daten zuerst „Watt’n’Pit-Einstellungen importieren“ mit der JSON-Datei und dann „Watt’n’Pit-Verlauf importieren“ mit der CSV-Datei ausführen.
3. SmartSolar und SmartShunt mit ihren bisherigen Bluetooth-Adressen und Instant-Readout-Schlüsseln einrichten. Die Einstellungssicherung enthält keine Geräteschlüssel oder Kopplungsgeheimnisse.
4. Das Roller-BMS mit seiner bestehenden Bluetooth-Adresse einrichten. Erweiterte Rollerwerte lassen sich anschließend wie bisher vom Anzeigehandy aus einstellen.
5. Bei pausierter WohnPit-Aufzeichnung und beendetem Archivabruf „Verbrauch und Lernstand übernehmen“ wählen und die Diagnose-ZIP aus der alten G5-App auswählen. Die Geräteadressen müssen zur bisherigen Anlage passen.
6. Aufzeichnung starten; Android-Berechtigungen erteilen. Unter Android 8–11 muss für die BLE-Suche der Standortdienst eingeschaltet sein.
7. In WohnPit „Anzeigehandy koppeln“ öffnen. Auf S20/S10 den bisherigen Anlagenverbund verlassen und mit dem WohnPit-Code verbinden.
8. Livewerte, historischen Verbrauch, Archivabruf und die bestehende BMS-Übernahme/Rückgabe mit ScootPit am Roller prüfen.

## Historischen Verbrauch und Lernstand nach einem Umzug ergänzen

WohnPit 0.0.3 über 0.0.1 oder 0.0.2 installieren, ohne die App zu deinstallieren. Geräte, Messwerte und die bestehende Kopplung bleiben dadurch erhalten; erneutes Verknüpfen der Anzeigehandys ist nicht erforderlich.

1. Auf dem G5 in der alten Watt’n’Pit-App Einstellungen → Energieplanung → Mess- und Lerndiagnose exportieren aufrufen. Die alte Aufzeichnung pausiert lassen. Watt’n’Pit 1.8.1 unterstützt diesen Export bereits.
2. In WohnPit die Aufzeichnung pausieren. Einen noch laufenden Archivabruf abwarten oder abbrechen.
3. Unter Einstellungen und Daten „Verbrauch und Lernstand übernehmen“ antippen und die eben gespeicherte ZIP-Datei auswählen.
4. Nach erfolgreichem Import die WohnPit-Aufzeichnung wieder starten und auf dem Anzeigehandy den Verlauf neu öffnen.

Der Import ergänzt vorhandene frühere Aufstellungen, archivierte Wetterwerte, Geräteprofile, beobachtete Roller-Ladeintervalle, gespeicherte Lernabschnitte und ursprüngliche Prognosevergleiche. Bereits bestätigte frühere Aufstellungen werden samt Wetterarchiv übernommen. Fehlende Wetterwerte einer bereits installierten identischen Bestätigung werden mit dem vorhandenen Wetter-Merge ergänzt. Bestehende Datensätze werden erhalten; wiederholtes Importieren erzeugt keine doppelten Messungen. Heutige Einstellungen und BMS-Zuständigkeit werden nicht durch alte Werte ersetzt. Die bisherige Verbrauchsberechnung bleibt unverändert. Für diesen Fix benötigen Watt’n’Pit und ScootPit kein Update.

Der Import übernimmt jetzt auch den vorhandenen Lernstand im unterstützten Diagnoseformat, einschließlich der älteren Version 1.6.10 mit demselben Version-2-Lernschema. Er ist weiterhin keine vollständige App-Wiederherstellung: Separate Tagesarchive und Roller-Bedieneinstellungen werden nicht kopiert. Zeiträume, für die auch die alte Diagnose keine ausreichenden Grundlagen enthält, bleiben als Lücken erkennbar.

### Wenn nach 0.0.2 nur noch wenige Lernstunden angezeigt werden

0.0.2 ergänzte die Verbrauchsgrundlagen, ließ gespeicherte Lernabschnitte und Prognosevergleiche aber aus. Deshalb konnten Messwerte vorhanden sein und trotzdem frühere Lernerfahrung fehlen. Es gibt keine Begrenzung auf 100 Stunden.

Nach dem Update auf 0.0.3 denselben Diagnoseexport bei pausierter Aufzeichnung erneut mit „Verbrauch und Lernstand übernehmen“ einlesen. Ein erneuter CSV-Import oder eine neue Kopplung ist nicht nötig. Danach die Aufzeichnung starten und Watt’n’Pit vollständig neu öffnen. Bereits vorhandene und überlappende neuere Lernabschnitte werden erhalten; alte Exporte erzeugen keine doppelt gezählten Lernstunden. Der Import meldet die tatsächlich hinzugekommenen Lernabschnitte und Prognosevergleiche. Die Zahl ausgewerteter Stunden wird weiterhin von der bestehenden Anzeige-Berechnung ermittelt, einschließlich ihrer Abdeckung und Ausreißerfilter.

Es lassen sich nur Lernabschnitte wiederherstellen, die im gewählten Export tatsächlich gespeichert sind. Die Auswertung auf dem Anzeigehandy bleibt dort; WohnPit führt für den Import keine neue Lernberechnung aus.

Watt’n’Pit kann weiterhin ohne Zentrale direkt mit den Victron-Geräten arbeiten. WohnPit läuft als eigene App daneben; bestehende Daten werden nicht automatisch zwischen den App-Speichern verschoben.

## Aufzeichnung und Archivauflösung

Die Zentrale empfängt Bluetooth-Livewerte laufend und speichert sie im eingestellten Intervall (standardmäßig 10 Sekunden). Der zusätzliche Gerätearchivabruf ergänzt Lücken; er ist von der WLAN-Aktualisierung der Anzeigehandys unabhängig.

Victron dokumentiert für die gespeicherten Trends von SmartSolar und SmartShunt folgende Stufen:

| Abstand der Messpunkte | Rückblick im Gerät |
|---|---|
| 1 Sekunde | etwa 2 Minuten, nur RAM |
| 30 Sekunden | etwa 2 Stunden |
| 5 Minuten | etwa 1 Tag |
| 30 Minuten | etwa 45 Tage |

Quelle: [VictronConnect-Änderungsprotokoll, v5.70](https://www.victronenergy.com/live/victronconnect:beta#v570_released_officially). Beim tatsächlichen Abruf liest der vorhandene Planer die vom Gerät gemeldeten Stufen; die Tabelle ersetzt keine Geräteabfrage.

Die standardmäßige stündliche Prüfung lässt übliche Empfangslücken noch innerhalb des etwa zweistündigen 30-Sekunden-Fensters nachladen. Sie sichert nicht sämtliche 1-Sekunden-Archivpunkte. Bereits in WohnPit gespeicherte Livewerte behalten ihre eigene Auflösung. Längere Ausfälle können nur mit den dann noch verfügbaren gröberen Archivwerten ergänzt werden.

## Bewusst übernommene Grundlagen

Das Datenbankschema, die genaue Messwertspeicherung, der Wetterabruf mit Cache und die Erfassung von Anlagen-/Wetterkontexten bleiben in 0.0.1 erhalten. Wetter kommt wie bisher von Open-Meteo, sofern ein Wetterort eingerichtet ist. Das verhindert einen zusätzlichen Umbau der bereits funktionierenden Anzeige- und Lernlogik.

Die geplante Verdichtung auf Tagesübersichten mit 15-Minuten-Abschnitten ist noch nicht aktiviert. In dieser ersten Version werden keine alten Rohdaten zugunsten neuer Zusammenfassungen gelöscht. Gas und Wasser werden ab 0.0.4 manuell erfasst; zusätzliche Füllstandssensoren folgen später.

## Bauen

JDK 17, Android SDK 35 und die vorhandene Gradle-Version 8.11.1 verwenden.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Ein Release wird mit `WOHNPIT_KEYSTORE` und `WOHNPIT_SIGNING_PASSWORD` signiert, Alias `wohnpit`. Ohne diese Variablen entsteht eine unsignierte Release-APK und eine gewöhnliche Debug-APK. Signierschlüssel gehören nicht in das Repository.

Die normale GitHub-Automatik prüft und baut. Ein Release wird gezielt über `releases/current.json` freigegeben. Die Veröffentlichungsroutine übernimmt ausschließlich die bereits signierte, geprüfte APK, kontrolliert ihre SHA-256-Prüfsumme und den erfolgreichen Android-Prüflauf und hängt APK und Prüfsumme an das GitHub-Release an. Signierschlüssel werden dafür nicht auf GitHub benötigt.

## Herkunft und Lizenz

Aus Watt’n’Pit von Tim Weißbach, Repository [Tim-Gurke/Watt-n-Pit](https://github.com/Tim-Gurke/Watt-n-Pit), Commit `7aff6c831df84bfd1f23b548ad674ecf739cbf0c` übernommen. Lizenz: MIT, siehe [LICENSE](LICENSE). Eine Dateiübersicht der übernommenen Protokoll- und Speicherbausteine steht in [UPSTREAM.json](UPSTREAM.json).

© Tim Weißbach.

