# Statistiken und Diagramme auf dem Anzeigehandy

## Update auf 2.0.1

Bereits in Watt’n’Pit 1.8.4 und auch in WohnPit 2.0.0 konnten Livewerte sichtbar sein, während der erste Statistikabruf immer wieder von neu eintreffenden Messwerten überholt wurde. WohnPit 2.0.1 hält vollständige Ergebnisse sichtbar und lässt auch weitere wartende Diagramme laden.

1. Die aktuelle APK auf dem Anzeigehandy über die bestehende Watt’n’Pit- oder WohnPit-App installieren. Ein direktes Update von Watt’n’Pit 1.8.4 auf WohnPit 2.0.1 ist möglich; Zwischenversionen müssen nicht installiert werden. Nicht vorher deinstallieren oder deren Daten löschen.
2. Anzeige öffnen und die Verbindung zur Zentrale abwarten. Bei mehreren Handys jedes Anzeigehandy aktualisieren. Eine Zentrale mit WohnPit 2.0.0 kann für diese Korrektur weiterlaufen.
3. Die Übersicht geöffnet lassen, während der erste Abgleich läuft. Die Zentrale darf dabei weiter aufzeichnen. Umfang und WLAN-Geschwindigkeit beeinflussen die Dauer.
4. Solarertrag und Geräteverbrauch sowie ein Leistungsdiagramm prüfen. Unter **Verlauf** einen aufgezeichneten Zeitraum wählen. Die Kacheln behalten ihre eingestellten eigenen Zeiträume.
5. Bei einem bereits abgeschlossenen Umzug weder Sicherung erneut importieren noch den Anlagenverbund neu koppeln. Für den erstmaligen Wechsel von der alten WohnPit-Zentrale gilt die separate [Umzugsanleitung](ZUSAMMENFUEHRUNG.md).

## Status richtig lesen

| Anzeige | Bedeutung |
|---|---|
| Daten werden im Hintergrund geladen / Messdaten werden abgeglichen | Für diese Ansicht läuft der erste Abruf. Livewerte können bereits vorhanden sein. |
| Letzter Stand · neue Daten werden geladen | Ein vollständiger Statistikstand ist sichtbar; neuere Messwerte werden ergänzt. |
| Letzter Stand · Aktualisierung fehlgeschlagen | Der letzte vollständige Stand bleibt sichtbar. WLAN und Zentrale prüfen; ein weiterer Versuch folgt. |
| Strich oder ohne Messwert | Für diese Kennzahl und diesen Zeitraum ist kein geeigneter Messwert verfügbar. Das bedeutet nicht automatisch 0. |

Live-Solarleistung wird in **W**, Solarertrag über einen Zeitraum in **Wh** dargestellt. Ein aktueller Leistungswert beweist daher nicht, dass schon eine vollständige Tagesstatistik vorliegt. Leere Diagramme für Zeiten ohne gespeicherte Messdaten bleiben korrekt leer.

## Wenn weiterhin keine Statistik erscheint

Prüfen, ob oben die richtige Anlage verbunden ist und auf dem Anzeigehandy tatsächlich Version 2.0.1 oder neuer läuft. Zentrale und Anzeigehandys benötigen dasselbe lokale WLAN. Die Aufzeichnung und vorhandenen Messpunkte an der Zentrale kontrollieren und einen passenden Zeitraum wählen. Beim ersten Abgleich darf der Hinweis zum Laden länger sichtbar sein.

Für eine Fehlermeldung genügen zunächst Versionen auf Zentrale/Anzeige, betroffene Kennzahl, Zeitraum und der angezeigte Lade- oder Fehlertext. **Keine vollständige Umzugsdatei oder Geräteschlüssel öffentlich hochladen.** Screenshots vor dem Teilen auf private Verknüpfungen und Standortdaten prüfen.
