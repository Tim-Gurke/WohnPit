# Gemeinsame Entwicklung und Veröffentlichung

WohnPit führt die bisherige Zentrale und das Watt’n’Pit-Cockpit in einer App zusammen. Sie bietet Zentrale, Anzeigehandy und eigenständigen Betrieb ohne Zentrale. Gemeinsame Einstellungen und Versorgungseinträge müssen von den Anzeigehandys aus bedienbar sein. Im Zentralbetrieb sammelt und archiviert die Zentrale Wetter- und Messdaten; die Anzeigehandys übernehmen aufwendige Berechnungen und Verbrauchslernen. Im eigenständigen Betrieb übernimmt ein Handy alle Aufgaben.

Bei jeder Veröffentlichung APK mit der vorhandenen Updatesignatur bereitstellen, passende Prüfungen ausführen, aktuelle Screenshots sichtbar im README verlinken, ausführliche Anleitungen und eine vollständige dokumentierte Versionshistorie pflegen. Die kompatiblen App-Versionen und die Update-Reihenfolge nennen. Nach erfolgreicher Veröffentlichung höchstens die drei neuesten veröffentlichten Releases je Repository behalten; Quellhistorie und Änderungsdokumentation erhalten.

Vorhandene Messdaten, Kopplungen und die eigenen Kachelanordnungen je Gerät und Ausrichtung erhalten. Neue Kacheln sind frei wählbar und verwenden das bestehende Raster. Reichweiten für Gas und Wasser in Tagen und Stunden anzeigen, etwa „1 Tag 17 Stunden“. Fehlende Werte bleiben unbekannt; manuelle Angaben und daraus berechnete Vorräte als Schätzung kennzeichnen. Tatsächliche Hardwaretests nicht aus Softwareprüfungen ableiten. Private Signierschlüssel, Geräteschlüssel und persönliche Beispielmessungen gehören nicht in öffentliche Repositories.

## Gemeinsamer Standard für alle Pit-Apps

Gilt für Watt’n’Pit, WohnPit, ScootPit BMS und AirPit sowie zukünftige Apps und Repositories von Tim. Vereinbart am 05.11.2026. Bei den nächsten regulären Updates jede App anhand dieser Vorgaben prüfen, bereinigen und vereinheitlichen. Die folgenden Punkte sind Anforderungen für die Umsetzung, keine Behauptung, dass alle Apps sie bereits erfüllen.

- Zusammengehörige Messwerte erhalten zentral verwaltete Farben. Eine Änderung gilt in Kachelauswahl, Zahlen, Rundinstrumenten, Diagrammen und Detailansichten. Farbcodes als #RRGGBB anzeigen, eingeben und kopieren lassen; Einstellungen mit sichern. Bewusste eigene Kachelfarben klar kenntlich machen. In Watt’n’Pit bleiben Solar orange, Batterie grün und berechnete Akkubilanz hellblau; Wasser und Gas erhalten unterscheidbare eigene Farben. Benutzerdefinierte Farben unverändert übernehmen.
- Bedienung, Einstellungen, App-Kopf mit Versionsnummer, Hell-/Dunkelansicht, große Bedienelemente und Copyright-Vermerk appübergreifend konsistent gestalten, soweit fachlich passend. Aktive, pausierte und nicht verfügbare Funktionen klar beschriften; nicht verfügbare Aktionen erklären und deaktivieren. Bedeutung nie ausschließlich durch Farbe vermitteln.
- Datenabrufe nachvollziehbar machen: Zeitpunkt allein belegt keine Vollständigkeit. Bei rückwirkenden Abrufen den tatsächlich gespeicherten Zeitraum, Datenort/Quelle, vorhandene und fehlende Tage sowie laufenden Abruf und Fehler zeigen. Vorhersagen gelten nicht als geladene historische Wetterdaten. Unbekannte Werte nicht als Null oder erfolgreich geladen darstellen.
- Vorhandene Messdaten, Profile, Kopplungen und pro Gerät/Ausrichtung gespeicherte Layouts erhalten. Eine ausdrücklich gewünschte Layoutübernahme höchstens einmal ausführen; spätere eigene Änderungen erhalten. Umbauten mit Datenmigration und kompatiblen Versionsständen dokumentieren.
- Jede Veröffentlichung enthält die installierbare APK mit bestehender Updatesignatur, aktuelle sichtbare Screenshots im README, ausführliche Installations-, Einrichtungs-, Bedien- und Update-Anleitungen sowie eine vollständige Versionshistorie. Passende Prüfungen ausführen und deren Grenzen nennen. Keine realen Hardwaretests behaupten, wenn nur Softwareprüfungen erfolgt sind.
- Nach erfolgreicher Veröffentlichung nur die drei neuesten Releases je Repository veröffentlicht lassen. Ältere Release-Veröffentlichungen entfernen, Quellhistorie, Tags und Änderungsdokumentation erhalten. Private Schlüssel, Zugangsdaten und persönliche Mess-/Standortdaten nicht veröffentlichen.

## Beschlossene Zusammenführung unter WohnPit

Am 05.11.2026 ausdrücklich beauftragt: WohnPit und Watt’n’Pit unter WohnPit weiterführen. Eine gemeinsame App bietet Zentrale (schlanke native Ansicht), Anzeigehandy (Daten einer Zentrale) und eigenständigen Betrieb (Victron direkt; Aufzeichnung, Wetter, Berechnung und Anzeige auf demselben Handy). Keine Zentrale voraussetzen. Die bestehende Watt’n’Pit-Updatekennung und Signatur sowie ScootPit-Schnittstelle bleiben für den sicheren Übergang technisch erhalten. Der öffentliche Name und das führende Repository sind WohnPit.

Vor der Umstellung eine Übernahmeversion der bisherigen WohnPit-Zentrale mit vollständiger verschlüsselter Datensicherung bereitstellen. Aufzeichnungen, Journal, Wetter und Lernstände haben Vorrang; neu verknüpfen ist als Rückweg akzeptiert. Daten erst nach Prüfung und ausdrücklichem Import ersetzen, bisherige Installation und Rückweg erhalten. Alte Quellhistorien und dokumentierte Releases nicht löschen.

WohnPit als lokales Android-Cockpit für Energie, Gas und Wasser im Wohnwagen, Ferienhaus, Gartenhaus und an passenden Balkonanlagen vermitteln. Tatsächliche Victron-Kompatibilität und lokale WLAN-Grenzen klar nennen. Öffentliche Projektseite mit verständlichen Suchtexten, sichtbaren Screenshots, Downloads, Anleitungen und vollständiger Versionshistorie pflegen. Google-Indexierung und Platzierung nicht garantieren.

## Private Übernahmedaten

Die vollständige Umzugsdatei enthält private Verknüpfungen, Geräteschlüssel und Kennwörter. Sie darf auch verschlüsselt niemals öffentlich auf GitHub, in Releases, Prüfartefakten oder auf der Projektseite erscheinen. Nur lokal auswählen/speichern, mit eigenem Kennwort authentifiziert verschlüsseln; das Kennwort nicht speichern oder protokollieren. Signierschlüssel und echte private Einstellungs-/Messdaten ebenfalls ausschließen. Vor jedem Hochladen die vorgemerkte Dateiliste mit tools/check-public-files.py prüfen; nur neutrale Testdaten veröffentlichen.

Wenn ein altes Übernahme-Release später aus den letzten drei Releases fällt, die notwendige signierte Übernahme-APK als zusätzliches Hilfsprogramm in den aktuellen Releases weiter anbieten; private Umzugsdateien niemals anhängen.

## Bekannte Grenze der Übernahme aus 0.0.5

Die Sicherung aus 0.0.5 kopiert die Geräte-Konfiguration einschließlich der `secret`-Felder, überträgt aber nicht den AndroidKeyStore der alten App. Beim Wechsel `de.wohnpit.app` → `de.wattpit.app` sind diese verschlüsselten Victron-Schlüssel für WohnPit 2.0.1 nicht lesbar. Aktueller Wiederherstellungsweg: nach dem Import beide 32-stelligen Instant-Readout-Schlüssel in den vorhandenen Geräteeinträgen der neuen Zentrale erneut speichern. Messwertarchive und Geräteadressen dabei erhalten. Die Umzugsanleitung nennt diesen Schritt ausdrücklich.

Eine künftige automatische Schlüsselübernahme muss die Schlüssel in der Quell-App entschlüsseln, ausschließlich innerhalb der kennwortgeschützten vollständigen Sicherung übertragen und für den AndroidKeyStore der Ziel-App neu verschlüsseln. Keine ungeschützten öffentlichen Schlüsseldateien oder Protokolle erzeugen. Der Import muss auf nicht übertragbare Schlüssel hinweisen, statt allein aus dem vorhandenen verschlüsselten Feld einen funktionsfähigen Schlüssel abzuleiten.

Dafür einen Android-Test mit tatsächlich getrennten App-UIDs beziehungsweise Installationen und echten AndroidKeyStore-Schlüsseln verwenden. Ein ContextWrapper mit verändertem Paketnamen und ein synthetischer Klartext-Einstellungswert prüfen die UID-Grenze nicht. Erst nach dieser Prüfung eine automatische Übernahme der Victron-Schlüssel zusagen.

## AutarPie ab 3.0.0

Am 12.11.2026 beauftragt: sichtbarer Name AutarPie und Ressourcen-Kreislauf-Symbol. Das bestehende Repository wurde am 13.11.2026 von `Tim-Gurke/WohnPit` in `Tim-Gurke/AutarPie` umbenannt. Neue Projekt-, Download- und Dokumentationslinks verwenden `Tim-Gurke/AutarPie`. Paket `de.wattpit.app`, vorhandene Updatesignatur, Daten, Kopplungen und ScootPit-Schnittstelle bleiben erhalten. Die neuen Bereichsfarben ersetzen die alten Standardfarben; bewusste eigene Farben bleiben erhalten.

Alle Prognosen, Energieauswertungen, Lernmodelle und Statistiken laufen auf Anzeigehandys oder im eigenständigen Betrieb. Die native Zentrale sammelt, speichert und synchronisiert Grundlagen und führt bestätigte Steueraufträge sowie zeitlich begrenzte Ladepläne mit direkten Abschaltbedingungen aus. Keine neue automatische Ladung ohne gültigen Anzeigeplan. Hardwarestatus und gesendeten Auftrag getrennt darstellen.

Alle Standardbereiche und selbst erstellte Dashboards verwenden denselben Kacheleditor. Layouts sind gerätelokal, Bereichsfarben gemeinsam; Einzelkachel-Abweichungen erkennbar erhalten. Entfernen einer Kachel erfordert eine Bestätigung. Darstellungsgröße nur nach ausdrücklichem Speichern ändern.
