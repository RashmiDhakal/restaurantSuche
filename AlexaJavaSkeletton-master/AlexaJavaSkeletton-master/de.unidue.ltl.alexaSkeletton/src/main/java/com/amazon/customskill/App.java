package com.amazon.customskill;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class App {
	
    public static ArrayList<Restaurant> getData(String addresse) {
    	ArrayList<Restaurant> res = new ArrayList<Restaurant>();
    	String key = "Efio1-A9NjP2UHSaA5aGwn3IILFcHD39ISzq201w-pxaaaQ2MBiothsuZzUoVmulDTr0W8TPDhsAkt8qS1UpBURKhKFr-6V-EKuJZvFebQXOXiAEOGbLmNhIt_vWW3Yx";
    	HttpClient client = HttpClientBuilder.create().build();
    	HttpGet get = new HttpGet(getURL(getCoordinate(addresse)));    
		get.addHeader("Authorization", "Bearer " + key);
		String s = "";
	    try {
			HttpResponse response = client.execute(get);
			s = EntityUtils.toString(response.getEntity());
			res = stringToJSON(s);
		//	write_file(s, "Ausgabe");
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block and do
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return res;
    }
    
    public static void write_file(String text, String name) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(name + ".txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	    BufferedWriter bw = new BufferedWriter(fw);
	    try {
			bw.write(text);
			bw.close();
			if (name.equals("Ausgabe")) {
				stringToJSON(text);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
/*	private static String userInput() {
		Scanner reader = new Scanner(System.in);
		System.out.println("Gib die Stadt ein: ");
		String sity = reader.nextLine();
		System.out.println("Gib die Strasse ein: ");
		String street = reader.nextLine();
		System.out.println("Gib die Hausnummer ein: ");
		String homeNumber = reader.nextLine();
		reader.close();
		return street + " " +  homeNumber + ", " + sity;
	}
	*/
	
	private static String getCoordinate(String address) {
		Map<String, Double> coords;
	    coords = OpenStreetMapUtils.getInstance().getCoordinates(address);
	    return "" + coords.get("lat") + "," + coords.get("lon");
	}
	
	private static String getURL(String ll) {
		String URL = "https://api.yelp.com/v3/businesses/search?";
		String radius = "500";
		String term = "restaurant";
		String location = ll;
		String limit = "3";
		//HttpGet get = new HttpGet("https://api.yelp.com/v3/businesses/search?radius=500&term=restaurant&latitude=51.46484&longitude=7.01392&limit=10");
		URL += "radius=" + radius + "&term=" + term + "&location=" + location + "&limit=" + limit; 
		return URL;
	}
	
	private static ArrayList<Restaurant> stringToJSON(String stringToParse) {
		JSONParser parser = new JSONParser(); 
		ArrayList<Restaurant> ans = new ArrayList<Restaurant>();
		
		try {
			JSONObject json = (JSONObject) parser.parse(stringToParse);
			JSONArray response = (JSONArray) json.get("businesses"); 
			
			for (int i = 0; i < response.size(); i++) {
				JSONObject item = (JSONObject) response.get(i); 
				double distance = (Double) item.get("distance");
				double rating = (Double) item.get("rating");
				String phone = (String) item.get("phone");
				String name = (String) item.get("name");
				JSONObject location = (JSONObject) item.get("location");
				String address = (String) location.get("address1");
				ArrayList<String> aliasList = new ArrayList<String>();
				ArrayList<String> titleList = new ArrayList<String>();
				
				JSONArray categories = (JSONArray) item.get("categories");  
				for (int j = 0; j < categories.size(); j++) {
					JSONObject items = (JSONObject) categories.get(j);
					String alias = (String) items.get("alias");
					String title = (String) items.get("title");
					
					aliasList.add(alias);
					titleList.add(title);
				}
				
				boolean is_closed = (Boolean) item.get("is_closed");
				
				Restaurant restaurant = new Restaurant(name, address, phone, distance, rating, aliasList, titleList, is_closed);
				ans.add(restaurant);
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return ans;
	}
}
