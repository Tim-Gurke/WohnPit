# AutarPie 3.0.0

WohnPit wird zum AutarPie-Cockpit für autarkes Leben: Energie, Speicher, E-Scooter, Modellbau, Gas und Wasser. Das Ressourcen-Kreislauf-Symbol verbindet Strom, Wasser und Wärme.

## Installation und Update-Reihenfolge

1. Auf der bisherigen Zentrale eine vollständige private Sicherung erstellen.
2. **Zuerst die Zentrale**, dann alle Anzeigehandys auf **3.0.0** aktualisieren. Im eigenständigen Betrieb genügt dieses eine Gerät.
3. Die neue APK über die bestehende gemeinsame WohnPit-/Watt’n’Pit-Installation installieren. **Nicht deinstallieren.** Paketkennung `de.wattpit.app`, bestehende Updatesignatur, Daten, Geräteverknüpfungen und ScootPit-Schnittstelle bleiben erhalten.
4. Nach dem Update AutarPie einmal öffnen und die gespeicherte Rolle kontrollieren. Einen bereits abgeschlossenen Umzug aus der alten separaten WohnPit-Zentrale nicht wiederholen.

Das GitHub-Repository bleibt `Tim-Gurke/WohnPit`; am Repository musst du nichts umstellen. Die bestehende ScootPit-Übergabe wird über dieselbe lokale Schnittstelle weitergeführt. Für die neuen gemeinsamen Einstellungen und Berechnungsgrundlagen sollen Zentrale und Anzeigen denselben Versionsstand 3.0.0 verwenden.

## Neu

- Übersicht für alle Anwendungen; Bereiche E-Scooter, Solar & Speicher, Gas/Wasser, Modellbau, Verlauf, Geräte und Einstellungen. „Bereiche“ bleibt oben erreichbar. Untere Kurzwahlen frei wählen, entfernen und ordnen.
- Eigene benannte Dashboards leer anlegen oder kopieren, umbenennen und nach Bestätigung löschen. Dieselbe Kachelauswahl und derselbe Editor für die einzelnen Anwendungen. Bestehende Raster und getrennte Hoch-/Queranordnungen bleiben erhalten.
- Zentrale Bereichsfarben, bewusste eigene Wertfarben je Kachel, App- und Kachelhintergründe mit einer oder zwei Farben und Verlaufwinkel 0–360°. Einzelne Kachelhintergründe lassen sich gesondert hervorheben. Zwei- oder dreifarbige Instrumente erhalten weiche Übergänge und einstellbare Grenzen.
- Helles und dunkles Cockpit; Designwahl in den Einstellungen. Größe nur im Dialog mit Vorschau und ausdrücklichem Speichern. „Abbrechen“ verwirft die Größenänderung. Entfernen einer Kachel fragt mit deren Namen nach und löscht keine Messdaten.
- Einstellungsbereiche als direkt anwählbare Kacheln. Eine gemeinsame Solareinrichtung für Standort, Richtung, Neigung, Verschattung, Module, Verkabelung, Regler und Grenzen. Solarkreise lassen sich umsortieren; ihre Kennung und historischen Daten werden dabei nicht getauscht.
- Kreise mit derselben Reglerkennung teilen sich ihre Strom-/Leistungsgrenze in der Prognose. Die Stromgrenze wird mit der Batteriespannung in eine Leistung umgerechnet; ohne Livewert gilt die eingetragene Ersatzspannung. Einzelne mobile Kreise bekommen direkt anklickbare Namen in der Wetterkarte.
- PV-Spannung separat von Batteriespannung; aktiver Liveabruf am gekoppelten SmartSolar. Lastausgang mit bestätigtem Zustand und gemessenem Strom, daraus berechneter Leistung und frei wählbarer Zuordnung.
- Optionale Rollerautomatik mit frei wählbarem Ladeziel, Wohnwagenreserve, Tagesziel, Überschussart, W-Schwellen, Wh-Prognosereserve und Verzögerung. Standardmäßig aus. Ladeziel und manuelles Ausschalten sperren die Wiederaufnahme bis zur erneuten Freigabe.
- 608PD-Anbindung für aktuelle Betriebsart, Akkuspannung, Akkustrom, Laufzeit, mAh und Zellspannungen. Private Verbindungskennung aus dem selbst ausgewählten Android-HCI-Fehlerbericht übernehmen; sie bleibt verschlüsselt am erfassenden Gerät und wird nicht mit der App veröffentlicht.
- 608PD: Beenden von Laden oder Powerbank-Modus; App-Start derzeit auf das tatsächlich beobachtete **3S-LiPo-Profil, 4,21 V/Zelle, 0,50 A** begrenzt und mit Profilbestätigung. Andere Profile und den Powerbank-Modus am Gerät starten. Wohnwagenladung über Powerbank → 608PD als externe Quelle einordnen; sie wird nicht als Solarertrag oder zusätzlicher Shuntverbrauch addiert.
- Zeitleiste der tatsächlich gespeicherten Solar-Ladephasen. Lücken bleiben unbekannt; keine erfundenen Zwischenphasen. Tagesarchiv mit kurzen Abdeckungsangaben, aufklappbaren Details und erhaltener horizontaler Scrollposition.
- Schlanke native Zentralenoberfläche mit Bereichswahl, Gerätezuständen, Aufzeichnung, Archiv, Lastausgang, Verbund und Sicherung. Bedienung der gemeinsamen Funktionen auch vom Anzeigehandy mit Rückmeldung der Zentrale.
- Autostart stellt den gespeicherten Betriebszustand nach Neustart oder App-Update wieder her. Pausierte Aufzeichnung bleibt pausiert. Je nach Android-Version und Hersteller sind erstes Entsperren, Bluetooth-/Hintergrundfreigaben und Benachrichtigungsberechtigungen nötig.

## Aufgabenverteilung

Die Zentrale sammelt, speichert und verteilt Grundlagen und führt Steueraufträge aus. Energieauswertungen, Prognosen und Lernen laufen auf den Anzeigehandys bzw. ohne Zentrale auf dem eigenständigen Gerät. Neue BMS-Ladeabschnitte werden auf der Zentrale als Rohgrundlagen gespeichert; Energie daraus berechnet die Anzeige.

Die Rollerautomatik erhält ihre Prognose von einem aktiven Anzeigehandy. Dessen Plan gilt höchstens eine Minute; nur ein Anzeigehandy führt den Plan zur selben Zeit. Ohne gültigen Plan wird eine automatische Ladung zum Ausschalten angefordert. Direkte Reserve-/Ladezielprüfungen laufen am erfassenden Gerät. Ein Schaltauftrag gilt erst mit tatsächlicher Regler-Rückmeldung als bestätigt. BLE-Ausfälle können eine Schaltung verzögern; die App behauptet dann keinen bestätigten Zustand.

## Erhaltenes Verhalten und Prüfgrenzen

Gas und Wasser lernen weiterhin aus Beschaffungen und Flaschenwechseln. Eine Vorratskorrektur korrigiert den Inhalt und die Reichweite, sie wird nicht als neuer Verbrauch gelernt. Alte Messungen, Lernstände und eigene Anordnungen werden nicht pauschal zurückgesetzt. Vollständige private Sicherungen enthalten sensible Verknüpfungen und gehören niemals in öffentliche Releases.

Geprüft werden Konfiguration, Migration, Berechnungen, echte TCP-/SQLite-Verbundabläufe, Browseroberfläche sowie Android-Build und Android-Oberfläche. Reale Funkverbindungen, Aufwachen des konkreten G5 und Schalt-/Ladevorgänge mit den vorhandenen Geräten sind damit nicht als getestet behauptet. Die neue Automatik bleibt bis zur bewussten Einrichtung aus.

[Einrichtung und Bedienung](AUTARPIE-3.md) · [Vollständige Versionshistorie](../CHANGELOG.md)
