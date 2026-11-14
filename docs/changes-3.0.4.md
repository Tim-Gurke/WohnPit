# AutarPie 3.0.4 – Lerndaten erhalten, Roller und Relais vorbereiten

## Aktualisieren

**Zuerst die Zentrale, anschließend alle Anzeigehandys auf 3.0.4 aktualisieren.** Die Lernalgorithmus-Kennung wurde angehoben: Berechnungen im Verbund benötigen denselben Stand. Über die vorhandene Installation installieren, einmal öffnen; nicht deinstallieren oder App-Daten löschen. Paket `de.wattpit.app`, ursprüngliche Updatesignatur, Geräte, Archive, vorhandene Kachelanordnungen und eigene Farben bleiben erhalten. ScootPit-Schnittstelle und Altübernahme-Hilfsprogramm bleiben verfügbar.

**Der bisherige direkte Ladeweg bleibt aktiv.** Das Update schaltet weder den Strompfad noch die Ladeautomatik selbstständig auf Relais um. Auch eine bereits freigegebene Automatik wird durch die neue Option nicht erstmals eingeschaltet.

## Zentrale nach Neustart

Android 10/11 benötigt für Bluetooth-Suche nach einem Hintergrundstart zusätzlich die Standortfreigabe **Immer zulassen**. Die bisherige Berechtigung allein beim Verwenden der App genügt dafür nicht. Das erklärt, weshalb eine Zentrale erreichbar sein konnte, aber erst nach Öffnen der App neue Victron-Werte empfing.

Auf dem G5 nach dem Update einmal AutarPie öffnen. Den Hinweis befolgen oder Android-Einstellungen → Apps → AutarPie → Berechtigungen → Standort → Immer zulassen wählen. Bluetooth und Android-Standortdienst eingeschaltet lassen. Autostart sowie Aufzeichnung müssen in AutarPie eingeschaltet sein. Falls crDroid zusätzlich Autostart oder Hintergrundbetrieb beschränkt, diese Freigaben ebenfalls prüfen. Eine Displaysperre mit notwendiger erster Entsperrung kann Androids Zugriff auf geschützte App-Daten bis zur Entsperrung verhindern.

Der vorhandene Boot-/Update-Empfänger stellt den gespeicherten Betriebszustand wieder her; absichtlich pausierte Aufzeichnung bleibt pausiert. Fehlende Hintergrundfreigabe bleibt als Fehler sichtbar. Ein still gewordener BLE-Scan wird mit Abstand erneut gestartet. Ein frischer Lastausgangs-Status gilt nicht mehr als Beleg für vollständige Solar- und Shunt-Livewerte: Die Übersicht unterscheidet Live, teilweise live und fehlenden Geräteempfang.

## Verbrauchslernen und Roller-Sondernutzung

Es gibt **keine 200-Stunden-Grenze**. Ausgewertete Stunden sind geeignete Messabschnitte; sie entsprechen nicht einfach der Laufzeit der Zentrale. Geschätzte mobile Anteile, fehlende Abdeckung, Tagesabschnitte, unterschiedliche Tage und Sondernutzungen werden weiterhin getrennt bewertet.

Ein konkreter Fehler wurde korrigiert: Die Anzeige löschte bei geänderten Rohdaten zunächst alle späteren Lernabschnitte, bevor feststand, ob sie aus Wetter, früherer Aufstellung und vorhandenen Rohwerten wiederherstellbar waren. Die Löschung konnte auch die anschließende Erhaltung bereits gelernter Abschnitte aushebeln. Jetzt werden tatsächlich betroffene Datenblöcke gezielt markiert und ihre bisherigen Erkenntnisse gespeichert gehalten. Nicht verlässlich neu berechenbare Abschnitte bleiben ausdrücklich zur Neuberechnung vorgemerkt, statt als gesicherte Grundlage weiterzuzählen. Ihr Umfang wird unter **Datengrundlage** getrennt ausgewiesen; spätere Abgleiche berücksichtigen auch vorgemerkte Abschnitte außerhalb der letzten Woche. Vollständig neu aufgebaute Abschnitte ersetzen ihre alte Fassung.

Die geöffnete Datengrundlage aktualisiert sich mit dem Lernstand und bleibt dabei aufgeklappt. Zuvor konnte die angezeigte Stundenzahl bis zum erneuten Öffnen stehen bleiben. Bereits durch eine ältere Version gelöschte Erkenntnisse lassen sich nur aus noch vorhandenen Rohdaten beziehungsweise einer Sicherung rekonstruieren; dieses Update erfindet keine verlorenen Messungen.

Im Zentralbetrieb fehlte zudem die Zählung beobachteter Roller-Nutzungstage. Die Zentrale speichert nun endliche BMS-Beobachtungen auch ohne Ladung. Die Anzeige berechnet daraus Tagesabdeckung und zugeordnete Ladeenergie. Wiederholte Synchronisation zählt keine Zeiten doppelt. Unbeobachtete Tage werden nicht als verbrauchsfreie Tage ergänzt; nicht zuordenbare Ladung bleibt unbekannt. Alte reine Ladeaufzeichnungen allein belegen keinen vollständig beobachteten Ruhetag. Kompakte reine Ruhebeobachtungen werden für die letzten 90 Tage gehalten; bestehende Energie- und Ladehistorien behalten ihre bisherige Aufbewahrung.

## Roller außerhalb der Reichweite

Die letzten BMS-Messwerte bleiben mit Zeitpunkt und **Letzter Datenstand · Roller nicht erreichbar** in der Übersicht und im Rollerbereich sichtbar. Dies umfasst vorhandene Akkudaten und den daraus geschätzten Speicherinhalt beziehungsweise die Reichweite. Die Anzeige schreibt keine Werte fort. Nach Neustart steht der zuletzt gespeicherte Stand zur Verfügung; im laufenden Betrieb bleibt der zuletzt empfangene Stand erhalten.

Aktuelle Ladeleistung, Restladezeit, Wirkungsgrad und Ladefreigabe benötigen weiterhin geeignete frische Messungen. Formeln erhalten aus einem veralteten BMS-Stand keine aktuelle Messquelle. Geänderte Roller-Adresse entfernt den Cache des vorherigen Geräts.

## Relais erst nach dem Umbau manuell wählen

**Solar oder E-Scooter → Lastausgang → Einrichten → Strompfad** bietet:

- **Bisheriger Betrieb:** Ladestrom durch den Lastausgang; unabhängige Eingangs-/BMS-Messungen können weiter den Wirkungsgrad liefern.
- **Relaisbetrieb:** Lastausgang versorgt nur die Relaisspule. Ladestrom fließt über den separaten Versorgungsweg zum Step-up. Erst nach tatsächlichem Umbau ausdrücklich auswählen und speichern. Eine laufende Ladeaufzeichnung zuvor beenden.

Die bisherigen Messungen bleiben erhalten. Ein aus mindestens einer Minute gemeinsam gemessener Abschnitte berechneter Wirkungsgrad lässt sich im Einrichtungsdialog ausdrücklich als Referenz übernehmen. Ohne geeignete Vergleichsmessung bleibt der hinterlegte Wirkungsgrad eine Annahme. Der gespeicherte Wert gilt auch nach dem Wechsel; höherer Ladestrom kann die tatsächlichen Verluste verändern.

Im Relaisbetrieb bleiben BMS-Leistung und BMS-Ladeenergie gemessen. Der Roller-Ladebedarf am Wohnwagen wird aus BMS und Referenzwirkungsgrad **geschätzt**. Die Ausgangskachel benennt die gemessene Relaisspule. Spulenstrom wird nicht als Ladeeingang oder neuer gemessener Wirkungsgrad verwendet. Für eine neue Wirkungsgradmessung wäre eine unabhängige Messung im tatsächlichen Leistungspfad erforderlich. Die Spule ist weiterhin im gesamten, am Shunt erfassten Wohnwagenverbrauch enthalten. Während der Überschussregelung wird ihre gemessene Leistung zusätzlich zur geschätzten aktiven Rollerladung berücksichtigt. Die bisherige Abschaltprüfung für den eigentlichen Ausgangsladestrom wird erst im gewählten Relaisbetrieb durch die Bestätigung aus Ausgangszustand und frischem BMS-Ladestrom ersetzt.

Die Roller-Sondernutzung bleibt Bestandteil des gemessenen Gesamtverbrauchs. Ihr geschätzter Anteil wird nur für das Lernen des üblichen Wohnwagenbedarfs getrennt behandelt und nicht ein zweites Mal zur Gesamtbilanz addiert.

## Gas früher mit Wetter korrigieren

Schon der erste vollständige Flaschenabschnitt – zwei entsprechend erfasste Wechsel – kann mit passenden historischen Wetterdaten eine **vorläufige Temperaturkorrektur** ermöglichen. Diese verwendet eine vorsichtige Heizannahme, die am beobachteten Flaschenbedarf verankert ist; sie behauptet keine aus einer Flasche persönlich gelernten Heiz- oder Kochanteile. Die Änderung bleibt auf −35 % bis +60 % des üblichen Tagesbedarfs begrenzt. Die Unsicherheit bleibt groß.

Ab drei unterschiedlich temperierten Abschnitten wird geprüft, ob ein persönliches Temperaturmodell den einfachen Durchschnitt in einer Vergleichsprüfung ausreichend verbessert. Wind wird erst mit genügend zusätzlichen Daten geprüft. Unpassender Ort, fehlende Rückblickdaten oder ausgeschaltete/veraltete Vorhersage erzeugen keine vorgetäuschte Wetterkorrektur. Bedarf, Restvorrat seit dem letzten bekannten Vorrat und Reichweite verwenden dieselben Grundregeln; der übliche Tagesbedarf bleibt als empirischer Durchschnitt erkennbar.

## Darstellung und Rückschau

Die letzten drei Wettertage enthalten zusätzlich **Solarertrag aus dem Regler-Tagesarchiv** und **gespeicherten SmartShunt-Akkustand bei Sonnenuntergang**. Für den Akku gilt höchstens 15 Minuten Abstand; die tatsächliche Messzeit wird in der Detailansicht genannt. Fehlende Werte bleiben offen. Wetterrückblick ist ein Wettermodell, Solar- und Akkudaten stammen aus den jeweiligen Messarchiven. Ungemessenes mobiles Solar gehört nicht zum Regler-Tagesertrag.

Die Kurzwahl zeigt ausschließlich Symbole: Roller von der Seite und Controller für Modellbau; zugängliche Namen für Vorlesefunktion und Bedienhilfen bleiben erhalten.

Neue Installationen von **AutarPie** erhalten die vereinbarte Gestaltung und Dashboard-Anordnung: dunkel, angepasste Bereichsfarben, Verläufe, Skalen und 75 % Darstellungsgröße. Vorhandene Installationen behalten ihre eigene Anordnung. Die Vorlage enthält ausschließlich Darstellung; persönliche Standorte, Geräteadressen, Anlagenkonfiguration, Nutzungsdaten und Zugangsdaten werden nicht mitgeliefert. Andere Apps werden nicht geändert.

## Prüfung und Grenzen

Regressionsprüfungen nutzen die tatsächliche SQLite- und Synchronisationslogik sowie neutrale Beispieldaten. Sie prüfen den früheren Lernverlust bei fehlenden Rohdaten, Ortswechsel und noch unpassendem Wettercache, den getrennten Aufschub, echte mehrtägige BMS-Beobachtung, wiederholte Berechnung und erhaltene Eingangsenergie. Berechnungstests prüfen vorläufige Gas-Korrektur, Grenzen, veraltete BMS-Werte und Schätzung im Relaisbetrieb. Browserprüfungen verwenden die echte Oberfläche mit der ausgelieferten Darstellungsvorlage. Android-Prüfungen erfassen Installation, drei Betriebsarten, Datenübernahme und erhaltene Layouts auf API 26 und 30.

Der erfolgreiche Build, die Prüfergebnisse, APK-Prüfsumme und ursprüngliche Signatur werden im Release nachgewiesen. Emulator-/Softwareprüfungen ersetzen keinen Neustartversuch auf dem konkreten G5 und keinen Schaltversuch am echten Relais; diese Hardwareergebnisse werden nicht behauptet.
