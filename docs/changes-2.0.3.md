# WohnPit 2.0.3 – Statistiken trotz laufender Aufzeichnung laden

Livewerte konnten auf dem Anzeigehandy bereits ankommen, während Solarertrag, Geräteverbrauch, Akkubilanz und mobile Solarwerte dauerhaft beim Laden blieben. Ein vollständiger Statistikabruf wurde nachträglich verworfen, wenn die Zentrale währenddessen neue Wetter- oder Berechnungsgrundlagen erhielt. Die frühere Korrektur der JavaScript-Anzeige allein reichte dafür nicht aus.

## Korrektur

- Ein vollständig übertragener und berechneter Stand derselben Anlage bleibt sichtbar. Er wird ausdrücklich als älterer Stand gekennzeichnet, bis der anschließende Abgleich abgeschlossen ist.
- Ein neuerer Stand auf der Zentrale löst einen weiteren Abruf aus, auch wenn die Anzeige noch keine neue Live-Revision erhalten hat. Die einzelnen Kurven kommen weiterhin nacheinander an die Reihe.
- Der erste Abruf der sichtbaren Kacheln hat Vorrang vor dem großen Abgleich der Lernhistorie. Spätere Aktualisierungen verhindern das Lernen nicht.
- Ein Wechsel der Anlage und ein ersetzter Datenbestand dürfen weiterhin keine alten Ergebnisse als aktuelle Daten übernehmen. Fehlende Werte bleiben unbekannt; es werden keine Nullen als Ersatz erfunden.
- Vorhandene Messungen, Gas-/Wasserjournal, Kopplungen und die eigene Anordnung je Handy und Ausrichtung bleiben erhalten. Die Behältergrößen und Einheiten aus 2.0.2 bleiben verfügbar.

## Aktualisieren

Zuerst die gemeinsame Zentrale, danach die Anzeigehandys auf **2.0.3** aktualisieren. Die Korrektur des Statistikabrufs läuft auf den Anzeigehandys; eine noch mit 2.0.2 betriebene gemeinsame Zentrale bleibt kompatibel. Mit 2.0.3 auf beiden Seiten ist zusätzlich ein während der Berechnung ersetzter Datenbestand eindeutig erkennbar. Im eigenständigen Betrieb nur dieses Handy aktualisieren.

Die APK über die bestehende WohnPit- beziehungsweise Watt’n’Pit-Installation installieren. **Nicht deinstallieren, keine App-Daten löschen, keine Sicherung erneut importieren und nicht neu koppeln.** Danach die Übersicht geöffnet lassen, während der erste Abgleich läuft. Ein größerer vorhandener Datenbestand benötigt weiterhin Übertragungs- und Rechenzeit.

Die bisherige Watt’n’Pit-Updatesignatur und App-Kennung bleiben erhalten. Die alte separate WohnPit-Zentrale 0.0.x benötigt weiterhin den in der [Umzugsanleitung](https://github.com/Tim-Gurke/AutarPie/blob/main/docs/ZUSAMMENFUEHRUNG.md) beschriebenen Übergang. Die signierte Übernahme-APK 0.0.5 bleibt als Hilfsprogramm im Release verfügbar; persönliche Umzugsdateien bleiben privat.

## Prüfungen

Der Fehler wurde mit dem unveränderten 2.0.2-Code über die echte verschlüsselte TCP-Strecke und SQLite nachgestellt: Eine Änderung des Wetterstands während des ersten Abrufs führte zur Ausnahme und verwarf die Statistik. Derselbe Test prüft jetzt vollständige Solar-, Verbrauchs-, Bilanz- und Mobilreihen, die Kennzeichnung des älteren Stands, das anschließende abgeschlossene Update und den Schutz bei einem ersetzten Datenbestand.

Ein zusätzlicher Oberflächentest verwendet genau diese nativen Berechnungsergebnisse in der tatsächlichen App-Oberfläche. Er prüft sichtbare Zahlen und gezeichnete Kurven trotz weiterlaufender Revisionen, den Vorrang vor dem Lernabgleich, das Ende der Ladeanzeige, unveränderte Layouts sowie beide Designs. Alle dafür verwendeten Messungen sind erfunden. Bestehende Unit-, Verbund-, Archiv-, Android-Build-, Lint- und Emulatorprüfungen bleiben Teil der Freigabe. Signatur und Inhalt der fertigen APK werden geprüft.

Die persönlichen Daten und die Funkverbindungen auf G5, S20 und S10 wurden hier nicht am echten Gerät getestet.

[Statistiken und Statusanzeigen](https://github.com/Tim-Gurke/AutarPie/blob/main/docs/ANZEIGE-STATISTIK.md) · [Installieren und umziehen](https://github.com/Tim-Gurke/AutarPie/blob/main/docs/ZUSAMMENFUEHRUNG.md) · [Versionshistorie](../CHANGELOG.md)
