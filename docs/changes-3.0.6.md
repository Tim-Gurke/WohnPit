# AutarPie 3.0.6 – Rollerlernen bei Abwesenheit und farbige Kurzwahlen

## Aktualisieren

**Zuerst die Zentrale, danach alle Anzeigehandys auf 3.0.6 aktualisieren.** Eigenständige Geräte direkt aktualisieren. Über die vorhandene Installation installieren und einmal öffnen. Paket `de.wattpit.app`, ursprüngliche Updatesignatur, Kopplungen, Messungen, Ladejournal, eigene Farben und Layouts bleiben erhalten. Nicht deinstallieren und keine App-Daten löschen.

Für die neuen Grundlagen und die Wochenannahme benötigen Zentrale und Anzeigen 3.0.6. Die Anzeige liest beim ersten Abgleich auch vorhandene Roller- und Ladeweg-Aufzeichnungen der letzten 90 Tage ein. Ein bestehender Lernstand wird weiterverwendet, soweit seine Grundlage passt. Der historische Haushaltsabgleich aus 3.0.5 bleibt enthalten.

## Rollerlernen ohne ganztägigen BMS-Empfang

**Ladeenergie und Ladehäufigkeit werden getrennt gelernt.** Eine ausreichend vollständig erfasste Wohnwagenladung kann schon allein einen vorläufigen Bedarf pro Ladung liefern. Der Roller muss dafür nicht täglich 20 Stunden erreichbar sein. Die Anzeige verwendet das vorhandene Ladejournal und seine aufgezeichneten BMS-Abschnitte; im eigenständigen Betrieb sind auch geeignete ältere Journal-Energiewerte nutzbar. Automatische Ladeaufzeichnung wie bisher im Rollerbereich ausdrücklich einschalten oder die Aufzeichnung einer Ladung manuell starten und beenden.

Für einen verwertbaren abgeschlossenen Vorgang müssen mindestens 98 Prozent der Aufzeichnungsdauer belegt sein. Noch aktive, wesentlich unvollständige oder wegen einer Pause offen gebliebene Aufzeichnungen liefern keinen vollständigen Ladungsbedarf. Wiederholte Einträge werden zusammengeführt; zeitlich nahe zusammengehörige Fragmente mit passendem Akku-/Ladewegkontext werden als eine Aufzeichnung bewertet. Lücken werden nicht mit angenommener Energie gefüllt. Bis zu 28 geeignete Ladungen aus den letzten 90 Tagen bilden die Schätzung; Anzahl, Alter, vorläufiger Stand und ausgeschlossene Aufzeichnungen bleiben sichtbar.

Die BMS-Energie und der daraus abgeleitete **Wohnwagenanteil bleiben unterscheidbar**. Der Wohnwagenanteil wird mit den für die damalige Aufzeichnung gespeicherten Verlustannahmen geschätzt. Im Relaisbetrieb ist Relaisspulenleistung kein gemessener Roller-Ladeeingang und wird nicht zur Ladungsenergie umgedeutet. Eine mögliche externe Ladung ohne Wohnwagenzuordnung wird nicht als Wohnwagenverbrauch gelernt. Aus einer Akkustandsdifferenz während der Abwesenheit wird kein exakter Fahrverbrauch abgeleitet.

## Ladehäufigkeit und Tage ohne Wohnwagenladung

Damit die App ein Ausbleiben der Wohnwagenladung bewerten kann, den zugehörigen Regler unter **Lastausgang → Einrichten** dem E-Scooter zuordnen und aktiv auslesen. **„Dieser Ladeweg versorgt ausschließlich die Rollerladung“** nur bestätigen, wenn dies tatsächlich der Verkabelung entspricht. Den Relaisbetrieb wie bisher erst nach dem tatsächlichen Umbau manuell auswählen.

Ein vom Regler durchgehend als ausgeschaltet bestätigter Ladeweg kann auch bei abwesendem Roller Zeiten ohne Wohnwagenladung belegen. Während zugeordneter Ladung liefern die aufgezeichneten BMS-Abschnitte die Energiegrundlage. Nur vollständig abgedeckte abgeschlossene Kalendertage zählen zum neuen Ladeweg-Durchschnitt. Mindestens drei solche Tage sind erforderlich; bis zu 28 passende Tage werden verwendet. Auch 23-/25-Stunden-Tage werden nach ihrem tatsächlichen Kalenderumfang bewertet. Eine Funklücke, ein unbekannter Ausgangszustand oder ein Ausfall der Zentrale wird nicht zum verbrauchsfreien Tag. Geeignete bisherige bekannte Nutzungstage und manuell bestätigte Tage bleiben als bisherige Grundlage verfügbar.

**E-Scooter → Verbrauch lernen** öffnet dieselben Sondernutzungen wie die Solarvorschau. Bei unbekannter Häufigkeit lässt sich unter **Bearbeiten → Vorläufige Ladehäufigkeit** beispielsweise `3,5` Ladungen pro Woche annehmen. `0` bedeutet „noch offen“, nicht „nie laden“. Die Extras-Prognose verbindet diese ausdrücklich gekennzeichnete Annahme mit der gelernten Energie je Ladung. Sobald genügend geeignete Ladeweg-Tage vorliegen, verwendet sie deren tatsächlichen Tagesdurchschnitt. Alternativ kann eine vorhandene eigene Tages-/Kilometerschätzung überbrücken; sie bleibt als eigene Annahme gekennzeichnet. Ohne Grundlage oder Annahme bleibt der Tagesbedarf unbekannt.

## Unterwegs und nach der Rückkehr

Bei fehlendem BMS-Empfang bleiben geeignete Ladungserfahrungen, ihr Messzeitpunkt und ein vorhandener Planungswert verfügbar. Ein gespeicherter Akkustand wird weiterhin als letzter Datenstand angezeigt. Nach der Rückkehr ersetzen frische BMS-Werte den pauschalen Ansatz für eine konkret eingeplante Ladung bis zum Ladeziel. Beide werden nicht addiert. Diese Lernänderung erteilt keine automatische Schaltfreigabe; die vorhandenen frischen Bestätigungen bleiben für die Steuerung erforderlich. Die Sondernutzung wird weiterhin nicht zusätzlich in den bereits gemessenen Gesamtverbrauch addiert.

Alle Auswertungen laufen auf Anzeigehandys oder im eigenständigen Betrieb. Die Zentrale sammelt und synchronisiert die Grundlagen.

## Kurzwahlen in der gemeinsamen App-Akzentfarbe

Die ausgewählte Kurzwahl verwendet für alle vorgegebenen und eigenen Dashboards die gemeinsame App-Akzentfarbe. Ein abweichendes Farbdesign des gerade geöffneten Dashboards verändert diese Auswahlfarbe nicht. Änderungen der gemeinsamen Akzentfarbe werden unmittelbar übernommen, auch in Hell-/Dunkelansicht. Die Markierung bleibt beim Neuaufbau und Umordnen der Kurzwahlen erhalten. Eine zusätzliche Unterstreichung und die zugängliche Kennzeichnung der aktuellen Seite machen die Auswahl auch unabhängig von der Farbe erkennbar.

## Prüfung und Grenzen

Neue Rechentests prüfen einzelne kurze Ladungen, unbekannte Häufigkeit, gekennzeichnete Wochenannahmen, vollständige Tage ohne Ladung, Ausfälle der Zentrale, andere BMS-Geräte, doppelte oder unvollständige Aufzeichnungen, veraltete Daten, den 25-Stunden-Tag und die einmalige Ersetzung durch einen konkreten Ladebedarf.

Der neue produktive SQLite-/TCP-Test beobachtet das BMS an drei Tagen jeweils weniger als eine Stunde, erfasst getrennt den Ladeweg und rekonstruiert drei Ladungen. Geteilte und doppelte Journaleinträge zählen einmal; eine unvollständige Ladung und ein halber Tag ohne Zentralbeobachtung bleiben ausgeschlossen. Der echte verschlüsselte Viewer-Abgleich liefert dieselbe Auswertung. Die Relaisspule wird dabei ausdrücklich nicht als Rollerenergie verwendet.

Browserprüfungen bedienen alle vorgegebenen und eigene Kurzwahlen, ändern deren Reihenfolge und die globale Akzentfarbe, verwenden ein abweichendes Dashboard-Design sowie beide Darstellungen und bearbeiten die neue Wochenannahme bei fehlendem BMS-Empfang. Die bestehenden Rechen-, Archiv-, Verbund-, Lint-, Browser- und Android-Emulatorprüfungen bleiben Teil des Freigabelaufs. Sämtliche Beispieldaten sind synthetisch. Echte Bluetooth-Verbindungen, die persönliche Prognosegüte und die tatsächliche Verkabelung wurden damit nicht physisch geprüft.
