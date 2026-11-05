# WohnPit ab 2.0.0 – Zentrale und Cockpit in einer App

**Noch Watt’n’Pit 1.8.4 auf dem Anzeigehandy?** Direkt die aktuelle WohnPit-App (mindestens 2.0.1) über die vorhandene Installation installieren. Die Zwischenversionen 1.9.0 und 2.0.0 müssen nicht einzeln installiert werden. Die App-Kennung, Updatesignatur und bisherige Messwertdatenbank werden weiterverwendet. Vorher nicht deinstallieren. Beim ersten Wechsel wird die ausdrücklich gewünschte 20-Kachel-Anordnung einmal übernommen; bei Bedarf das bisherige Layout vorher exportieren. Der Umzug einer separaten WohnPit-Zentrale ist ein eigener Vorgang und hängt von deren tatsächlichem Versionsstand ab.

**Bereits WohnPit 2.0.0 installiert?** Für die Statistik-Korrektur genügt das Update auf 2.0.1 auf den Anzeigehandys. Ein abgeschlossener Umzug muss nicht wiederholt werden. [Update und Statistikprüfung](ANZEIGE-STATISTIK.md).

WohnPit führt die bisherigen Funktionen von Watt’n’Pit und der schlanken WohnPit-Zentrale in einer Anwendung zusammen. Der Name beschreibt ein lokales Cockpit für Energie, Gas und Wasser – im Wohnwagen, Ferienhaus, Gartenhaus oder an einer passenden Balkonanlage.

## Drei Betriebsarten

| Betriebsart | Datenquelle und Aufgabe |
|---|---|
| **Zentrale** | Victron- und weitere vorhandene Datenquellen lesen, Messwerte/Journal/Wetter speichern und im lokalen WLAN bereitstellen. Die schlanke native Konsole startet kein Dashboard-WebView. |
| **Anzeigehandy** | Eine Zentrale koppeln; Kacheln, Diagramme, Berechnungen, Lernen und gemeinsame Bedienung auf diesem Handy. Vor einer Kopplung startet keine lokale Victron-Aufzeichnung. |
| **Eigenständig · Victron direkt** | Ohne zweite Zentrale direkt mit den eigenen Victron-Geräten verbinden. Dasselbe Handy übernimmt Aufzeichnung, Wetterabruf, Berechnung und Anzeige. |

Bei einer neuen Installation die Betriebsart beim ersten Start wählen. Bestehende Watt’n’Pit-Installationen übernehmen ihre bisherige Verbundrolle; ohne Verbund starten sie eigenständig. Unter **Einstellungen → WohnPit auf diesem Handy → Betriebsart ändern** lässt sich die Rolle später wechseln. In der Zentralen-Konsole liegt derselbe Eintrag unter Einstellungen und Daten. Vorher Aufzeichnung und Archivabruf pausieren. Die bisherige Kopplung einer Rolle wird lokal für die Rückkehr erhalten; Messwerte und Layouts werden beim Wechsel nicht gelöscht. Ein noch nicht gekoppeltes Anzeigehandy führt zur Verbundeinrichtung.

Die Energiegeräte über die vorhandene Geräteeinrichtung hinzufügen. Im eigenständigen Betrieb benötigt dieses Handy die Bluetooth-Berechtigungen und für Wetter selbst Internet. Im Verbund benötigen Zentrale und Anzeigen dasselbe lokale WLAN; für die Datensammlung ist dann die Zentrale zuständig. Die App bietet dadurch keinen automatischen Fernzugriff über das Internet.

## Was erhalten bleibt

Solar-, Akku- und Verbrauchswerte, Gerätearchive, Energieplanung, zusätzliche Speicher, Wetter, das manuelle Gas-/Wasserjournal und seine Schätzungen gehören weiter zum Cockpit. Reichweiten bleiben in Tagen und Stunden lesbar. Wasser- und Gasfarben werden zentral mit Hexcodes verwaltet; die ausdrücklich gewünschte Kachelanordnung wird einmal übernommen. Siehe [Messwertfarben und Wetterrückblick](MESSWERTFARBEN-WETTER.md).

Die vorhandene ScootPit-Schnittstelle und die WLAN-/BMS-Übergabe bleiben erhalten. ScootPit braucht für diese Zusammenführung kein Update. Regler-/Shunt-Einstellungen und Firmware-Updates gehören weiterhin in VictronConnect.

Für das Cockpit ist ein aktuelles Android-System-WebView erforderlich. Die schlanke Zentralenansicht benötigt kein WebView. Android 8 oder neuer bleibt die Mindestversion; Bluetooth-Funktionen verlangen die für die Android-Version nötigen Berechtigungen.

## Bestehendes Watt’n’Pit auf S20/S10 weiterverwenden

Die gemeinsame WohnPit-App behält technisch die bisherige Watt’n’Pit-Updatekennung und ihre Signatur. **Die aktuelle WohnPit-Version (mindestens 2.0.1) über die vorhandene Watt’n’Pit-App installieren**, ohne vorher zu deinstallieren. Der sichtbare App-Name wird WohnPit. Vorhandene lokale Daten, Einstellungen und Verbundkopplungen bleiben dadurch im selben App-Speicher. Die ausdrücklich gewünschte Anordnung mit 20 Kacheln wird beim Update einmal auf diesem Gerät übernommen; spätere eigene Änderungen bleiben erhalten. Bei Bedarf das bisherige Layout vorher mit dem normalen Einstellungsexport sichern.

## Bisherige WohnPit-Zentrale umziehen

1. **WohnPit 0.0.5 · Übernahme** über die alte WohnPit-Zentrale installieren. Vorher nichts deinstallieren.
2. Aufzeichnung pausieren, Archivabruf beenden und einen Wetterabruf abwarten. Unter **Einstellungen und Daten → Vollständige Übernahmesicherung** eine lokale **.wohnpit-Datei** mit eigenem langem Kennwort erstellen. [Ausführliche Sicherungsanleitung](changes-0.0.5.md).
3. Nach erfolgreicher Sicherung die alte App **WohnPit · Übernahme** vollständig beenden: Android-App-Info öffnen und **Beenden erzwingen / Stopp erzwingen** wählen. Die App installiert lassen, aber während des neuen Zentralbetriebs nicht wieder öffnen. Das Pausieren der Aufzeichnung allein beendet ihren WLAN-Verbunddienst nicht; dieser darf nicht gleichzeitig mit der neuen Zentrale dieselbe Anlage anbieten. Falls auf dem G5 noch eine alte Watt’n’Pit-Aufzeichnung läuft, auch diese pausieren. Danach **die aktuelle gemeinsame WohnPit-App** auf diesem Handy installieren. Ist das alte Watt’n’Pit noch vorhanden, aktualisiert diese APK dessen Installation; sonst wird die gemeinsame App neu installiert. Die App **WohnPit · Übernahme** bleibt daneben zunächst erhalten.
4. In WohnPit **Übernahmesicherung einlesen** öffnen – beim Erststart oder später in den Einstellungen. Die .wohnpit-Datei wählen und das zugehörige Kennwort eingeben. Der Import ersetzt die Daten der Zielinstallation; deshalb einen dort benötigten eigenen Stand vorher ebenfalls vollständig sichern.
5. Die App prüft zuerst die Sicherung und ihre Datenbanken. Bei falschem Kennwort oder beschädigter Datei wird nichts eingespielt. Nach erfolgreicher Prüfung **Übernehmen und schließen** bestätigen und erneut öffnen. **Verwerfen** behält den bisherigen Stand. Erst der folgende Neustart übernimmt Datenbanken und Einstellungen; zusätzlich bleibt der vorherige Zielzustand intern gesichert. Bei einem unterbrochenen Einspielen erneut öffnen und die Meldung beachten.
6. Betriebsart **Zentrale** prüfen, Aufzeichnung starten und Anzeigehandys verbinden. Messwerte, Verlauf, Journal, Wetterrückblick und Lernstände vergleichen. Übernommene Kopplungen werden weiterverwendet; falls nötig Geräte und Anzeigehandys neu verknüpfen. Android-Berechtigungen, systemweite Bluetooth-Kopplungen und WLAN-Zugangsdaten lassen sich nicht als App-Einstellung übertragen; diese bei Bedarf erneut einrichten. Aufzeichnung und Lernstände haben Vorrang.
7. Die Übernahme-App bis zur Kontrolle vollständig beendet lassen und erst entfernen, wenn der neue Betrieb geprüft ist. Beide Zentralen niemals gleichzeitig betreiben oder aufzeichnen lassen. Die verschlüsselte Sicherungsdatei privat aufbewahren.

**Die Umzugsdatei enthält private Verknüpfungen und Zugangsdaten. Sie darf auch verschlüsselt niemals in öffentliche Repositories, Releases, Prüfartefakte oder auf die Projektseite geladen werden.** Die App lädt sie nicht auf einen Server. Das Sicherungskennwort wird nicht gespeichert. Öffentliche Downloads enthalten ausschließlich die signierte Anwendung; alle veröffentlichten Screenshots und Tests verwenden künstliche Daten.

## Geeignete Anlagen und Orte

WohnPit ist nicht auf Wohnwagen beschränkt. Entscheidend sind passende Datenquellen: unterstützte Victron SmartSolar-/SmartShunt-Geräte mit Bluetooth-Sofortanzeige und den richtigen Geräteschlüsseln. Auch am Ferienhaus, Gartenhaus oder Balkonkraftwerk lassen sich so die tatsächlich verfügbaren Werte verfolgen. Beliebige Wechselrichter, Netzstromzähler oder fremde Sensoren werden durch die neue Bezeichnung nicht automatisch unterstützt. Gas und Wasser lassen sich manuell erfassen.

## Herkunft und Versionshistorie

Das führende Projekt ist [WohnPit](https://github.com/Tim-Gurke/WohnPit). Die [bisherige Watt’n’Pit-Versionshistorie](https://github.com/Tim-Gurke/Watt-n-Pit/blob/main/CHANGELOG.md) und alle alten Quellstände bleiben erhalten. Die neue [WohnPit-Versionshistorie](../CHANGELOG.md) führt die gemeinsame Entwicklung fort. Je Repository bleiben nur die drei neuesten Releases veröffentlicht.

Die technischen Details der Übernahme und die Prüfgrenzen stehen in [WohnPit 2.0.0](changes-2.0.0.md). Ein Test im Emulator ersetzt keine Prüfung von Bluetooth, WLAN-Empfang oder Stromverbrauch am tatsächlichen G5.
