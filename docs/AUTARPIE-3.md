# AutarPie einrichten und benutzen

## Rollen und Wiederanlauf

Die **Zentrale** erhält Bluetooth-Verbindungen, Messdaten, Wettergrundlagen und den lokalen Anlagenverbund. Ihre native Oberfläche zeigt Betriebszustände und direkte Bedienaktionen. Die **Anzeige** berechnet und zeichnet aus den Grundlagen einer gekoppelten Zentrale. **Eigenständig** kombiniert Erfassung, Berechnung und Cockpit auf einem Handy.

Unter Einstellungen → Zentrale & Verbindungen → Aufzeichnung, Archiv & Autostart lässt sich der Wiederanlauf einschalten. AutarPie nach Installation einmal öffnen und die Android-Freigaben erteilen. Eine ausgeschaltete Aufzeichnung wird durch Autostart nicht eingeschaltet. Ein geplanter täglicher kompletter Handy-Neustart wird weiterhin außerhalb der App eingerichtet; AutarPie übernimmt danach den Wiederanlauf, soweit Android ihn freigibt.

## Bereiche, Kurzwahlen und eigene Dashboards

„Bereiche“ im Kopf öffnet alle Anwendungen, auch wenn ihre untere Kurzwahl ausgeblendet ist. „Kurzwahlen & Dashboards“ bietet Ein-/Auswahl und Reihenfolge. Dort lassen sich eigene Dashboards umbenennen, kopieren und löschen. „Eigenes Dashboard“ startet leer oder übernimmt eine Kopie eines vorhandenen Dashboards. Eine Kopie lässt die Vorlage unverändert.

„Aktuelles Dashboard bearbeiten“ öffnet die Anordnung. Kacheln hinzufügen, verschieben, skalieren oder über ihr Menü anpassen. „Fertig“ beendet das Anordnen. Pro Gerät bleiben Hoch- und Queransicht getrennt. Entfernen betrifft nur das aktuelle Dashboard und verlangt eine Bestätigung; Geräte und Messdaten bleiben gespeichert.

## Farben, Flächen, Skalen und Größe

Einstellungen → Farben & Anzeige → Farben, Flächen & Skalen:

| Bereich | Vorgabe |
| --- | --- |
| Solar | Orange |
| Batterie / Speicher | Indigo |
| Berechnete Werte | Grün |
| E-Scooter | Türkis |
| Wasser | Blau |
| Gas | Gelb |
| Modellbau | Lila |
| Geräte / System | Grau |

Die Vorgaben lassen sich zentral ändern. Im Verbund übernimmt die Zentrale die gemeinsam gespeicherte Darstellung; das Layout bleibt lokal. Eine ausdrücklich eigene Kachel-Wertfarbe bleibt erhalten. Bei „Zentrale Messwertfarbe“ erbt die Kachel wieder die Bereichsvorgabe.

App- und zentrale Kachelhintergründe getrennt mit einer Farbe oder Verlauf aus zwei Farben und Winkel 0–360° festlegen. In den Kacheleinstellungen kann ein einzelner Hintergrund davon abweichen; „Zentraler Kachelhintergrund“ hebt diese Abweichung auf. Ohne eigene Flächenfarben verwendet die helle Ansicht einen hellen Hintergrund.

Mehrfarbige Rundinstrumente übernehmen die zentrale Skala mit zwei oder drei Farben, Grenzen und Übergangsbreite. Beispielsweise Rot bis 20, Gelb bis 40 und Grün darüber; die Übergänge sind weich. Eigene Skalenfarben sind je Kachel möglich. Die Werte beziehen sich auf den prozentualen Fortschritt zwischen dem jeweiligen Minimum und Maximum.

Die Darstellungsgröße ist gerätelokal. Dialog öffnen, Vorschau ändern, **Speichern**. Abbrechen oder Schließen verwirft die Vorschau. Das normale Scrollen über die Einstellungsseite speichert keine Größe mehr.

## Solaranlage gemeinsam konfigurieren

Unter Einstellungen → Solaranlage die gemeinsame Einrichtung öffnen. Dort stehen Standort/GPS, Richtung/Kompass, Neigung, Aktivität und Verschattung sowie unter „Module, Regler & Verkabelung“ die Hardwareangaben. Das Speichern übernimmt Anlage und Aufstellung gemeinsam.

Jeder Kreis hat eine unveränderliche interne Kennung. Name und Reihenfolge sind unabhängig davon. Pfeile ändern die angezeigte Kreisnummer/Reihenfolge, ohne Messungen einer anderen Kennung zuzuordnen. Mobil oder fest lässt sich pro Kreis bestimmen; mobile Kreise werden vor Ort in der Wetterkarte über ihren eigenen Namen zugeschaltet.

Für Kreise an demselben Regler dieselbe **Reglerkennung** vergeben, beispielsweise `mppt-75-15`. Eine Gruppe aus 2 × 100 W in Reihe und eine aus 1 × 150 W parallel an demselben 75/15 ergeben zusammen 350 Wp, teilen sich aber die eingetragene **15-A-Ladestromgrenze**. Die Prognose begrenzt die Summe gemeinsam mit `15 A × Batteriespannung` und einer gegebenenfalls eingetragenen Wattgrenze. Der mobile 100-W-Kreis am separaten PWM-Regler erhält eine eigene Kennung.

PV-Spannung bezeichnet den Solareingang und ist ein eigener Kachelwert. Aktiver Abruf benötigt eine bestehende Android-Kopplung zum Regler. Während eines Archivabrufs kann der Liveabruf warten. Andere aktive Apps können die Verbindung belegen. Fehlende oder alte Werte erscheinen als unbekannt, nicht als Null.

## Lastausgang und Rollerautomatik

Auf Solar & Speicher oder E-Scooter „Lastausgang → Einrichten“ öffnen. Zuordnung wählen: E-Scooter, Modellbau, anderer Verbraucher oder unzugeordnet. Aktive Messung einschalten. Die App zeigt gemeldeten Ausgangszustand und Strom; Leistung wird aus Spannung × Strom berechnet. Der Lastausgangsverbrauch ist bereits im Gesamtverbrauch am Shunt enthalten.

Manuelles Schalten sendet einen Auftrag. Anzeige „An“ oder „Aus“ richtet sich nach der Regler-Rückmeldung. Ein belegter oder unerreichbarer Regler liefert keine erfundene Bestätigung.

Für die Überschuss-Automatik dem E-Scooter zuordnen und ausdrücklich freigeben. Dann Ladeziel, Mindestreserve und Tagesziel des Wohnwagenakkus, Prognosereserve in Wh sowie aktuelle Start-/Stoppschwellen in W bestimmen. Die Stoppschwelle muss unter der Startschwelle liegen. Startverzögerung und angenommene Ladeleistung sind separat einstellbar.

Das BMS liefert Erreichbarkeit und Akkustand. Positive Ströme an Ausgang und BMS bestätigen erst die tatsächliche Rollerladung. Reiner BMS-Empfang beweist keinen eingesteckten Ladestecker. Ohne bestätigte Ladung wird nach einer Anlaufzeit wieder ausgeschaltet und eine erneute Freigabe benötigt. Manuelles Aus oder erreichtes Ladeziel verhindern ein sofortiges Nachladen bei einem kleinen SOC-Rückgang.

Die Berechnung benötigt ein aktives Anzeigehandy bzw. die aktive eigenständige Ansicht. Ein Plan wird regelmäßig erneuert und gilt höchstens eine Minute. Ein zweites Anzeigehandy übernimmt erst nach Ablauf der bisherigen Planführerschaft. Ohne rechtzeitig erneuerten Plan fordert das erfassende Gerät das Abschalten an. Die verbleibende Schaltverzögerung hängt von der tatsächlichen Reglerverbindung ab.

## 608PD

Unter Modellbau → Einrichten zuerst Name, Bluetooth-Adresse und Energiepfad speichern. Danach den eigenen Android-Fehlerbericht mit der aufgezeichneten 608PD-Anmeldung über „Eigene Bluetooth-Aufzeichnung übernehmen“ auswählen. Die App extrahiert nur die Verbindungskennung und speichert sie verschlüsselt auf dem erfassenden Gerät. Anschließend die Verbindung aktivieren und Aufzeichnung starten. Im Verbund lässt sich dies vom Anzeigehandy an der Zentrale einrichten.

Unterstützte Livewerte: Betriebszustand, Akkuspannung, Akkustrom, Laufzeit, gezählte mAh und Zellspannungen. Nicht nachgewiesene Temperatur-, USB-Eingangs- oder CC/CV-Phasenwerte werden nicht erfunden. „Beenden“ ist für Laden und Powerbank-Betrieb vorgesehen. Powerbank-Betrieb und andere Ladeprofile am Gerät starten. Der App-Start bietet ausschließlich das beobachtete 3S-Profil 4,21 V je Zelle / 0,50 A und fragt die Profilübereinstimmung ausdrücklich ab.

Für Powerbank → 608PD → Wohnwagenakku den Energiepfad **externe Batterieladung** auswählen. Diese Energie ist bereits in der Shuntbilanz sichtbar und wird nicht als Solarertrag zusätzlich addiert. Andere geeignete Batterieprofile am Lader einstellen.

## Verlauf, Versorgung und Sicherung

Die Ladephasen-Zeitleiste zeigt heute gemessene Reglerphasen wie Bulk, Absorption und Float. Graue Bereiche bedeuten fehlende Daten. Im Tagesarchiv kurze Abdeckungsangaben antippen, um Details zu sehen; weitere Spalten bleiben seitlich erreichbar.

Gas/Wasser: Beschaffung oder Flaschenwechsel eintragen; daraus lernen Bedarf und Reichweite. Eine reine Inhaltskorrektur unter „Bekannter Vorrat“ korrigiert den Bestand, ohne als neuer Verbrauch zu zählen. Details unter [Gas und Wasser](GAS-WASSER.md).

Die portable Einstellungssicherung enthält auch Dashboards, Kurzwahlen und Erscheinungsbild. Verbindungskennungen bleiben außerhalb dieser portablen Sicherung. Für einen vollständigen Geräteumzug die private Gesamtsicherung verwenden und niemals auf GitHub veröffentlichen. Hintergrund-/Bluetooth-Freigaben hängen am Android-Gerät und müssen dort kontrolliert werden.
