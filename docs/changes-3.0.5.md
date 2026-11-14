# AutarPie 3.0.5 – Den vorhandenen Verlauf fürs Verbrauchslernen nutzen

## Aktualisieren

**Zuerst die Zentrale, danach alle Anzeigehandys auf 3.0.5 aktualisieren.** Eigenständige Geräte direkt aktualisieren. Die Versionen müssen wegen der angehobenen Lernalgorithmus-Kennung übereinstimmen. Die APK über die vorhandene Installation installieren und einmal öffnen. Paket `de.wattpit.app`, ursprüngliche Updatesignatur, Kopplungen, Messarchive, eigene Farben und Kachelanordnungen bleiben erhalten.

Anschließend die Anzeige mit ihrer Zentrale verbinden und geöffnet lassen, bis der erste historische Abgleich abgeschlossen ist. Je nach Archivgröße und WLAN dauert dieser erste Abruf länger als ein gewöhnlicher Liveabgleich. Er läuft auf dem Anzeigehandy im Hintergrund; die Zentrale erfasst weiterhin die Grundlagen. Ein unterbrochener Abgleich bleibt ausstehend und wird erneut versucht. Bereits vollständig übertragene unveränderte Datenblöcke werden wiederverwendet.

## Warum zu wenige Stunden gelernt wurden

Eine Anzeige konnte bereits einige importierte Lernabschnitte besitzen, ohne den älteren Rohverlauf vollständig lokal zu haben. Bisher verhinderte der vorhandene Lernstand den umfassenden ersten Abruf: die Berechnung blieb praktisch auf einen jüngeren Ausschnitt beschränkt. 3.0.5 gleicht einmalig den vorhandenen historischen Messbereich innerhalb des dreijährigen Lernfensters ab, auch wenn bereits gelernte Abschnitte existieren. Danach bewertet die Anzeige den Verlauf mit den gespeicherten Grundlagen neu.

Archivierte stündliche Wetter-/Einstrahlungsdaten werden für den damaligen Standort und die gespeicherte Modulgeometrie wiederverwendet. Die damalige Verschattung und der eingetragene Reglerfaktor bleiben wirksam. Ein heutiger Wetterpuffer oder ein anderer Standort ersetzt keine fehlende historische Grundlage. Ausschließlich gemessene geeignete Abschnitte können auch nach einem Standortwechsel ohne erneute Wetterbeschaffung ausgewertet werden.

Die Anzahl gespeicherter Stunden ist weiterhin nicht gleich der Anzahl geeigneter Lernstunden: Messlücken, unbekannte frühere Aufstellung, fehlende historische Wettergrundlagen, Ladungsbegrenzung und Sonderverbrauch bleiben fachlich zu berücksichtigen. Geschätzte mobile Beiträge bleiben als Schätzung gekennzeichnet. Bereits vorhandene Erkenntnisse werden bei einer unvollständigen Neuberechnung erhalten beziehungsweise als ausstehend vorgemerkt. Es wird kein persönlicher Lernstand versprochen, bevor der tatsächliche Abgleich auf dem Gerät erfolgt ist.

## Datengrundlage nachvollziehen

Solarvorschau → **Datengrundlage** zeigt zusätzlich den lokal vorhandenen Messzeitraum und die Zahl der gespeicherten Messwerte. Dieser Archivumfang ist ausdrücklich keine Behauptung lückenloser Messabdeckung. Geeignete Abschnitte mit einem anderen Anwesenheitsprofil und mit unbekannter Anwesenheit werden getrennt erklärt.

Ein weiterer Fehler versteckte Ausschlussgründe beim Zusammenführen der Tagesabschnitte nach Anwesenheitsprofil. Diese Gründe bleiben jetzt erhalten. Neu ausgewertete Ausschlüsse gehören zum jeweiligen Profil; ältere nur global gespeicherte Gründe werden getrennt als **Anwesenheit nicht zuordenbar** angezeigt. Sie werden nicht nachträglich einem Profil zugeschrieben.

## Verlauf erneut auswerten und Diagnose exportieren

Unter **Einstellungen → Daten & Sicherung → Verbrauchslernen und Diagnose**:

- **Gespeicherten Verlauf neu auswerten** fordert einen erneuten vollständigen Abgleich mit anschließender Auswertung an. Dies löscht weder Rohmessungen noch Geräte oder Layouts. Bei einer Anzeige muss die Zentrale für den neuen Abgleich erreichbar sein.
- **Diagnose speichern** funktioniert jetzt auch auf Anzeigehandys. Das ZIP enthält deren Analyse der ausgewählten Zentrale, gespeicherte Lern-/Wettergrundlagen und den bereits lokal verfügbaren Messverlauf. Ein Export ist auch bei vorübergehend nicht erreichbarer Zentrale möglich; er ist als zwischengespeicherter Stand gekennzeichnet. Der Export lädt nicht automatisch ein noch fehlendes Archiv nach.

Die eigenständigen Daten eines Anzeigehandys werden nicht mit den Daten seiner gekoppelten Zentrale vermischt. Diagnose und Verlauf der Zentrale bleiben direkt an der Zentrale verfügbar.

## Prüfung und Grenzen

Ein Regressionstest verwendet den produktiven verschlüsselten TCP-Verbund und SQLite: eine absichtlich unvollständige übernommene Lernhistorie wird aus 29 vollständig belegten Messtagen zu 696 geeigneten Stunden aufgebaut. Derselbe Test scheitert auf dem unveränderten 3.0.4-Quellstand bereits am fehlenden historischen Abruf. Wiederholter Abgleich, unterbrochener Wiederaufbau, Offline-Diagnose und Erhalt der unabhängigen Handydaten werden geprüft. Alle Beispieldaten sind synthetisch.

Ein weiterer produktiver Auswertungstest rekonstruiert fünf Stunden an einem früheren Standort mit gespeicherter Einstrahlung und ausdrücklich modelliertem mobilem Solar. Fremde Standorte und fehlende Archivstunden werden zurückgewiesen; gespeicherte Verschattung verändert die Schätzung. Rechentests prüfen getrennte Anwesenheitsprofile und frühere nicht zuordenbare Ausschlüsse. Die tatsächliche Oberfläche prüft den Diagnosezugang, den Neuabgleich und die Anzeige dieser Gründe.

Zusätzlich werden die vorhandenen Android-Unit-, LAN-/SQLite-, Browser-, Lint- und Emulatorprüfungen ausgeführt. Emulatorprüfungen decken Android 8 und Android 11 ab; echte Bluetooth-Geräte, das Zentralhandy, dessen Betriebssystem und die persönliche Prognosegüte sind damit nicht physisch geprüft. Die neue Verhaltensauswertung des Rollers bei längerer Abwesenheit gehört nicht zu diesem Update.
