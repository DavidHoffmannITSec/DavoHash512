package org.example;

public class Main {
    public static void main(String[] args) {
        String input = "H2";
        String hashValue = DavoHash512.hash(input);
        System.out.println("Hash-Wert: " + hashValue);
    }

}