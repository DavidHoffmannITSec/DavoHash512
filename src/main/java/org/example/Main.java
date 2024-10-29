package org.example;

public class Main {
    public static void main(String[] args) {
        String input = "Hallo";
        String hashValue = DaHoEncryption.hash(input);
        System.out.println("Hash-Wert: " + hashValue);
    }

}