# WohnPit 0.0.5 – Private Übernahmeversion

Diese Version bereitet den Wechsel zur gemeinsamen **WohnPit-App ab 2.0.0** vor. Die bisherige schlanke Zentrale erhält eine **vollständige verschlüsselte Übernahmesicherung**. Ihre Aufzeichnung und bestehende Daten werden dadurch nicht gelöscht.

## Vorbereiten und sichern

1. 0.0.5 über die bestehende WohnPit-Zentrale installieren, ohne vorher zu deinstallieren. Das App-Symbol heißt zur Unterscheidung **WohnPit · Übernahme**.
2. Aufzeichnung pausieren, einen Archivabruf beenden und einen laufenden Wetterabruf abwarten.
3. Unter **Einstellungen und Daten → Vollständige Übernahmesicherung** die Sicherung starten. Einen lokalen Speicherort wählen.
4. Ein eigenes langes Kennwort eingeben und wiederholen. Die Datei endet auf **.wohnpit**. Sicherung und Kennwort für den Import aufbewahren.
5. Erst nach erfolgreicher Sicherungsmeldung zur neuen WohnPit-App wechseln. Die bisherige App bis zur geprüften Übernahme behalten und ihre Aufzeichnung pausiert lassen.

Die Sicherung umfasst Messwertdatenbanken, Anlagen-/Standortverlauf, Gas-/Wasserjournal, gespeichertes Wetter, Lernstände sowie App-Einstellungen einschließlich vorhandener Geräte- und Verbundschlüssel. Der gewöhnliche portable Einstellungsexport und eine CSV sind keine gleichwertige Vollsicherung. Geräte bei Bedarf nach dem Import neu verknüpfen; wichtig ist insbesondere der Erhalt von Aufzeichnungen und Lernständen.

**Die .wohnpit-Datei enthält private Zugangsdaten und darf niemals öffentlich auf GitHub, in Releases oder auf einer Website erscheinen.** Die App verschlüsselt sie mit dem von dir eingegebenen Kennwort und speichert dieses Kennwort nicht. Die öffentliche APK ist nur die Anwendung; sie enthält keine persönliche Umzugsdatei. Ein lokaler Dateidialog wird verwendet; die App lädt diese Sicherung nicht auf einen Server hoch. Ein verlorenes Kennwort kann die App nicht wiederherstellen.

## Import in die gemeinsame WohnPit-App

Die neue WohnPit-Version 2.0.0 wird separat bereitgestellt und führt die bisherige Watt’n’Pit-Updatekennung weiter. Vor dem Umzug auch eine gegebenenfalls noch vorhandene alte Watt’n’Pit-Aufzeichnung auf dem G5 pausieren. Dort die neue WohnPit-App installieren, **Übernahmesicherung einlesen** wählen und die .wohnpit-Datei mit ihrem Kennwort öffnen. Bei bereits eingerichteter neuer App findest du den Import in den Einstellungen.

Zuerst prüft die neue App Entschlüsselung, Sicherungsinhalt und Datenbanken. Bis zum erfolgreichen Abschluss bleiben die aktuellen Daten unangetastet. Nach der Prüfung die App schließen und erneut öffnen. Erst beim Neustart wird die Übernahme ausgeführt; zusätzlich bleibt intern eine Sicherung des vorherigen Zustands erhalten. Danach die Betriebsart Zentrale prüfen, Aufzeichnung starten und die Anzeigehandys kontrollieren. Livewerte, Rückblick, Journal und Lernstand vergleichen. Falls nötig Geräte oder Anzeigehandys erneut koppeln. Die alte Übernahme-App erst entfernen, wenn der neue Betrieb geprüft ist.

## Prüfung und Grenzen

Der Android-Build, Unit-Tests, Lint und die vorhandenen Archiv-/Verbundprüfungen gehören zur Freigabe. Die Verschlüsselung wird mit mehreren Datensätzen, zufälligen Initialwerten, falschem Kennwort, beschädigten oder abgeschnittenen Abschnitten und unerwartet angehängten Daten geprüft. Die gemeinsame App prüft den Import zusätzlich im Android-Emulator mit Datenbank und Einstellungen. Ausschließlich künstliche Testdaten werden veröffentlicht; persönliche Sicherungen sind durch Dateifilter und Veröffentlichungskontrolle ausgeschlossen.

Kein Test am tatsächlichen G5 oder den echten Victron-Geräten wurde hier durchgeführt. Diese Version bleibt eine Vorbereitung zur Umstellung, nicht die neue Cockpit-Oberfläche.
