package com.amazon.customskill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class NumberWordsToNumbers {
    private HashMap<String, Integer> numbers;
    private HashMap<String, Integer> numbersOver10;
    private HashMap<String, Integer> numbersOver20;
    private HashMap<String, Integer> numbersOver100;
    
    private Integer multiplier;
    
    private String currentState;
    
    public NumberWordsToNumbers() {
        this.multiplier = 1;
        this.numbers = new HashMap<String, Integer>();
        this.numbersOver10 = new HashMap<String, Integer>();
        this.numbersOver20 = new HashMap<String, Integer>();
        this.numbersOver100 = new HashMap<String, Integer>();
        
        // 10 - 19: longer words first, otherwise e.g. "dreißig" will be recognized as "drei"
        this.numbersOver10.put("zehn", 10);
        this.numbersOver10.put("elf", 11);
        this.numbersOver10.put("zwölf", 12);
        this.numbersOver10.put("dreizehn", 13);
        this.numbersOver10.put("vierzehn", 14);
        this.numbersOver10.put("fünfzehn", 15);
        this.numbersOver10.put("sechzehn", 16);
        this.numbersOver10.put("siebzehn", 17);
        this.numbersOver10.put("achtzehn", 18);
        this.numbersOver10.put("neunzehn", 19);
        //20-90
        this.numbersOver20.put("zwanzig", 20);
        this.numbersOver20.put("dreißig", 30);
        this.numbersOver20.put("vierzig", 40);
        this.numbersOver20.put("fünfzig", 50);
        this.numbersOver20.put("sechzig", 60);
        this.numbersOver20.put("siebzig", 70);
        this.numbersOver20.put("achtzig", 80);
        this.numbersOver20.put("neunzig", 90);
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
        this.numbersOver100.put("hundert", 100);
        this.numbersOver100.put("tausend", 1000);
        this.numbersOver100.put("million", 1000000);
        this.numbersOver100.put("milliard", 1000000000);
    }
    
    public Integer Umwandeln(String str) throws Exception {
        Integer result = 0;
        this.currentState = str;
        prepareString();
        List<Integer> nachkommastellen = this.nachkommastellen();
        List<Integer> splited = this.split(this.currentState);
        ArrayList<Integer> resArr = new ArrayList<Integer>();
        for(int i = 0; i < splited.size(); i++) {
            if(splited.get(i) > 99 || splited.get(i) < 10) {
                if(i > 0 &&  splited.get(i-1) < splited.get(i)) {
                    result *= splited.get(i); 
                } else {
                    resArr.add(result);
                    result = splited.get(i);
                }
            } else {
                result += splited.get(i);
            }
        }
        result += sum(resArr);
        result *= this.multiplier;
        for(int i = 0; i < nachkommastellen.size(); i++) {
            result += nachkommastellen.get(i) * this.multiplier / (int)Math.pow(10, i + 1);
        }
        return result;
    }
    
    private Integer sum(ArrayList<Integer> list) {
        Integer result = 0;
        for(Integer elem: list) {
            result += elem;
        }
        return result;
    }
    
    private ArrayList<Integer> split(String str) throws Exception {
        String cpy = str;
        ArrayList<Integer> result = new ArrayList<Integer>();
        int length = cpy.length();
        while(cpy.length()>0) {
            cpy = splitByUnit(this.numbersOver100, cpy, result);
            cpy = splitByUnit(this.numbersOver20, cpy, result);
            cpy = splitByUnit(this.numbersOver10, cpy, result);
            cpy = splitByUnit(this.numbers, cpy, result);
            if(cpy.length()== length){
                throw new Exception("Keine gültige Zahl");
            }
        }
        return result;
    }
    
    private String splitByUnit(HashMap<String, Integer> units, String str, ArrayList<Integer> result) {
        String cpy = str;
        for(String unit : units.keySet()) {
            if(units.containsKey(cpy)) {
                result.add(units.get(cpy));
                return cpy.substring(cpy.length());
            } else {
                if(cpy.startsWith("und")) {
                    cpy = cpy.substring("und".length());
                } 
                if(cpy.startsWith(unit)) {
                    result.add(units.get(unit));
                    return cpy.substring(unit.length());
                }
            }
        }
        return cpy;
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