# DaHo512 Hash-Algorithmus

DaHo512 ist ein individueller, robuster Hash-Algorithmus, der speziell für hohe Sicherheit und effiziente Berechnung entwickelt wurde. Dieser Algorithmus wandelt eine beliebige Eingabe (z. B. Text, Zahlen, Sonderzeichen) in einen einzigartigen, festen 512-Bit-Hash-Wert um und hängt den Tag `DaHo` an den Hash an, um den Hash als "DaHo-Hash" zu kennzeichnen.

## Funktionsweise

Der DaHo-Algorithmus verarbeitet die Eingabe in mehreren Schritten:

1. **Normalisierung der Eingabe**  
   Die Eingabe wird standardisiert, um sicherzustellen, dass verschiedene Darstellungen desselben Inhalts gleich behandelt werden.

2. **Padding (Auffüllung)**  
   Der Algorithmus fügt der Eingabe spezielle Füllwerte hinzu, um sicherzustellen, dass die Eingabe immer in gleich großen Datenblöcken (128 Byte) verarbeitet wird.

3. **Blockverarbeitung und parallele Berechnung**  
   Bei größeren Eingaben wird die Verarbeitung parallelisiert. Das bedeutet, dass mehrere Datenblöcke gleichzeitig berechnet werden, um die Geschwindigkeit bei großen Eingaben zu erhöhen.

4. **Adaptive Konstanten und dynamische Rundenanzahl**  
   Der DaHo-Algorithmus verwendet adaptive Konstanten und passt die Rundenanzahl an die Eingabelänge an. Längere Eingaben erfordern mehr Runden, wodurch die Sicherheit erhöht wird.

5. **Zustandsvektor und Feedback-System**  
   Der Zustandsvektor speichert den aktuellen Hash-Zustand und wird bei jeder Runde aktualisiert. Das Feedback-System sorgt für eine kontinuierliche Rückkopplung, was bedeutet, dass jeder Block das Ergebnis des vorherigen Blocks beeinflusst.

6. **S-Box und Permutationsmatrix**  
   Der Algorithmus verwendet eine S-Box, die jeden Block einer "Substitution" unterzieht. Dadurch wird die Nichtlinearität erhöht, was die Hash-Werte widerstandsfähiger gegen Mustererkennung macht. Eine Permutationsmatrix sorgt für zusätzliche Bitmusterverteilung.

7. **Zusätzliche Rotation und finale Zusammenführung**  
   Der DaHo-Algorithmus rotiert Bits dynamisch und kombiniert das Endergebnis im finalen Zustand. Der finale Hash wird in einen Hexadezimal-String umgewandelt, und der Tag `[DaHo]` wird angefügt.

## Beispiel

Der folgende Aufruf erzeugt einen Hash für die Eingabe "Hallo":

```java
String hashValue = DaHoEncryption.hash("Hallo");
System.out.println("Hash-Wert: " + hashValue);
```

Das ist der Output: "6c78db2014e7da662e2cba57d32ac779ec6b58e29918d0d170e4e5fc49c7759d513abca7d1079addda43fae7e749bf92b1d6ca1d6915999d2885d26348196fb"
