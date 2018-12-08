package com.amazon.customskill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class NumberWordsToNumbers {
    private HashMap<String, Integer> numbers;
    
    private Integer multiplier;
    
    private String currentState;
    
    public NumberWordsToNumbers() {
        this.multiplier = 1;
        this.numbers = new HashMap<String, Integer>();
        // 10 - 19: longer words first, otherwise e.g. "dreißig" will be recognized as "drei"
        this.numbers.put("zehn", 10);
        this.numbers.put("elf", 11);
        this.numbers.put("zwölf", 12);
        this.numbers.put("dreizehn", 13);
        this.numbers.put("vierzehn", 14);
        this.numbers.put("fünfzehn", 15);
        this.numbers.put("sechzehn", 16);
        this.numbers.put("siebzehn", 17);
        this.numbers.put("achtzehn", 18);
        this.numbers.put("neunzehn", 19);
        //20-90
        this.numbers.put("zwanzig", 20);
        this.numbers.put("dreißig", 30);
        this.numbers.put("vierzig", 40);
        this.numbers.put("fünfzig", 50);
        this.numbers.put("sechzig", 60);
        this.numbers.put("siebzig", 70);
        this.numbers.put("achtzig", 80);
        this.numbers.put("neunzig", 90);
        // 10^0
        this.numbers.put("null", 0);
        this.numbers.put("ein", 1);
        this.numbers.put("eine", 1);
        this.numbers.put("eins", 1);
        this.numbers.put("zwei", 2);
        this.numbers.put("drei", 3);
        this.numbers.put("vier", 4);
        this.numbers.put("fünf", 5);
        this.numbers.put("sechs", 6);
        this.numbers.put("sieben", 7);
        this.numbers.put("acht", 8);
        this.numbers.put("neun", 9);
        
        // >=10^2
        this.numbers.put("hundert", 100);
        this.numbers.put("tausend", 1000);
        this.numbers.put("million", 1000000);
        this.numbers.put("milliard", 1000000000);
    }
    
    public Integer Umwandeln(String str) throws Exception {
        Integer result = 0;
        this.currentState = str;
        prepareString();
        List<Integer> nachkommastellen = this.nachkommastellen();
        List<Integer> splited = this.split(this.currentState);
        for(int i = 0; i < splited.size(); i++) {
            if(splited.get(i) < 10) {
                if(i < splited.size() - 1 && splited.get(i+1) > 99) {
                    result += splited.get(i) * splited.get(i+1);
                    i++;
                } else {
                    result += splited.get(i);
                }
            } else {
                result += splited.get(i);
            }
        }
        result *= this.multiplier;
        for(int i = 0; i < nachkommastellen.size(); i++) {
            result += nachkommastellen.get(i) * this.multiplier / (int)Math.pow(10, i + 1);
        }
        return result;
    }
    
    private ArrayList<Integer> split(String str) throws Exception {
        String cpy = str;
        ArrayList<Integer> result = new ArrayList<Integer>();
        int length = cpy.length();
        while(cpy.length()>0) {
            for(String unit : this.numbers.keySet()) {
                if(this.numbers.containsKey(cpy)) {
                    result.add(this.numbers.get(cpy));
                    cpy = cpy.substring(cpy.length());
                } else {
                    if(cpy.startsWith("und")) {
                        cpy = cpy.substring("und".length());
                    } 
                    if(cpy.startsWith(unit)) {
                        result.add(this.numbers.get(unit));
                        cpy = cpy.substring(unit.length());
                    }
                }
            }
            if(cpy.length()== length){
                throw new Exception("Keine gültige Zahl");
            }
        }
        return result;
    }
    
    private void prepareString() throws Exception {
        if(this.currentState.matches(".*km.*|.*kilometer.*")) {
            this.multiplier = 1000;
        }
        if(this.multiplier == 1 && (this.currentState.contains("komma")|| this.currentState.contains("halb"))) {
            throw new Exception("How exactly are you going to measure it?");
        }
        this.currentState = this.currentState.replaceAll("\\s*(kilometer|meter)\\s*", "");
        this.currentState = this.currentState.replaceAll("\\s+", "");
    }
    
    private ArrayList<Integer> nachkommastellen() throws Exception {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(this.currentState.contains("halb")) {
            result.add(5);
            this.handleHalf();
            return result;
        }
        if(this.currentState.contains("komma")) {
            String cpy = this.currentState;
            cpy = cpy.substring(cpy.indexOf("komma"));
            cpy = cpy.substring("komma".length());
            result = this.split(cpy);
            if(result.size() > 3) {
                throw new Exception("How exactly are you going to measure it?");
            }
            this.currentState = this.currentState.substring(0, this.currentState.indexOf("komma"));
        }
        return result;
    }
    
    private void handleHalf() {
        if(this.currentState.equals("anderthalb") || this.currentState.equals("einundhalb")){
            this.currentState = "eins";
        }
        this.removeSubstring("einhalbes");
        this.removeSubstring("einenhalben");
        this.removeSubstring("halbes");
        this.removeSubstring("halben");
        this.removeSubstring("halbe");
        this.removeSubstring("einhalb");
        this.removeSubstring("undhalb");
    }
    
    private void removeSubstring(String substring) {
        if(this.currentState.contains(substring)){
            this.currentState = this.currentState.substring(0, this.currentState.indexOf(substring));
        }
    }
}