package com.amazon.customskill;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

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

public class RestaurantFinder {
	
	private static String key = "Efio1-A9NjP2UHSaA5aGwn3IILFcHD39ISzq201w-pxaaaQ2MBiothsuZzUoVmulDTr0W8TPDhsAkt8qS1UpBURKhKFr-6V-EKuJZvFebQXOXiAEOGbLmNhIt_vWW3Yx";
	private static final String URL = "https://api.yelp.com/v3/businesses/search?";
	
    public static ArrayList<Restaurant> getData(String address, long radius) {
    	ArrayList<Restaurant> res = new ArrayList<Restaurant>();
    	HttpClient client = HttpClientBuilder.create().build();
    	HttpGet get = new HttpGet(getURL(getCoordinate(address), radius));    
		get.addHeader("Authorization", "Bearer " + key);
		String s = "";
	    try {
			HttpResponse response = client.execute(get);
			s = EntityUtils.toString(response.getEntity());
			res = buildRestaurantList(s);
			//Debug
			write_file(s, "Ausgabe");
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block and do
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return res;
    }
    
    //Debug
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getCoordinate(String address) {
		Map<String, Double> coords;
	    coords = OpenStreetMapUtils.getInstance().getCoordinates(address);
	    return "" + coords.get("lat") + "," + coords.get("lon");
	}
	
	private static String getURL(String location, long radius) {
		String term = "restaurant";
		String limit = "20";
		String locale = "de_DE";
		return URL + "radius=" + radius + "&term=" + term + "&location=" + location + "&limit=" + limit + "&locale=" + locale;
	}
	
	private static ArrayList<Restaurant> buildRestaurantList(String stringToParse) {
		//Debug
		String result = "";
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
				
				//Debug
				result += (i+1) + ". Name: " + name.toLowerCase() + System.getProperty("line.separator") +
						"Phone: " + phone + System.getProperty("line.separator") +
						"Distance: " + distance + System.getProperty("line.separator") +
						"Rating: " + rating + System.getProperty("line.separator") +
						"Address: " + address.toLowerCase() + System.getProperty("line.separator");
						
				JSONArray categories = (JSONArray) item.get("categories");  
				for (int j = 0; j < categories.size(); j++) {
					JSONObject items = (JSONObject) categories.get(j);
					String alias = (String) items.get("alias");
					String title = (String) items.get("title");
					
					if(title.contains("(")) {
						title = title.substring(0, title.indexOf("(")).trim();
					}
					
					aliasList.add(alias.toLowerCase());
					titleList.add(title.toLowerCase());
				
					//Debug
					result += "Alias: " + alias.toLowerCase() + System.getProperty("line.separator");
					result += "Title: " + title.toLowerCase()+ System.getProperty("line.separator");
				}
				
				boolean is_closed = (Boolean) item.get("is_closed");
				
				//Debug
				result += "Is closed: " + is_closed + System.getProperty("line.separator") + System.getProperty("line.separator");
				
				Restaurant restaurant = new Restaurant(name.toLowerCase(), address.toLowerCase(), phone, distance, rating, aliasList, titleList, is_closed);
				ans.add(restaurant);
				
				//Debug
				write_file(result, "Result");
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return ans;
	}
}