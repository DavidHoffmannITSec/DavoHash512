package org.example;

public class Main {
    public static void main(String[] args) {
        String input = "Hhjgvukzfgjuztrfik7u 6zcgh,jkzb][ukv zhbmulbgj9oihgbjhv" ;
        String hashValue = DaHoEncryption.hash(input);
        System.out.println("Hash-Wert: " + hashValue);
    }

}