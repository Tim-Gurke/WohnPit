# Eine App für Zentrale und Anzeige – Entwurf vom 05.11.2026

Status: bevorzugter Vorschlag zur Prüfung, noch keine Zusammenführung oder freigegebene Umstellung. Die laufenden Versionen bleiben gültig.

## Bedienung

Eine gemeinsame APK auf G5, S20 und S10 installieren. Beim ersten Start fragt die App „Wie möchtest du dieses Handy nutzen?“ und bietet zwei klar beschriebene Rollen:

| Rolle | Aufgabe | Einrichtung |
|---|---|---|
| Zentrale | Geräte auslesen, Messdaten und Gas-/Wasserjournal speichern, Wetter einschließlich Rückblick laden, Anzeigehandys versorgen | Geräte einrichten oder Daten übernehmen, Aufzeichnung starten, Kopplungscode anzeigen |
| Anzeigehandy | Zentrale koppeln, Kacheln und Diagramme anzeigen, berechnen und lernen, Anlage und Einträge bedienen | Kopplungscode scannen oder eingeben, eigenes Layout verwenden |

Die Rollenauswahl erfolgt beim ersten App-Start. Für beide Rollen wird dieselbe APK installiert. Auf einem bereits eingerichteten Handy wird die bisherige Rolle erhalten und der Assistent nicht ungefragt wiederholt. Die eigenständige direkte Watt’n’Pit-Nutzung bleibt als gesondert zu prüfender Betriebsmodus erhalten.

Ein späterer Rollenwechsel gehört in die Einstellungen. Vorher laufende Aufzeichnung und Geräteverbindungen geordnet beenden; Kopplung und Zuständigkeiten nicht versehentlich überschreiben. Messdaten und lokale Layouts erhalten, Folgen des Wechsels verständlich anzeigen. Kein stilles Aktivieren einer zweiten Zentrale.

## G5 und Berechtigungen

Im Zentralenmodus die schlanke native WohnPit-Oberfläche weiterverwenden. Kein Dashboard-WebView und keine Anzeige-Berechnungen im Hintergrund starten. Auch bei gesperrtem Display müssen Aufzeichnung, Wetterabruf, Archivplaner und WLAN-Bereitstellung weiter funktionieren. Der gemeinsame APK-Inhalt allein ist kein Beleg für höheren laufenden Ressourcenverbrauch; RAM, CPU und Akku auf dem echten G5 vor/nach dem Umbau messen.

Im Anzeigemodus keine lokale Bluetooth-Aufzeichnung oder Archivdienste allein durch App-Start aktivieren. Notwendige Berechtigungen jeweils dann anfordern, wenn die gewählte Funktion sie benötigt.

## Vorhandene Installationen übernehmen

Aktueller Quellstand: Watt’n’Pit 1.9.0 verwendet die Android-App-Kennung `de.wattpit.app`, WohnPit 0.0.4 `de.wohnpit.app`; beide besitzen getrennte Updatesignaturen und Datenspeicher. Eine gemeinsame APK kann deshalb nicht ohne geplante Übernahme beide bisherigen Apps zugleich als gewöhnliches Update ersetzen.

Bevorzugt prüfen: Watt’n’Pit mit bestehender Kennung und Signatur als gemeinsame App fortführen, den schlanken Zentralenmodus aus WohnPit übernehmen und für bestehendes WohnPit eine gezielte Migrationsversion anbieten. Alternativen erst anhand von Updatefähigkeit, Signaturen und Datenübernahme entscheiden. Keinen neuen App-Namen, keine neue Kennung und kein Abschalten der alten Apps ohne Entscheidung festlegen.

Die Migration muss Einstellungen, Messwertarchive, Anlagen-/Standorthistorie, Gas-/Wasserjournal, Wetterrückblick, Lernstände sowie Geräte- und Verbundzuordnung berücksichtigen. Vorhandene Exporte sind nicht automatisch eine vollständige Wiederherstellung: Geräteschlüssel und Kopplungsgeheimnisse werden nicht in gewöhnliche öffentliche oder unverschlüsselte Sicherungen aufgenommen. Für solche Daten einen geschützten Übergabeweg oder eine klar geführte Neueinrichtung vorsehen. Erst nach bestätigter Übernahme und Funktionsprüfung die alte Zentrale beenden. Die alte App nicht automatisch deinstallieren oder ihre Daten löschen.

## Umsetzung in prüfbaren Schritten

1. Rollen-, Dienst- und Speichermatrix beider Quellstände vergleichen. Gemeinsame Bausteine vereinheitlichen, ohne vorhandene Protokolle oder Datenformate nebenbei zu ändern.
2. Daten- und Signaturstrategie einschließlich geschützter Schlüsselübernahme und Rückweg festlegen. Migrationsablauf mit Bestandsdaten erproben.
3. Erststart-Assistent und dauerhaft gespeicherte Rolle bauen. Vorhandene eigenständige Nutzung und lokale Layouts erhalten.
4. Zentralenmodus mit nativer Statusansicht und Anzeigemodus mit bestehendem Dashboard integrieren. Einrichtungs- und Rollenwechselabläufe prüfen.
5. Übergangsphase mit alten und neuen App-Versionen über den bestehenden Anlagenverbund testen. Kompatibilitätsmatrix und Update-Reihenfolge dokumentieren.
6. Auf G5, S20 und S10 prüfen: gesperrtes Display, Netzverlust, Neustart, Wetterlücken/Rückblick, Archive, Gas/Wasser, Lernen, Steuerung und bestehende ScootPit-BMS-Übernahme/Rückgabe. Keine doppelten Aufzeichnungen, Bluetooth-Verbindungen oder unerwarteten Dienste.
7. Nach Entscheidung veröffentlichen: eine signierte APK, beide Rollen mit sichtbaren Screenshots, ausführlicher Anleitung, Versionshistorie und höchstens drei veröffentlichten Releases je Repository. WohnPit-Repository als dokumentierte Herkunft/Migrationsstrecke erhalten, statt es vorschnell zu löschen.

Offene Entscheidung: ob und wann die Zusammenführung umgesetzt wird. Farben, Layoutübernahme und nachvollziehbarer Wetterrückblick im laufenden Watt’n’Pit-Update hängen davon nicht ab.
