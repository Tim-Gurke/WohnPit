# WohnPit 0.0.4 – Gemeinsame Datenbasis für Gas und Wasser

WohnPit sammelt und speichert jetzt zusätzlich manuell erfasste Wasserbeschaffungen, Gasflaschenwechsel und bekannte Restvorräte. Die vollständige Bedienung, Verbrauchsauswertung und Anzeige erfolgt in Watt’n’Pit ab 1.9.0 auf S20/S10.

- Gemeinsames Journal mit dauerhaft bestätigten Einträgen, Korrekturen, Löschungen und einstellbaren Vorschlagsmengen von 20 L Wasser und 11 kg Gas.
- Vorzeitige Gaswechsel und zeitlich geordnete Bestandskorrekturen bleiben im Originaljournal erkennbar. Wiederholte Übertragungen erzeugen keine doppelten Einträge.
- Wettervorhersagen und historische Temperatur-/Windwerte für den eingerichteten Wohnwagenstandort sammeln. Wetterrückblicke werden passend zu früheren Einträgen in begrenzten Abschnitten ergänzt; ausgefallene Abrufe erhalten bereits gespeicherte Werte.
- Ursprüngliche Wettervorhersagen werden getrennt von späteren Rückblicken archiviert. Der Wetterdienst erhält nur Ort und Zeitraum, keine Verbrauchsmengen oder Gerätekennungen.
- Kleine native Statusanzeige für Journal und Wetterstand. Die neue Gas-/Wasser-Lernberechnung bleibt auf den Anzeigehandys.
- Der vorhandene verschlüsselte Anlagenverbund überträgt das neue Journal zusätzlich. Das bestehende Bestätigungs-/Revisionsverfahren gilt auch für die neuen Aktionen. BMS-Übergabe und Messwertspeicherung behalten ihre vorhandenen Bausteine.

## Aktualisieren

WohnPit **0.0.4 über die vorhandene Installation** installieren. Danach Watt’n’Pit **1.9.0** auf S20 und S10 aktualisieren. Nicht vorher deinstallieren. Messdaten, Geräte und vorhandene Anzeige-Kopplungen bleiben erhalten; erneutes Verknüpfen ist nicht erforderlich. ScootPit benötigt hierfür kein Update.

[Ausführliche Gas-/Wasseranleitung](GAS-WASSER.md) · [Versionshistorie](../CHANGELOG.md)

## Prüfung und Grenzen

Zur Freigabe gehören Android-Unit-Tests, Lint, signierter Release-Build sowie Integrationstests mit echter verschlüsselter TCP-Verbindung und SQLite. Die Journalprüfungen decken zwei Anzeige-Clients, Speicherung vor Bestätigung, Wiederholungen, Korrekturen, ungültige Mengen und Löschen ab. Die Statusoberfläche wird zusätzlich auf einem Android-8-Emulator aufgenommen.

Die angezeigten Vorräte bleiben Schätzungen aus manuellen Angaben. Sensoren und ein separater Reserveflaschenbestand gehören noch nicht zu dieser Version. Die vorhandenen Einstellungs-/Energieexporte enthalten das neue Versorgungsjournal noch nicht. Bluetooth-Betrieb und tatsächlicher Stromverbrauch auf dem G5 werden durch die Softwareprüfungen nicht nachgewiesen.

Je Repository bleiben höchstens die drei neuesten Releases veröffentlicht; die dokumentierte Versionshistorie bleibt erhalten.
