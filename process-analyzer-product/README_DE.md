# Prozess Analyzer
Das Process-Analyzer-Tool von Axon Ivy bietet dir umfassende Einblicke in deine Workflow-Prozesse.

Hauptfunktionen:

- **Prozessdiagramme visualisieren**: Eine klare und intuitive Darstellung deiner Prozesse wird durch dynamische Diagramme ermöglicht, die du schon aus Axon Ivy gewohnt bist.
- **Übersichtliche Statistiken**: Visualisiere Nutzungsdaten und Auswertungen zur Aufgabendauer direkt im Prozessdiagramm.
- **Zeitbasierte Analyse**: Filtere und analysiere Daten für spezifische Zeitintervalle
- **Benutzerdefinierte Attributfilter**: Nutze prozessspezifische Attribute (z. B. benutzerdefinierte Felder aus Aufgaben oder Fällen), um deine Analyse detailliert zu verfeinern.
- **Flexible Prozessauswahl**: Analyse Prozesse einem Security Kontext 
- **Export-Funktionen**: Exportiere Prozessdiagramme und detaillierte Excel-Berichte mit Nutzungs- und Dauerstatistiken.
  
## Demo

- Select the PMV (within the same security context) that matches the desired process and KPI type, then click the **"Show Statistic"** button. This will display a raw data visualized process diagram alongside an analyzed statistics table. *(In this version, the data is limited to **"DONE"** cases with fewer than **TWO** alternative elements in their process.)*

![alt text](image1.png)

- To gain deeper insights, additional filter criteria have been included: time intervals and custom field values.
  1) Time interval filter: By default, this filter includes all cases whose start timestamp falls within the specified time range.
  2) Custom filter: This option allows users to filter cases based on custom field values (from the case or task) that match the specified conditions.

![alt text](image2.png)

- For reporting purposes, users can export a Excel file containing analyzed data or a FullHD diagram with KPI values directly from the two buttons located at the bottom-right corner of the UI.

![alt text](image3.png)

## Setup

```
@variables.yaml@
```
