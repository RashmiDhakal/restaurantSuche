package com.amazon.customskill;

import java.util.ArrayList;

public class StringFilter {
    public static String[] numbers = new String[] {
        "null",
        "ein",
        "eine",
        "eins",
        "zwei",
        "drei",
        "vier",
        "fünf",
        "sechs",
        "sieben",
        "acht",
        "neun",
        // 10 - 19
        "zehn",
        "elf",
        "zwölf",
        "dreizehn",
        "vierzehn",
        "fünfzehn",
        "sechzehn",
        "siebzehn",
        "achtzehn",
        "neunzehn",
        //20-90
        "zwanzig",
        "dreißig",
        "vierzig",
        "fünfzig",
        "sechzig",
        "siebzig",
        "achtzig",
        "neunzig",
        // >=10^2
        "hundert",
        "tausend",
        "million",
        "milliard",
        //Teile?
        "halb",
        "halbes",
        "komma"
    };
    
    public static String FilterForDistance(String input) {
        String copy = input;
        String[] words = copy.split("\\s+");
        ArrayList<String> result = new ArrayList<String>();
        for(String word: words) {
            for(String number: numbers) {
                if(word.startsWith(number) || word.equals("und") || word.contains("meter") ){
                    result.add(word);
                    break;
                }
            }
        }
        return String.join("", filterResult(result)).toLowerCase();
    }
    
    private static ArrayList<String> filterResult(ArrayList<String> result) {
        for(int i = 0; i < result.size(); i++) {
            String word = result.get(i);
            boolean delete = true;
            for(String number: numbers) {
                if(word.endsWith(number) || word.equals("und") || word.contains("meter")) {
                   delete = false;
                   break; 
                }
            }
            if(delete) {
                result.remove(word);
                i--;
            }
        }
        return result;
    }
}