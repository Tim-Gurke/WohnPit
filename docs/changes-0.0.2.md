# WohnPit 0.0.2

## Historischen Verbrauch ergänzen

Der bisherige CSV-Umzug enthält Messwerte, aber nicht die früheren Aufstellungen und archivierten Modelldaten für ein mobiles Panel. Dadurch können Solar- und Batteriehistorie vorhanden sein, während der Verbrauch unbekannt bleibt.

„Historischen Verbrauch übernehmen“ liest diese vorhandenen Grundlagen aus dem bereits von Watt’n’Pit 1.8.1 angebotenen Export „Mess- und Lerndiagnose exportieren“. Die Aufzeichnung muss pausiert und ein Archivabruf beendet sein. Vorhandene Datensätze und heutige Einstellungen bleiben erhalten; Messwerte werden dabei nicht erneut importiert. Die Energie- und BMS-Berechnung bleibt unverändert. Eine Anleitung steht in der README.

## Weniger Archivverbindungen

Nach der ersten Bedarfsprüfung 15 Sekunden nach Aufzeichnungsbeginn prüft die Zentrale standardmäßig stündlich. Zusätzliche Archivverbindungen erfolgen weiterhin nur bei Bedarf. Unter Energiegeräte → Automatischen Abruf einstellen sind 15 Minuten, 30 Minuten, stündlich oder nur manuell wählbar. Erfolgreiche und gescheiterte Abrufe erhalten je Gerät das gewählte Intervall als Pause; nach Abbruch mindestens 30 Minuten. Manuelles Nachladen bleibt möglich. Liveempfang, Datenverbund und BMS-Übergabe laufen unabhängig weiter.

Das sichert keine lückenlose 1-Sekunden-Archivhistorie: Diese feinste Gerätestufe reicht nur etwa zwei Minuten zurück. Die standardmäßige eigene Liveaufzeichnung bleibt bei zehn Sekunden; Empfangslücken lassen sich im etwa zweistündigen 30-Sekunden-Fenster nachladen.

## Zentrale erkennbar und leichter bedienbar

- Eigenes Haus-Akku-Symbol, einschließlich der einfarbigen Android-Variante.
- Nativer App-Kopf mit Logo, WohnPit und Versionsnummer.
- Aktive Aufzeichnung in Mint, pausierte Aufzeichnung und laufender Archivabruf in Gelb.
- Starten ist bei aktiver Aufzeichnung ausgegraut; Pausieren ist nur bei laufender oder vorbereiteter Aufzeichnung verfügbar.
- Archivabbruch ist nur bei laufendem Abruf verfügbar, Nachladen nur mit eingerichteten Geräten und ohne parallelen Abruf.
- Kopplung, historische Übernahme und BMS-Neuverbindung berücksichtigen ebenfalls ihren Betriebszustand. Graue Schaltflächen bleiben als Orientierung sichtbar.
- Die native Liveanzeige verwendet den tatsächlichen Messzeitpunkt statt eines nicht vorhandenen Feldnamens.

## Installation und Prüfgrenzen

Die APK über WohnPit 0.0.1 installieren; die App nicht vorher deinstallieren. App-Kennung und Signierschlüssel bleiben gleich. Watt’n’Pit und ScootPit benötigen für diese Änderungen kein Update.

Automatische Tests prüfen die vorhandenen Protokolle, Berechnungen und Datenbanken sowie Umzug und Archivpausen. Der Android-8-Emulator prüft Installation und Bedienoberfläche. Echte Funkverbindungen, Stromverbrauch und die Anlage im Wohnwagen müssen am Gerät geprüft werden.
