# WohnPit 2.0.2 – Gas, Wasser und ein gemeinsames Projekt

**WohnPit führt Watt’n’Pit und die bisherige WohnPit-Zentrale in einer App zusammen.** Dieselbe APK bietet Zentrale, Anzeigehandy und eigenständigen Betrieb direkt mit unterstützten Victron-Geräten. Die Weiterentwicklung findet im öffentlichen WohnPit-Repository statt; Watt’n’Pit wird privat aufbewahrt. Die [vollständige dokumentierte Watt’n’Pit-Vorgeschichte](wattnpit-archiv/CHANGELOG.md), historischen Anleitungen und zugehörigen Bilder bleiben im gemeinsamen Projekt öffentlich erreichbar.

## Gas- und Wasserbedarf verständlich anzeigen

- Bedarf für **3 / 7 / 14 Tage** wahlweise in **kg oder Flaschen** sowie **Litern oder Kanistern** anzeigen, zum Beispiel 25,3 kg als 2,3 Flaschen zu 11 kg oder 30 L als 1,5 Kanister zu 20 L.
- Unter **Gas / Wasser → Einstellen** Kanistergröße in Litern und Gasflaschenfüllmenge in Kilogramm anpassen. Vorgaben sind 20 L und 11 kg; auch Größen mit Nachkommastellen sind möglich. Die Kanistergröße ist unabhängig von der üblichen Wasser-Beschaffungsmenge.
- Größen vom Anzeigehandy aus ändern; die Zentrale speichert sie für alle gekoppelten Anzeigen. Die Einheit auf der Versorgungsseite und je Bedarfskachel auswählen. Zahlenwert, Rundinstrument, Skala und Großansicht verwenden dieselbe Bezugsgröße. Die portable Einstellungssicherung erhält die Einheitenwahl.
- Einheiten- und Größenwechsel verändern keine bereits erfassten Mengen, Vorräte oder Lernabschnitte. Fehlende Bedarfsgrundlagen bleiben unbekannt.

## Wetterberechnung erkennen

Die Gasansicht zeigt den tatsächlichen Lernstand: geeignete abgeschlossene Flaschenabschnitte, aktives Temperatur-/Windmodell oder den konkreten Grund, weshalb noch ohne Wetterkorrektur geschätzt wird. Ein geladener Wetterrückblick allein aktiviert kein Modell. Die bestehenden Prüfgrenzen bleiben erhalten: Temperatur frühestens ab sechs geeigneten unterschiedlich kalten Abschnitten, Wind frühestens ab neun; das Modell muss die bisherigen Verbräuche zuverlässiger erklären als der Durchschnitt.

Aktuelle Vorhersagen für den richtigen Ort beeinflussen Bedarf und Reichweite. Die Korrektur des bisherigen Verbrauchs mit Wetterrückblick wird gesondert genannt. Der **aktuelle Restvorrat** ist die heute geschätzte Menge; eine andere Vorhersage für kommende Tage ändert den künftigen Bedarf und die Reichweite. Die Tagesbedarfskachel bleibt der Durchschnitt aus abgeschlossenen Abschnitten.

Die Navigation erhält ein Symbol aus Flamme und Wassertropfen. Beide Versorgungskarten verwenden passende Symbole. Die Zusammenführung wird im README, in der App und auf der Projektseite ausdrücklich benannt.

## Installieren und aktualisieren

1. **Zuerst die bereits gemeinsame WohnPit-Zentrale auf 2.0.2 aktualisieren.**
2. Danach **2.0.2 auf S20/S10 beziehungsweise den Anzeigehandys** über die vorhandene App installieren.
3. **Gas / Wasser → Einstellen** öffnen, Größen prüfen und die gewünschte Bedarfsanzeige wählen. In Kacheln unter **Kachel-Einstellungen → Einheit** umstellen.

Nicht vorher deinstallieren, keine App-Daten löschen und nicht neu koppeln. Messwerte, Journal, Kopplungen und eigene Kachelanordnungen bleiben erhalten. Ein abgeschlossener Umzug muss nicht wiederholt werden. Für individuelle Kanistergrößen braucht auch die Zentrale 2.0.2; bei einer älteren Zentrale ist die noch nicht unterstützte Größen-Einstellung gekennzeichnet. Ältere Anzeigen können die bisherigen Standardmengen ändern, ohne die neue Kanistergröße zurückzusetzen. Im eigenständigen Betrieb nur dieses Handy aktualisieren. ScootPit benötigt kein Update.

**Noch Watt’n’Pit auf der Anzeige:** direkt die gemeinsame WohnPit-APK über die vorhandene Watt’n’Pit-App installieren. App-Kennung und Updatesignatur bleiben erhalten. **Noch die separate WohnPit-Zentrale 0.0.x:** zuerst die zusätzlich angehängte **WohnPit-Uebernahme-v0.0.5.apk** verwenden und die [Umzugsanleitung](ZUSAMMENFUEHRUNG.md) beachten. Die persönliche kennwortgeschützte .wohnpit-Sicherung ist privat und gehört niemals in ein öffentliches Release. Nach dem alten Umzug gegebenenfalls die Victron-Schlüssel wie in der Anleitung erneut speichern.

Die notwendige signierte Übernahme-APK bleibt als Hilfsprogramm beigefügt, auch wenn das frühere 0.0.5-Release aus den drei neuesten Veröffentlichungen fällt. Quellhistorie und vollständige Änderungsdokumentation bleiben erhalten.

[Ausführliche Gas-/Wasseranleitung](GAS-WASSER.md) · [Installieren und umziehen](ZUSAMMENFUEHRUNG.md) · [Versionshistorie](../CHANGELOG.md)

## Prüfungen und Grenzen

Geprüft werden die Umrechnung und fehlende Werte, frei einstellbare Größen, unveränderte historische Mengen und Vorräte, Größenübertragung über zwei verschlüsselte LAN-Anzeigen, ältere Einstellbefehle, Einstellungen nach Neustart sowie Export/Import der Einheitenwahl. Die tatsächliche Oberfläche wird mit erfundenen Daten auf Auswahl, Größen, Zahlen, Rundinstrumente, Skalen und Großansicht geprüft; aktuelle Screenshots zeigen beide Designs.

Android-Unit-Tests, Lint, Build, Archiv-/Verbundprüfungen, vorhandene Browserprüfungen und die Android-Emulatorprüfungen gehören zur Freigabe. Signatur und Zertifikat der veröffentlichten APK werden mit der bestehenden Updatesignatur verglichen. Ein Test mit den persönlichen Daten, Bluetooth-Verbindungen und der tatsächlichen Hardware auf G5, S20 und S10 wurde hier nicht durchgeführt.
