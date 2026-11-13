# AutarPie 3.0.3 – Dashboardfarben bleiben bei ihrer Seite

Behebt die falsche Farbzuordnung beim Wechsel zwischen Übersicht, Solar, Verlauf und anderen Bereichen. Der Verlauf verwendet sein eigenes gespeichertes Design oder die gemeinsamen Vorgaben. Seine Farbe hängt nicht mehr von der vorher geöffneten Seite ab.

## Korrektur

- „Farben dieses Dashboards“ nennt und bearbeitet die tatsächlich sichtbare Seite, auch im Verlauf, bei Geräten und in den Einstellungen.
- Hintergrund und Kachelflächen werden auch beim Wechsel in diese Bereiche neu aus ihren eigenen Vorgaben gesetzt. Ohne eigenes Design gelten die gemeinsamen Farben.
- Eine gemeinsame Wert- und Diagrammfarbe des Verlaufs wird auch in seinen Diagrammen verwendet. Die gemeinsamen Farben anderer Seiten werden dadurch nicht verändert.
- Speichern und „auf alle Kacheln anwenden“ betreffen nur die gewählte Seite. Der Kachelbestand des zuletzt geöffneten anderen Dashboards wird nicht in den Verlauf kopiert.
- Abbrechen erhält den gespeicherten Zustand. Bei abgelehntem Speichern bleiben die bisherigen Farben erhalten; ein erneuter Versuch speichert weiterhin dieselbe Seite.

Die Ursache lag in der Verwechslung der sichtbaren Seite mit dem zuletzt geladenen Kacheldashboard. Der Verlauf besitzt eine eigene Diagrammansicht; deshalb blieb diese alte Zuordnung beim Wechsel bisher stehen. Sichtbare Seite und geladenes Kacheldashboard werden jetzt getrennt berücksichtigt. Die Korrektur betrifft die bestehende Darstellung und ihre Speicherung; die Funk- und Erfassungsabläufe werden weiterverwendet.

## Update

Die APK über die vorhandene App installieren und einmal öffnen. Paket `de.wattpit.app` und bisherige Updatesignatur bleiben erhalten. Einstellungen, Geräteverknüpfungen, Messdaten und eigene Kachelanordnungen werden nicht zurückgesetzt.

Der Zentralen-Hotfix aus 3.0.2 ist enthalten. **Bei einer Zentrale mit 3.0.1 oder älter zuerst die Zentrale aktualisieren.** Eine Zentrale mit 3.0.2 bleibt mit Anzeigehandys auf 3.0.3 kompatibel; diese können direkt aktualisiert werden. ScootPit BMS 1.6.14 bleibt unverändert kompatibel. Das beigefügte alte WohnPit-Hilfsprogramm ist nur für einen noch ausstehenden Umzug aus der getrennten Altinstallation vorgesehen.

**Farben einstellen:** Gewünschte Seite öffnen → Menü → Farben dieses Dashboards. Der Dialogtitel nennt diese Seite. Für gemeinsame Vorgaben „Eigenes Dashboard-Design“ ausschalten. Bereits unter der falschen Seite gespeicherte Farben lassen sich nicht zuverlässig automatisch ihrer ursprünglich gemeinten Seite zuordnen; sie werden erhalten und können gezielt angepasst werden.

## Prüfung

Ein neuer Browser-Test mit der tatsächlichen Oberfläche reproduziert den Fehler im bisherigen Code: Nach „Übersicht → Verlauf“ bleibt die Übersichtsfarbe statt der gemeinsamen Verlaufsfarbe stehen. Derselbe Test prüft mit der Korrektur beide Wechselrichtungen, Solar, eigene Dashboards, Geräte und Einstellungen, wiederholte Aktualisierung, den tatsächlichen Farbeditor, unveränderte fremde Kachelbestände, abgelehntes Speichern samt Wiederholung, Abbrechen, Neustart und Hoch-/Queransicht. Die Prüfung läuft zweimal in der regulären Oberfläche-Prüfstrecke.

Die vollständigen Berechnungs-, Speicherungs-, Verbund- und Android-Oberflächenprüfungen einschließlich des Zentralen-Deadlocks bleiben Bestandteil der Freigabe. Der erfolgreiche Prüflauf und der geprüfte APK-Quellstand stehen im Release. Tests verwenden neutrale Beispieldaten. Funk- und Schaltversuche an den persönlichen Geräten werden daraus nicht abgeleitet.

[Bedienung](AUTARPIE-3.md) · [Vollständige Versionshistorie](../CHANGELOG.md)
