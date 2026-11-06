# Zusammenführung als WohnPit 2.0.0

Stand 05.11.2026: Die Zusammenführung ist beschlossen, umgesetzt und als **WohnPit 2.0.0** veröffentlicht. Die Weiterentwicklung findet im [WohnPit-Repository](https://github.com/Tim-Gurke/WohnPit) statt. Dieses Repository erhält die bisherigen Watt’n’Pit-Quellstände und die Versionshistorie.

**[Gemeinsame Android-App herunterladen](https://github.com/Tim-Gurke/WohnPit/releases/latest)** · [Ausführliche Einrichtung und Umzugsanleitung](https://github.com/Tim-Gurke/WohnPit/blob/main/docs/ZUSAMMENFUEHRUNG.md) · [Screenshots und Änderungen](https://github.com/Tim-Gurke/WohnPit/releases/tag/v2.0.0)

## Eine App mit drei Betriebsarten

| Betriebsart | Aufgabe |
|---|---|
| Zentrale | Schlanke native Konsole ohne Dashboard-WebView; Geräte, Aufzeichnung, Archive, Wetter und gemeinsame Datenbasis |
| Anzeigehandy | Mit einer Zentrale koppeln; anzeigen, berechnen und bedienen. Ohne Kopplung keine eigene Victron-Aufzeichnung |
| Eigenständig · Victron direkt | Ohne Zentrale mit Victron verbinden; Aufzeichnung, Wetter, Berechnung und Anzeige auf einem Handy |

Die Betriebsart wird beim Erststart gewählt und kann später geändert werden. Bestehende Watt’n’Pit-Rollen werden übernommen. Der Zentralbetrieb bleibt für das G5 vorgesehen; reale Bluetooth-Verbindungen und der Ressourcenverbrauch am G5 wurden bei dieser Veröffentlichung nicht gemessen.

## Installation und Datenübernahme

1. **Bisheriges Watt’n’Pit:** die aktuelle WohnPit-App über die vorhandene App installieren, nicht vorher deinstallieren. Die technische Kennung `de.wattpit.app`, Updatesignatur und ScootPit-Schnittstelle bleiben erhalten.
2. **Bisherige WohnPit-Zentrale:** zuerst die [Übernahmeversion 0.0.5](https://github.com/Tim-Gurke/WohnPit/releases/tag/v0.0.5) über die alte App mit Kennung `de.wohnpit.app` installieren. Aufzeichnung pausieren und eine vollständige verschlüsselte `.wohnpit`-Sicherung lokal erstellen.
3. Die gemeinsame WohnPit-App installieren, Sicherung importieren, Vorschau prüfen und die Übernahme bestätigen. App schließen und neu öffnen. Aufzeichnungen, Wetter, Journal, Lernstände und Einstellungen kontrollieren; Geräte bei Bedarf neu koppeln.
4. Die alte Übernahme-App und die private Sicherung bis zur erfolgreichen Kontrolle behalten. Die ausführliche Anleitung oben beschreibt Reihenfolge, Rückweg und Grenzen.

**Die persönliche Umzugsdatei enthält private Verknüpfungen, Schlüssel und Kennwörter. Sie darf auch verschlüsselt niemals öffentlich auf GitHub, in Releases, Prüfartefakten oder auf der Projektseite erscheinen.** Das Sicherungskennwort wird nicht gespeichert. Android-Bluetooth-Verbindungen und WLAN-Zugangsdaten des Betriebssystems müssen gegebenenfalls neu eingerichtet werden.

WohnPit ist ein lokales Cockpit für Energie, Gas und Wasser im Wohnwagen, Ferienhaus, Gartenhaus und an passenden Balkonanlagen. Die neue Bezeichnung erweitert die tatsächliche Gerätekompatibilität nicht auf beliebige Wechselrichter. [Projektseite](https://wohnpit.extraordinarygrove.chatgpt.site/)

Der ursprüngliche Entwurf bleibt in der Git-Historie erhalten. Alte Tags und Änderungsdokumente bleiben bestehen; je Repository werden nur die drei neuesten Releases veröffentlicht.
