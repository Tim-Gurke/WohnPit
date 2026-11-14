# AutarPie einrichten und benutzen

**Stand 3.0.6:** [Rollerlernen bei Abwesenheit, gekennzeichnete Ladehäufigkeit und gemeinsame Kurzwahlfarbe](changes-3.0.6.md). Zuerst die Zentrale, danach alle Anzeigen auf 3.0.6 aktualisieren.

## Rollen und Wiederanlauf

Die **Zentrale** erhält Bluetooth-Verbindungen, Messdaten, Wettergrundlagen und den lokalen Anlagenverbund. Ihre native Oberfläche zeigt Betriebszustände und direkte Bedienaktionen. Die **Anzeige** berechnet und zeichnet aus den Grundlagen einer gekoppelten Zentrale. **Eigenständig** kombiniert Erfassung, Berechnung und Cockpit auf einem Handy.

Unter Einstellungen → Zentrale & Verbindungen → Aufzeichnung, Archiv & Autostart lässt sich der Wiederanlauf einschalten. AutarPie nach Installation einmal öffnen und die Android-Freigaben erteilen. Eine ausgeschaltete Aufzeichnung wird durch Autostart nicht eingeschaltet. Ein geplanter täglicher kompletter Handy-Neustart wird weiterhin außerhalb der App eingerichtet; AutarPie übernimmt danach den Wiederanlauf, soweit Android ihn freigibt.

## Rollerbedarf aus Ladungen und Ladeweg lernen

E-Scooter → **Verbrauch lernen** zeigt Energie je ausreichend erfasster Ladung und die Grundlage der üblichen Häufigkeit getrennt. Der Roller benötigt dafür keinen ganztägigen BMS-Empfang. Bei unbekannter Häufigkeit unter Bearbeiten eine vorläufige Zahl von Ladungen pro Woche eingeben; 0 lässt sie offen. Geeignete bisherige Tages-/Kilometerschätzungen bleiben als eigene Annahme nutzbar. Mindestens drei vollständig beobachtete abgeschlossene Ladeweg-Tage können die tatsächliche Häufigkeit beziehungsweise den Tagesdurchschnitt liefern.

Dazu den tatsächlich ausschließlich für den Roller verwendeten Ladeweg unter Lastausgang → Einrichten entsprechend zuordnen und aktiv auslesen. Bestätigte Aus-Zeiten können auch bei abwesendem Roller ausgewertet werden; eine Funklücke oder eine abgeschaltete Zentrale ist keine bestätigte Nullnutzung. Bei Rückkehr ersetzen frische BMS-Werte den pauschalen Ansatz einer konkret geplanten Ladung. [Ausführliche Bedienung und Grenzen](changes-3.0.6.md).

## Verbrauchslernen aus dem gespeicherten Verlauf

Nach dem Update gleicht die Anzeige einmalig auch ältere vorhandene Messdaten ab, selbst wenn schon einige gelernte Abschnitte importiert waren. Dazu mit der Zentrale verbinden und die Anzeige bis zum Ende des Abgleichs geöffnet lassen. Vorhandene Rohdaten bleiben erhalten; der Abgleich läuft auf der Anzeige. Ein unverändert übertragener Datenblock muss beim nächsten Abgleich nicht erneut geladen werden.

Solarvorschau → Datengrundlage unterscheidet ausgewertete Stunden, gespeicherten Messzeitraum, andere Anwesenheitsprofile und unbekannte Anwesenheit. Neu bewertete Ausschlüsse werden dem jeweiligen Profil zugeordnet, ältere nicht zuordenbare Gründe getrennt angezeigt. Ein großer Messzeitraum belegt noch keine lückenlosen oder vollständig geeigneten Lernstunden. Fehlende Grundlagen bleiben unbekannt; archivierte Einstrahlung gehört immer zum damaligen Standort und zur damaligen Aufstellung.

Einstellungen → Daten & Sicherung → Verbrauchslernen und Diagnose bietet **Gespeicherten Verlauf neu auswerten**. Dies fordert den historischen Abgleich erneut an. Eine Anzeige benötigt dafür die erreichbare Zentrale. **Diagnose speichern** exportiert dagegen den bereits lokal verfügbaren Analyse- und Messstand der ausgewählten Anlage, auch wenn die Zentrale gerade offline ist. Der Export kennzeichnet diesen gespeicherten Stand und vermischt ihn nicht mit unabhängigen Daten des Anzeigehandys. [Details und Prüfgrenzen](changes-3.0.5.md).

## Bereiche, Kurzwahlen und eigene Dashboards

„Menü“ im Kopf öffnet alle Anwendungen, auch wenn ihre untere Kurzwahl ausgeblendet ist. „Kurzwahlen & Dashboards“ bietet Ein-/Auswahl und Reihenfolge. Ziehen am Griff oder die Pfeile ändern sofort die sichtbare Liste; die verschobene Zeile wird kurz hervorgehoben. Android-Zurück führt vom Einstellungsdialog zur Gruppe, zur Einstellungsübersicht und zum Menü auf dem zuvor geöffneten Dashboard. Dort lassen sich eigene Dashboards umbenennen, kopieren und löschen. „Eigenes Dashboard“ startet leer oder übernimmt eine Kopie eines vorhandenen Dashboards. Eine Kopie lässt die Vorlage unverändert.

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

## Neue Funktionen ab 3.0.1

**Standardansicht ausprobieren:** Menü → Einstellungen → Daten & Sicherung → Ansicht sichern. Anschließend „Zurücksetzen auswählen“ öffnen, gewünschte Bereiche markieren und übernehmen. Für die komplette Standardansicht „Alles auswählen“. Danach über „Wiederherstellen“ dieselbe Einstellungssicherung wählen und die gewünschten Bereiche zurückholen. Farben, Auswahl, Anordnung und Dashboards sind getrennt auswählbar. Messungen und Geräte werden durch diese Ansichtsfunktionen nicht gelöscht.

**Dashboard-Farbe:** Auf dem gewünschten Dashboard Menü → Farben dieses Dashboards. Das gilt auch für „Verlauf“: Der Dialog nennt die sichtbare Seite. Ohne aktiviertes eigenes Design gelten immer die gemeinsamen Farben, unabhängig von der vorher geöffneten Seite. Andere Dashboards werden beim Speichern nicht verändert. Eigenes Design aktivieren, Seiten-/Kachelflächen und gegebenenfalls eine gemeinsame Wertfarbe festlegen. Die Vorschau zeigt ihre gemeinsame Wirkung. Einzelne Kachelausnahmen bleiben erhalten; „auf alle Kacheln anwenden“ hebt ihre entsprechenden Ausnahmen ausdrücklich auf. Jeder Farbdialog beginnt mit der aktuellen Farbe und zeigt App-Farben sowie automatisch gemerkte eigene Mischfarben.

**Neue Kacheln:** Menü → Aktuelles Dashboard bearbeiten → Kachel hinzufügen. „Rollerladung“, „Restreichweite“, „Restladezeit“, Einzelzellwerte, Solarstrom gesamt und die einzelnen Lastausgangswerte stehen in derselben Auswahl. Zellen kommen aus dem BMS; ohne entsprechende aktuelle Antwort erscheinen keine errechneten Ersatzwerte. Bei Reglerwerten die passende Gerätezuordnung in der Kachel einstellen. Eine nach einem Import fehlende Gerätezuordnung bleibt sichtbar und verwendet keinen anderen Regler.

**Wirkungsgrad:** Unter E-Scooter oder Solar & Speicher → Lastausgang → Einrichten den Regler wählen, „E-Scooter“ zuordnen und bestätigen, dass ausschließlich die Rollerladung an diesem Ausgang hängt. Aktive Lastmessung einschalten. Aktueller Wirkungsgrad benötigt zeitlich passende, unabhängige Eingangs- und BMS-Messwerte. Ladungswirkungsgrad verwendet nur gemeinsam gemessene Abschnitte. Profil-Wirkungsgrad ist eine Annahme für Planung/Schätzung und wird nicht als gemessener Wirkungsgrad ausgegeben. Restreichweite benötigt Wh/km im Rollerprofil; Restladezeit verwendet dessen Ladeziel.

**Eigene Rechnung:** Kachel „Eigene Formel“ hinzufügen. Messwerte für A/B auswählen, bei Bedarf Regler, Einheit und Zellnummer bestimmen. Vorlage wählen oder etwa `=A/B*100` eingeben. Faktor und Korrekturwert werden anschließend angewendet. Die Vorschau nennt Quelle und Werte. Einheiten und Plausibilität prüfen; keine Gerätebefehle werden ausgeführt. Für einen Verlauf einen gemeinsamen Zeitraum wählen; nur geeignete tatsächlich gespeicherte Eingangsgrößen können eine vergangene Kurve liefern.

**Wetter:** Seitlich nach links zu bis zu drei vorhandenen vergangenen Tagen wischen; „Heute“ kehrt zum aktuellen Tag zurück. Die normale Vorschau zeigt bis zu 16 Kalendertage einschließlich heute. Menü → Einstellungen → Solaranlage → Wettertrend aktiviert die zusätzliche grobe Wochenübersicht bis 46 Tage. Die Zusatzfunktion beginnt ausgeschaltet und beeinflusst die normale Akku-Prognose nicht.

**Unterwegs Wasser/Gas eintragen:** Auf dem bereits gekoppelten Anzeigehandy wie gewohnt erfassen. Offline erscheint der Eintrag als „Übertragung ausstehend“. Die Warteschlange überlebt einen Neustart. Bei Rückkehr zur bekannten Zentrale erfolgt die Übertragung automatisch; deren Bestätigung schließt den Vorgang ab. Ausstehende Einträge lassen sich prüfen; Konflikte werden erklärt. Vor der Bestätigung die App-Daten nicht löschen.

**Last schalten:** Ein Schaltwunsch darf einen Archivabruf unterbrechen und wartet erforderlichenfalls auf eine laufende Liveabfrage. Die Anzeige unterscheidet vorgemerkt, gesendet und bestätigt. Nach 45 Sekunden verfällt ein nicht ausgeführter wartender Auftrag; bei fehlender Bestätigung Verbindung prüfen und erneut bedienen. „Aus“ pausiert zugleich die Automatik. Firmware-Schutzbedingungen und ein gemeldeter Abschaltgrund bleiben vom tatsächlichen Schaltzustand getrennt.

[Alle Änderungen und Prüfgrenzen von 3.0.1](changes-3.0.1.md)
