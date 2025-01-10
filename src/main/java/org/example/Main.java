package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        String input = "Hi";
        byte[] hash = DavoHash512.hash(input);
        System.out.println("Hash: " + DavoHash512.bytesToHex(hash));

    }

}