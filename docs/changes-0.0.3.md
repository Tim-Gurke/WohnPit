# WohnPit 0.0.3

Der Diagnoseimport von 0.0.2 übernahm Anlagen- und Wetterkontexte für den historischen Verbrauch, ließ aber bereits gespeicherte Lernabschnitte, Prognosevergleiche und bestätigte frühere Aufstellungen aus. Nach dem Umzug konnte dadurch die angezeigte Lerngrundlage deutlich kleiner sein.

„Verbrauch und Lernstand übernehmen“ ergänzt jetzt diese vorhandenen Tabellen. Auch die bekannte Version-2-Lerndiagnose aus Watt’n’Pit 1.6.10 bleibt nutzbar. Fehlendes Wetter einer bereits bestehenden identischen Aufstellungsbestätigung wird mit der vorhandenen Merge-Funktion ergänzt. Vorhandene Lernabschnitte haben Vorrang; überlappende alte Abschnitte werden nicht doppelt gezählt. Ein wiederholter Import bleibt ohne Duplikate. Unbekannte zukünftige Lernformate werden abgewiesen.

Die Lern- und Prognoseberechnung, das Drei-Jahres-Fenster und der bestehende Datenverbund bleiben unverändert. Die Übertragung an bereits gekoppelte Anzeigehandys wird mit zwei unabhängigen TCP-/SQLite-Instanzen geprüft. Eine unveränderte Kopie der Watt’n’Pit-1.8.3-Anzeigeberechnung vergleicht ausgewertete Stunden, Tagesbedarf, Abwesenheitsprofil und Ertragskalibrierung vor und nach dem Umzug. Die Referenzdateien sind ausschließlich Testmaterial und werden nicht in die APK gepackt.

## Anwenden

1. Die APK über WohnPit 0.0.1 oder 0.0.2 installieren, ohne die App zu deinstallieren.
2. WohnPit-Aufzeichnung pausieren und einen laufenden Archivabruf beenden.
3. Unter Einstellungen und Daten „Verbrauch und Lernstand übernehmen“ auswählen und den bisherigen Mess- und Lerndiagnose-Export der alten Watt’n’Pit-Zentrale erneut einlesen.
4. Aufzeichnung starten und Watt’n’Pit auf dem Anzeigehandy vollständig neu öffnen.

Kein erneuter CSV-Import und keine neue Kopplung erforderlich. Watt’n’Pit und ScootPit benötigen für diesen Import kein Update. Die Wiederherstellung hängt davon ab, welche Lernabschnitte im alten Export vorhanden sind; historische Stunden werden nicht erfunden oder pauschal angehoben.
