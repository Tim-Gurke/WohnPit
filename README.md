# AutarPie 3.0.5

![AutarPie Ressourcen-Kreislauf](app/src/main/assets/brand.svg)

**Das lokale Cockpit für autarkes Leben.** Solar und Stromverteilung, Wohnwagenakku, E-Scooter, Modellbau, Gas und Wasser in einer gemeinsamen App. AutarPie führt **Watt’n’Pit und WohnPit** weiter.

**[AutarPie 3.0.5 als APK herunterladen](https://github.com/Tim-Gurke/AutarPie/releases/download/v3.0.5/AutarPie-v3.0.5.apk)** · [Einrichtung und Bedienung](docs/AUTARPIE-3.md) · [Update und Prüfgrenzen](docs/changes-3.0.5.md) · [Vollständige Versionshistorie](CHANGELOG.md)

Android 8 oder neuer; das Cockpit benötigt einen aktuellen Android-System-WebView. Kein Konto, keine Werbung. Mess- und Lerndaten bleiben auf den eigenen Geräten. Der optionale Wetterabruf verwendet Open-Meteo. Der Anlagenverbund überträgt Daten verschlüsselt im lokalen WLAN.

## Ansichten

| Verbrauchslernen und Diagnose | Native Zentrale |
| --- | --- |
| ![AutarPie 3.0.5: Verbrauchslernen und Diagnose unter Daten und Sicherung](docs/images/autarpie-3.0.5-settings.svg) | ![AutarPie 3.0.5: schlanke native Zentrale](docs/images/autarpie-3.0.5-central.svg) |

Die Aufnahmen zeigen die tatsächliche geprüfte Oberfläche mit neutralen Beispieldaten. Kacheln, Bereichsfarben sowie Hell-/Dunkelansicht sind einstellbar; eigene Anordnungen bleiben erhalten.

## Ein Cockpit, mehrere Anwendungen

- **Übersicht:** frei zusammenstellen, was gerade gebraucht wird – auch Werte aus E-Scooter, Modellbau, Gas und Wasser.
- **Solar & Speicher:** Anlage, Batteriebilanz, PV-Spannung, Lastausgang, Wetter und Prognose.
- **E-Scooter:** BMS-Werte, Speicherinhalt, Ladeweg, Ladeplanung und Sondernutzung.
- **Gas / Wasser:** Beschaffungen, Flaschenwechsel, korrigierter Vorrat und gelernter Bedarf.
- **Modellbau:** 608PD-Livewerte, begrenzte bestätigte Bedienfunktionen und Einordnung externer Batterieladung.
- **Verlauf, Geräte, Einstellungen:** Messungen, Phasenzeitleiste, Verbindungen und gemeinsame Konfiguration.

Alle Bereiche sind oben über **Menü** erreichbar. Untere Kurzwahlen lassen sich ein-/ausblenden und ordnen; Ziehen und Pfeile zeigen die neue Reihenfolge sofort im Dialog. Android-Zurück führt durch die geöffneten Einstellungs- und Menüebenen. Eigene benannte Dashboards können leer beginnen oder eine vorhandene Ansicht kopieren. Raster, Kachelgrößen und Hoch-/Queranordnungen bleiben gerätelokal. Kachelentfernen fragt nach; die Darstellungsgröße verlangt ausdrückliches Speichern.

Gemeinsame Bereichsfarben und weiche zwei-/dreifarbige Skalen sind zentral einstellbar. App- und Kachelhintergründe unterstützen eine oder zwei Farben mit Verlaufwinkel 0–360°. Einzelne Kacheln dürfen davon abweichen. Optional erhält jedes Dashboard ein eigenes Farbdesign. Die Farbauswahl startet beim aktuellen Farbton und bietet App-Farben sowie automatisch gemerkte eigene Mischfarben.

## Zentrale, Anzeige oder eigenständig

| Rolle | Aufgabe |
| --- | --- |
| Zentrale | Mess- und Wettergrundlagen erfassen, speichern und verteilen; native Bedienoberfläche und bestätigte Steuerung |
| Anzeigehandy | Cockpit, Diagramme, Prognosen, Verbrauchslernen und eigene Dashboards aus den Zentraldaten berechnen |
| Eigenständig | Erfassung, Berechnung, Planung und Cockpit auf einem Gerät ohne Zentrale |

Autostart stellt nach Neustart oder App-Update den gespeicherten Zustand wieder her. Eine pausierte Aufzeichnung bleibt pausiert. Das konkrete Android-Gerät kann erstes Entsperren und zusätzliche Hintergrund-/Bluetooth-Freigaben verlangen. Gemeinsame Zentralfunktionen sind auch von den Anzeigehandys bedienbar, einschließlich Archivabruf und Aufzeichnung.

## Solar, Lastausgang und 608PD

Standort, Aufstellung, Verschattung, Module, Verkabelung und Regler werden gemeinsam eingerichtet. Solarkreise behalten beim Umordnen ihre Kennung und historischen Daten. Mehrere Kreise an demselben Regler teilen sich in der Prognose die eingetragene Strom-/Leistungsgrenze. Jeder mobile Kreis erhält einen eigenen anklickbaren Namen in der Wetterkarte.

Der SmartSolar-Lastausgang ist E-Scooter, Modellbau oder einem anderen Verbraucher zuordenbar. Zustand, Modus, Spannung, Strom und Abschaltgrund werden soweit unterstützt vom Gerät gelesen; Leistung und Energie daraus auf der Anzeige ausgewertet. Regler je Kachel wählen; aktuelle und historische Werte bleiben getrennt. Werte werden nicht nochmals zum bereits am Shunt erfassten Verbrauch addiert.

Die **optionale Rollerautomatik ist zunächst aus**. Ladeziel, Wohnwagenreserve, Tagesziel, Überschussschwellen und Prognosereserve sind variabel. Ein aktives Anzeigehandy erstellt den zeitlich begrenzten Plan; die Zentrale kontrolliert direkte Abschaltbedingungen. Ein Schaltauftrag ist erst mit der tatsächlichen Geräteantwort bestätigt. Ohne gültigen Plan wird zum Ausschalten angefordert. BMS-Empfang allein beweist noch keinen eingesteckten Ladestecker.

Der 608PD erhält eine private Verbindung aus der eigenen Bluetooth-Aufzeichnung. Unterstützt sind nachgewiesene Livefelder und Beenden von Laden/Powerbank-Betrieb. App-Start ist zunächst auf das beobachtete **3S-LiPo-Profil 4,21 V/Zelle / 0,50 A** mit ausdrücklicher Profilbestätigung begrenzt. Powerbank und andere Profile am Gerät starten. Nicht belegte Temperatur-/USB-/CC-CV-Felder werden nicht erfunden.

## Update 3.0.5

Die Anzeige gleicht den älteren Rohverlauf jetzt auch bei bereits vorhandenen importierten Lernabschnitten einmalig ab. Gespeicherte stündliche Einstrahlung für den damaligen Standort und die damalige Aufstellung fließt wieder in geeignete historische Abschnitte ein. Die Datengrundlage zeigt den lokalen Archivumfang, andere Anwesenheitsprofile und erhaltene Ausschlussgründe. Fehlende Grundlagen bleiben unbekannt; aus gespeicherter Laufzeit werden nicht pauschal Lernstunden.

Unter **Einstellungen → Daten & Sicherung → Verbrauchslernen und Diagnose** den historischen Abgleich erneut anfordern oder den lokal vorhandenen Mess- und Lernstand exportieren. Der Diagnoseexport funktioniert auch auf Anzeigehandys bei momentan nicht erreichbarer Zentrale und enthält die tatsächlich ausgewählte Anlage. Unabhängige Daten des Anzeigehandys bleiben getrennt.

**Über die vorhandene App aktualisieren: zuerst die Zentrale, danach alle Anzeigen auf 3.0.5.** Anschließend die Anzeige mit der Zentrale verbunden lassen, bis der erste historische Abgleich fertig ist. Keine Neuinstallation und kein Zurücksetzen nötig. Die ursprüngliche Updatesignatur bleibt erhalten. [Änderungen, Bedienung und Prüfgrenzen 3.0.5](docs/changes-3.0.5.md) · [Roller, manueller Relaisbetrieb und G5-Hintergrundfreigabe 3.0.4](docs/changes-3.0.4.md) · [Vollständige Historie](CHANGELOG.md).
