# AutarPie 3.0.2 – Hotfix für die Zentrale

Behebt einen mit 3.0.1 eingeführten Fehler: Die Zentrale konnte dauerhaft blockieren, keine Livewerte mehr liefern und den Android-Dialog „AutarPie reagiert nicht“ auslösen.

## Update

Die APK **über die vorhandene Zentrale installieren**, nicht deinstallieren und keine App-Daten löschen. Ein Zurücksetzen oder erneutes Einrichten ist nicht erforderlich. Nach dem Update AutarPie einmal öffnen und den gespeicherten Aufzeichnungszustand kontrollieren. Eine zuvor pausierte Aufzeichnung bleibt pausiert.

Paket `de.wattpit.app`, bisherige Updatesignatur, Geräteverknüpfungen, Messungen und Anordnungen bleiben erhalten. **Zuerst die Zentrale aktualisieren.** Anzeigehandys mit 3.0.1 sind mit diesem Hotfix kompatibel; auch sie können anschließend auf 3.0.2 aktualisiert werden. ScootPit BMS 1.6.14 bleibt unverändert kompatibel. Das ältere Hilfsprogramm für die getrennte WohnPit-Installation ist nur für einen noch ausstehenden Altumzug vorgesehen.

## Ursache und gezielte Korrektur

Der Datenabgleich hielt die Repository-Sperre und wartete auf den Energiezustand. Die neue Reglerzuordnung aus 3.0.1 hielt gleichzeitig die Energiezustands-Sperre und wartete auf dieselbe Repository-Sperre. Beide Abläufe konnten dadurch dauerhaft aufeinander warten. Die native Regleranzeige fragte diesen blockierten Zustand zusätzlich auf dem Android-Hauptthread ab.

- Die Gerätekonfiguration wird als unabhängige Kopie der bereits threadsicheren Einstellungen gelesen und wartet nicht auf den Repository-Monitor.
- Lastmesswerte werden unter einer kurzen Sperre als aktueller Zustand übernommen. Ihre SQLite-Archivierung und BMS-Zuordnung warten anschließend außerhalb dieser Livewertsperre.
- Die native Zentralenoberfläche liest Reglerzustände auf ihrem vorhandenen Hintergrund-Executor. Der Hauptthread übernimmt nur die fertigen Anzeigewerte.

Die Korrektur betrifft diese Sperr- und Anzeigewege. Datenformat, Übergabeprotokoll, Berechnungen, Kacheln und die Funktionen aus 3.0.1 werden weiterverwendet.

## Prüfung

Der unveränderte 3.0.1-Code wurde mit zwei gezielt gleichzeitig laufenden Produktionsaufrufen nachgestellt. Der Thread-Dump zeigt die gegenseitig blockierten Methoden `Repository.snapshot`, `EnergyStore.output/regulator` und `Repository.configs`. Mit der Korrektur schließen beide Aufrufe unter derselben erzwungenen Reihenfolge ab.

Der neue Regressionstest läuft in der normalen SQLite-/Verbund-Prüfstrecke. Ein zusätzlicher Android-Emulatortest hält die Reglerzustandssperre belegt und prüft, dass die Zentralenoberfläche trotzdem reagiert und nach Freigabe wieder den bestätigten Stromwert anzeigt. Die bestehende vollständige Build-, Lint-, Berechnungs-, Browser-, Übernahme- und Verbundprüfung bleibt Bestandteil der Freigabe. Der erfolgreiche Prüflauf und der geprüfte APK-Quellstand stehen im Release.

Ein ANR-Protokoll vom persönlichen G5 lag nicht vor. Der im Produktionscode reproduzierte Deadlock ist behoben; der erneute Betrieb am persönlichen Gerät muss nach Installation bestätigt werden. Software- und Emulatortests werden nicht als Funk- oder Hardwareprüfung ausgegeben.

[Bedienung](AUTARPIE-3.md) · [Alle Änderungen aus 3.0.1](changes-3.0.1.md) · [Versionshistorie](../CHANGELOG.md)
