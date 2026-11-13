# AutarPie 3.0.1

Dieses Update setzt die gesammelten Korrekturen an Anzeige, Wetter, Kacheln, Sicherung, Versorgungseinträgen und Roller-Ladeweg um.

## Installation und kompatible Versionen

1. Eine vollständige private Sicherung anlegen. Zum Ausprobieren der Standardansicht zusätzlich die portable Einstellungssicherung unter Menü → Einstellungen → Daten & Sicherung verwenden.
2. **Zuerst die Zentrale, danach alle Anzeigehandys auf 3.0.1 aktualisieren.** Eigenständiger Betrieb benötigt nur dieses eine Update.
3. Die APK über die vorhandene gemeinsame App installieren, **nicht deinstallieren**. Paket `de.wattpit.app`, Original-Updatesignatur, Aufzeichnungen, Kopplungen und bestehende Layouts bleiben erhalten.
4. AutarPie einmal öffnen. ScootPit BMS **1.6.14** verwendet weiterhin dieselbe lokale Übergabeschnittstelle. Für diese Korrektur ist keine neue ScootPit-APK erforderlich. Der reale G5/S20/BMS-Verbund muss nach dem Update vor Ort geprüft werden.

Ein bereits abgeschlossener Umzug aus der separaten alten WohnPit-App wird nicht wiederholt. Das Hilfsprogramm `WohnPit-Uebernahme-v0.0.5.apk` bleibt für einen noch ausstehenden Altumzug verfügbar. Private Sicherungen niemals auf GitHub hochladen.

## Anzeige und Bedienung

- Berechnete Verläufe verwenden vorhandene Grundlagen wieder. Laufende Messrevisionen starten teure Auswertungen höchstens im Abstand von 30 Sekunden neu; ausdrückliches Nachladen, Quellenwechsel und beendete Importe behalten Vorrang. Bisherige Ergebnisse bleiben währenddessen mit ihrem Status sichtbar. Wetter- und Lernabgleiche werden davon getrennt begrenzt.
- Beim Wechsel zwischen Live- und Archivdaten werden geeignete, zeitlich anschließende Kurven verbunden. Echte Messlücken und Gerätewechsel bleiben unterbrochen; Spitzen werden nicht weggebügelt. SVG-Kurven erhalten runde Linienenden und Verbindungen.
- „Bereiche“ heißt **Menü** und steht am bisherigen Statusplatz. **Live steht links daneben.** Android-Zurück führt innerhalb der Einstellungen zurück durch die geöffneten Ebenen, anschließend zum Menü des bisherigen Dashboards.
- Die Kurzwahlreihenfolge ist bereits im Bearbeitungsdialog sichtbar. Ziehgriff und Auf-/Ab-Pfeile, kurze Hervorhebung und Speicherhinweis machen die Änderung nachvollziehbar.
- Das Ressourcen-Kreislauf-Logo und alle Android-Symbolvarianten verwenden schlankere, präzise Vektorformen.
- Quellen auf dem E-Scooter-Dashboard nennen das Roller-BMS. Der Wohnwagen-Akkustatus ist ausdrücklich als solcher bezeichnet; **BMS-Fahrfreigabe** und **Regler-Lastausgang** werden getrennt benannt.

## Farben und Ansicht wiederherstellen

- Farbauswahl beginnt beim aktuellen Farbton. App-Farben und bereits übernommene eigene Mischfarben sind direkt auswählbar; übernommene neue Mischfarben werden automatisch gemerkt.
- Optionales Design pro Dashboard für Seitenhintergrund, Kachelflächen, Akzent und gemeinsame Werte-/Diagrammfarbe. Einzelkachel-Ausnahmen bleiben erhalten; eine ausdrückliche Option übernimmt die Gestaltung für alle Kacheln dieser Seite.
- Unter **Daten & Sicherung → Ansicht sichern & wiederherstellen** Farben/Design, Kachelauswahl, Größen/Anordnung sowie Dashboards/Ansichtseinstellungen einzeln oder kombiniert zurücksetzen oder aus der Einstellungssicherung laden. „Alles auswählen“ übernimmt die vollständige Ansicht einschließlich Hoch-/Querlayouts, Größenwahl, Wettertrend-Option und Roller-Werteauswahl.
- Nur ausgewählte Bereiche werden übernommen. Ansicht zurücksetzen löscht keine Messungen, Lernwerte, Versorgungseinträge, Geräte oder Schlüssel. Der bisherige vollständige Einstellungsimport einschließlich Solaranlage ist weiterhin ausdrücklich auswählbar. Eine private Gesamtsicherung bleibt der Weg für den vollständigen Geräteumzug.

## Wetter und Solaranlage

- Gleiche Spalten und Skalenbreiten, feste rechte Wertespalte und dezente senkrechte Tagestrenner. Mobile Solarkreise sind ohne Rahmen grau beziehungsweise grün gekennzeichnet; ihre Schaltzustände bleiben auch als Text/Zustand erkennbar.
- Heute steht beim ersten Öffnen links. Bis zu drei vorhandene vergangene Wettertage sind durch Wischen nach links erreichbar; „Heute“ springt zurück. Rückschau sind archivierte/modellbasierte Wetterdaten, keine erfundenen Akku-Vergangenheitsprognosen.
- Normale Vorschau bis **16 Kalendertage einschließlich heute**, soweit der Anbieter Daten liefert. Unter Solaranlage kann zusätzlich ein **Wettertrend bis 46 Tage** aktiviert werden; standardmäßig aus. Wochenmittel mit Zeitstand, Quelle und Unsicherheit bleiben von der normalen Energieprognose getrennt. Fehlende Anbieterwerte bleiben unbekannt.
- Die gemeinsame Einrichtung zeigt **Module → Stränge → Regler → Batterie**, Modulzahl, Reihenschaltung, parallele Stränge und die gemeinsame Reglerbegrenzung. Mehrere Kreise an derselben Reglerkennung teilen die eingetragene Grenze.
- „Solar-Ladestrom“ heißt eindeutig **Regler → Batterie · netto**. **Solarstrom gesamt · Batterieseite** ergänzt den Regler-Laststrom; ungemessene aktive PWM-Anteile werden separat geschätzt und gekennzeichnet. Fehlende Anteile ergeben keinen scheinbar vollständigen Gesamtwert.

## Neue Roller-, Last- und Formel-Kacheln

- **Rollerladung** als kombinierte Kachel sowie einzelne Werte für Eingangsleistung, BMS-Leistung, aktuellen Wirkungsgrad und Verlustleistung, gemeinsam gemessene Eingangs-/Rollerenergie, Ladungswirkungsgrad und Energieverluste.
- Gemessener Wirkungsgrad benötigt die bestätigte ausschließliche Rollerzuordnung des Ausgangs und unabhängige, zeitlich passende Last- und BMS-Messungen. Er wird nicht aus dem eingestellten Verlustprofil zurückgerechnet. Unplausible Ergebnisse bleiben erkennbar; Werte werden nicht auf 100 % begrenzt. Gemessen wird der elektrische Ladeweg bis zum BMS, keine chemische Speicher- oder spätere Entladeeffizienz.
- **Restladezeit** bis zum Roller-Ladeziel aus geglätteter tatsächlicher BMS-Ladeleistung; bei Pause/fehlenden Grundlagen unbekannt. **Restreichweite in km** aus Rollerenergie und Wh/km im Rollerprofil, optional mit Akkureserve; ausdrücklich eine Schätzung.
- Tatsächlich gelesene **Einzelzellspannungen**, wählbare Zellnummer, Minimum, Maximum, Differenz und Zellübersicht. Keine Ableitung aus Gesamtspannung geteilt durch Zellzahl.
- Lastausgang: Zustand, lesbarer Betriebsmodus, Strom, Spannung, Leistung, Verbrauch und gemeldeter Abschaltgrund. Regler je Kachel auswählen, auch mehrere nebeneinander. Bis zu acht Solarregler und ein Batteriewächter sind konfigurierbar; die aktive Abfrage erfolgt nacheinander. Fehlende/alte Registerwerte bleiben unbekannt. Neue Lastverläufe und Wh verwenden die archivierten Messungen des ausgewählten Reglers; alte Lastenergie-Auswertungen behalten ihre gekennzeichnete Näherung aus Regler-Batteriespannung.
- **Eigene Formel** mit Variablen A–L, Geräte-/Messwertzuordnung, Eingangseinheiten und Vorschau. Vorlagen für Summe, Differenz, Verhältnis und Wirkungsgrad; freie Formeln mit `+ - * / ^`, Klammern, `MIN`, `MAX`, `BETRAG`, `RUNDEN`, `WENN`. Dezimalkomma und Semikolon als Funktionstrenner. Ergebnis = Formel × Faktor + Korrekturwert. Formeln führen keine Gerätebefehle aus.
- Passende gespeicherte Eingangswerte können historische Formelkurven bilden. Unterschiedliche Quellen, fehlende Zeitabschnitte und Schätzanteile bleiben erkennbar. Kacheln verwenden den gemeinsamen Editor und werden mitgesichert.

## Lastausgang, Ladejournal und BMS-Übergabe

- Manuelle Last-Schaltwünsche erhalten Vorrang vor Archivabrufen. Ein laufender Archivabruf wird geordnet unterbrochen; während einer Liveabfrage wird kurz gewartet. Neuere Wünsche ersetzen ältere, wartende Aufträge verfallen nach 45 Sekunden. Manuelles Aus pausiert die Automatik sofort und kann einen wartenden Einschaltwunsch ersetzen.
- Vorgemerkt, gesendet und vom Regler bestätigt sind getrennte Zustände. Schaltknöpfe richten sich nach Rückmeldung und offenem Auftrag. Steuerung und Roller-Ladeweg verwenden den in der Einrichtung gewählten Regler.
- Ladejournal mit Beginn/Ende, Gesamtdauer, Akkustand und Messzeitpunkten, Endegrund, beobachteten Zeiten/Lücken, BMS-Wh, gemessener Eingangsenergie und gemeinsamem Wirkungsgrad. Die bisherige Profil-Schätzung ist getrennt beschriftet. Vorhandene Spannungs-, Strom-, Leistungs- und SOC-Kurven sind aufklappbar; nachgeladene Ergebnisse aktualisieren das offene Journal mit erhaltener Scrollposition.
- Bei bestätigter BMS-Rückgabe bleibt die Rückgabephase bis zum tatsächlichen Wiederempfang durch die Zentrale erhalten. Wiederholte Bestätigungen sind idempotent. Eine kurzzeitig verlorene WLAN-Antwort hebt eine gerade bestätigte Zuständigkeit nicht sofort auf und startet keinen zweiten direkten Leser.

## Offline-Wasser-/Gaseinträge

Auf einem bekannten Anzeigehandy Eingaben unterwegs speichern; sie erscheinen als **Übertragung ausstehend**. Die Warteschlange überlebt App-Neustarts und bleibt an dieselbe Anlage gebunden. Nach Wiederverbindung automatische Übertragung mit fester Vorgangskennung und Bestätigung; wiederholte Antworten erzeugen keine Duplikate. Datum/Uhrzeit bleiben erhalten. Konflikte bei inzwischen geänderten Einträgen werden zur Prüfung gezeigt. Diese Funktion überträgt Versorgungseinträge, keine verzögerten Schaltbefehle.

## Prüfung und Grenzen

Die zugehörige Android-Prüfung umfasst JavaScript-/Java-Berechnungen, Parser- und Sicherungsfälle, Lastenergie mit getrennten Reglern und Messlücken, SQLite-/TCP-Verbundprüfungen, Android-Lint, Browserbedienung einschließlich vollständigem Reset/Wiederherstellen und Offline-Neustart sowie drei Android-Emulatortests. Der veröffentlichte APK-Quellstand und der erfolgreiche Prüflauf sind im Release vermerkt. Aufnahmen verwenden neutrale Beispieldaten.

**Nicht am persönlichen Gerät gemessen:** reale G5/S20/S10-Funkübergabe, Bildschirm-aus-/Hotspot-Verhalten, BLE-Verzögerung und physisches Schalten/Laden. Die beschriebenen Softwarekorrekturen ersetzen diese Vor-Ort-Prüfung nicht. Wetterdaten bleiben anbieterabhängig; der Langfristtrend hat deutlich höhere Unsicherheit.

Technische Registergrundlage: [Victron BlueSolar/SmartSolar HEX-Protokoll](https://www.victronenergy.com/upload/documents/BlueSolar-HEX-protocol.pdf). Wetter: [Open-Meteo Forecast](https://open-meteo.com/en/docs) und [Seasonal Forecast](https://open-meteo.com/en/docs/seasonal-forecast-api).

[Bedienung](AUTARPIE-3.md) · [Gas und Wasser](GAS-WASSER.md) · [Vollständige Versionshistorie](../CHANGELOG.md)
