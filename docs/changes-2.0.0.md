# WohnPit 2.0.0 – Zentrale und Cockpit in einer App

WohnPit führt Watt’n’Pit und die bisherige WohnPit-Zentrale zusammen: ein lokales Android-Cockpit für **Energie, Gas und Wasser**, geeignet für Wohnwagen, Ferienhaus, Gartenhaus und Anlagen mit passenden Victron-Datenquellen – auch am Balkon.

## Eine App, drei Betriebsarten

- **Zentrale:** schlanke native Konsole, Aufzeichnung, Gerätearchive, Wetter und gemeinsame Datenbasis. In diesem Modus wird kein Dashboard-WebView erstellt.
- **Anzeigehandy:** mit der Zentrale koppeln, Kacheln und Diagramme anzeigen, Verbrauch berechnen und die Anlage bedienen. Eine noch nicht gekoppelte Anzeige startet keine lokale Victron-Aufzeichnung.
- **Eigenständig · Victron direkt:** ohne zweite Zentrale direkt mit den Geräten verbinden. Aufzeichnung, Wetter, Berechnung und Anzeige laufen auf demselben Handy.

Betriebsart beim Erststart wählen und später in den Einstellungen ändern. Bestehende Rollen werden aus Watt’n’Pit übernommen. Vor einem Wechsel Aufzeichnung und Archivabruf pausieren; vorhandene Rollen-Kopplungen werden für die Rückkehr lokal gespeichert.

## Farben, Layout und Wetterrückblick

- Zentrale Messwertfarben mit sichtbaren, eingebbaren und kopierbaren Hexcodes. Wasser **#4C65E0**, Gas **#C2C200** in Kachelauswahl, Instrumenten, Zahlen und Diagrammen. Bewusste Kachelausnahmen bleiben möglich. Solar orange, Akku grün und berechnete Akkubilanz hellblau bleiben erhalten.
- Die ausdrücklich gewünschte Anordnung mit **20 Kacheln in beiden Ausrichtungen** wird **einmal übernommen**. Damit wird die bisherige lokale Anordnung beim Update ersetzt. Falls gewünscht vorher das alte Layout exportieren; anschließende Änderungen bleiben erhalten. Wasserskala 40 L, Gasskala 11 kg.
- Unter **Gas / Wasser** den tatsächlich gespeicherten Wetterrückblick prüfen: Zeitraum, Ort, vorhandene und fehlende Tageswerte sowie Temperatur, Wind und Quelle. Aktueller Abrufzeitpunkt und Vorhersagen gelten nicht als Beleg vollständig geladener historischer Tage.
- Vorhandenes Gas-/Wasserjournal, Bedarfsschätzungen und Reichweiten in **Tagen und Stunden** bleiben erhalten.

## Sicher umziehen

**Watt’n’Pit auf S20/S10:** WohnPit 2.0.0 über die bestehende Watt’n’Pit-Installation installieren; die technische Kennung und Updatesignatur bleiben erhalten. Nicht vorher deinstallieren. Daten und Kopplungen bleiben im selben App-Speicher.

**Bisherige WohnPit-Zentrale:** zuerst **WohnPit 0.0.5 · Übernahme** aktualisieren und eine vollständige verschlüsselte **.wohnpit-Sicherung** erstellen. Danach die gemeinsame App installieren und die Sicherung in deren Einstellungen importieren. Prüfung abwarten, App schließen und erneut öffnen. Erst dann werden Datenbanken und Einstellungen übernommen; der vorherige Zielzustand bleibt intern gesichert. Nach Kontrolle die neue Zentrale starten. Geräte und Anzeigen bei Bedarf neu koppeln. Die Übernahme-App bis zur erfolgreichen Kontrolle behalten.

**Die persönliche .wohnpit-Datei enthält private Schlüssel und Verknüpfungen und darf niemals öffentlich werden – auch verschlüsselt nicht.** Sie gehört nicht auf GitHub, in Releases, Prüfartefakte oder auf die Projektseite. Das Sicherungskennwort wird nicht gespeichert. Öffentliche APKs enthalten ausschließlich den App-Code; alle veröffentlichten Beispiele sind erfunden.

[Ausführliche Installation und Umzugsanleitung](https://github.com/Tim-Gurke/WohnPit/blob/main/docs/ZUSAMMENFUEHRUNG.md) · [Farben und Wetter](https://github.com/Tim-Gurke/WohnPit/blob/main/docs/MESSWERTFARBEN-WETTER.md) · [Versionshistorie](https://github.com/Tim-Gurke/WohnPit/blob/main/CHANGELOG.md)

## Prüfung und Grenzen

Android-Unit-Tests, Lint, Release-Build, Frontend- und Archiv-/Verbundprüfungen gehören zur Freigabe. Im Android-Emulator werden falsches Kennwort, beschädigtes Sicherungsende, Prüfung vor dem Ersetzen, vollständige Datenbank-/Einstellungsübernahme und die drei Betriebsarten geprüft. Zentrale ohne WebView und Anzeige ohne lokale Aufzeichnung gehören dazu. Die Oberfläche wird zusätzlich mit neutralen Daten im aktuellen Chromium geprüft.

Die lokale Funktionsprüfung verwendete Android 9 im Emulator; der automatische Android-Prüflauf verwendet Android 8. Ein aktuelles Android-System-WebView ist für das Cockpit erforderlich. Echte Bluetooth-Verbindungen, WLAN-Empfang, Akkulaufzeit und der Umzug mit den persönlichen G5-Daten wurden hier nicht gemessen. Die neue Bezeichnung erweitert nicht automatisch die Gerätekompatibilität auf beliebige Wechselrichter oder Stromzähler. Der Verbund bleibt im lokalen WLAN.

ScootPit benötigt wegen erhaltener Schnittstelle und Signatur kein Update. Die alten Quellstände und Versionshistorien bleiben bestehen. Es werden je Repository nur die drei neuesten Releases veröffentlicht.
