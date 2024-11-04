# Davo512 Hash-Algorithmus

Davo512 ist ein individueller, robuster Hash-Algorithmus, der speziell für hohe Sicherheit und effiziente Berechnung entwickelt wurde. Dieser Algorithmus wandelt eine beliebige Eingabe (z. B. Text, Zahlen, Sonderzeichen) in einen einzigartigen, festen 512-Bit-Hash-Wert um.
## Funktionsweise

Der Davo-Algorithmus verarbeitet die Eingabe in mehreren Schritten:

1. **Normalisierung der Eingabe**  
   Die Eingabe wird standardisiert, um sicherzustellen, dass verschiedene Darstellungen desselben Inhalts gleich behandelt werden.

2. **Padding (Auffüllung)**  
   Der Algorithmus fügt der Eingabe spezielle Füllwerte hinzu, um sicherzustellen, dass die Eingabe immer in gleich großen Datenblöcken (64 Byte) verarbeitet wird.

3. **Blockverarbeitung und parallele Berechnung**  
   Bei größeren Eingaben wird die Verarbeitung parallelisiert. Das bedeutet, dass mehrere Datenblöcke gleichzeitig berechnet werden, um die Geschwindigkeit bei großen Eingaben zu erhöhen.

4. **Adaptive Konstanten und dynamische Rundenanzahl**  
   Der Davo-Algorithmus verwendet adaptive Konstanten und passt die Rundenanzahl an die Eingabelänge an. Längere Eingaben erfordern mehr Runden, wodurch die Sicherheit erhöht wird.

5. **Zustandsvektor und Feedback-System**  
   Der Zustandsvektor speichert den aktuellen Hash-Zustand und wird bei jeder Runde aktualisiert. Das Feedback-System sorgt für eine kontinuierliche Rückkopplung, was bedeutet, dass jeder Block das Ergebnis des vorherigen Blocks beeinflusst.

6. **S-Box und Permutationsmatrix**  
   Der Algorithmus verwendet eine S-Box, die jeden Block einer "Substitution" unterzieht. Dadurch wird die Nichtlinearität erhöht, was die Hash-Werte widerstandsfähiger gegen Mustererkennung macht. Eine Permutationsmatrix sorgt für zusätzliche Bitmusterverteilung.

## Beispiel

Der folgende Aufruf erzeugt einen Hash für die Eingabe "Hallo":

```java
String hashValue = DavoHash.hash("Hallo");
System.out.println("Hash-Wert: " + hashValue);
```

Das ist der Output: "6c78db2014e7da662e2cba57d32ac779ec6b58e29918d0d170e4e5fc49c7759d513abca7d1079addda43fae7e749bf92b1d6ca1d6915999d2885d26348196fb"
