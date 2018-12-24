package com.amazon.customskill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ListUtilities {
	
	public static <T> ArrayList<T> union(ArrayList<T> list1, ArrayList<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }
	
	public static <T> ArrayList<T> intersection(ArrayList<T> list1, ArrayList<T> list2) {
        ArrayList<T> list = new ArrayList<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
	
	 public static ArrayList<Restaurant> sortListByDistance(ArrayList<Restaurant> list) {	 
		list.sort(Comparator.comparingDouble(Restaurant::getDistance)); 
		return list;
	 }
	 
	 public static ArrayList<Restaurant> sortListByRating(ArrayList<Restaurant> list) {	 
			list.sort(Comparator.comparingDouble(Restaurant::getRating).reversed()); 
			return list;
		 }
}
