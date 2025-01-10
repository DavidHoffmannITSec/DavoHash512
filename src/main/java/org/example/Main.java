package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
      /*  String input = "Hi";
        byte[] hash = DavoHash512.hash(input);
        System.out.println("Hash: " + DavoHash512.bytesToHex(hash));
*/

        String filePath = "C:/Users/PC/Documents/test.txt";

        // Datei hashen
        File file = new File(filePath);
        byte[] fileHash = DavoHash512.hashFile(file);

        // Hash als Hex-String ausgeben
        String hexHash = DavoHash512.bytesToHex(fileHash);
        System.out.println("Datei-Hash: " + hexHash);


    }

}